"""BrewDeck AI spike: compare local Ollama models on fixed recipe scenarios.

Mirrors ClaudeRecipeSuggestionAdapter: same system prompts and user-message layout, the same
SuggestedRecipe fields, requested as JSON through Ollama's `format` (JSON schema). Scores each
answer for schema validity, sensible ranges, Spanish output, and latency.
"""
import json
import re
import sys
import time
import urllib.request

OLLAMA = "http://127.0.0.1:11434/api/chat"
MODELS = sys.argv[1:] or ["llama3.2:3b", "llama3.1:8b", "qwen3:8b"]

SUGGEST_SYSTEM = (
    "You are an expert barista. Given a coffee and a brew method, return brewing parameters"
    " as structured data only. Water temperature is in degrees Celsius, between 70 and 100."
    " Keep steps and rationale concise."
)
IMPROVE_SYSTEM = (
    "You are an expert barista tuning an existing coffee recipe using its brew history."
    " Given the current parameters and recent rated brews, return improved brewing"
    " parameters as structured data only. Water temperature is in degrees Celsius,"
    " between 70 and 100. Keep steps concise, and use the rationale to explain what you"
    " changed and why."
)
SPANISH = " Write steps and rationale in Spanish."

SCHEMA = {
    "type": "object",
    "properties": {
        "coffeeGrams": {"type": "number"},
        "waterGrams": {"type": "number"},
        "ratio": {"type": "string"},
        "grindSetting": {"type": "string"},
        "waterTemp": {"type": "integer"},
        "brewTime": {"type": "string"},
        "steps": {"type": "string"},
        "rationale": {"type": "string"},
    },
    "required": ["coffeeGrams", "waterGrams", "ratio", "grindSetting", "waterTemp", "brewTime", "steps", "rationale"],
}

# (id, kind, method, expected water/coffee range, user message). Synthetic but realistic: the
# owner's local database had no coffees or recipes when the spike ran.
def coffee(name, origin, roast, process, a, b, s, bi):
    return (f"Coffee: {name}\nOrigin: {origin}\nRoast: {roast}\nProcess: {process}\n"
            f"Tasting scores (1-5): acidity={a}, body={b}, sweetness={s}, bitterness={bi}\n")

SCENARIOS = [
    ("s1-v60", "suggest", "V60", (14, 18), coffee("Finca La Pastoría", "Chiapas, Mexico", "Light", "Washed", 4, 2, 4, 1)
     + "Brew method: V60 (Pour-over brewing method focused on clarity, aroma, and high acidity)\nUser notes: -"),
    ("s2-aeropress", "suggest", "AeroPress", (10, 17), coffee("Pluma Hidalgo", "Oaxaca, Mexico", "Medium", "Washed", 3, 3, 4, 2)
     + "Brew method: AeroPress (Immersion and pressure-based brewing method known for versatility)\nUser notes: I want a sweet, clean cup"),
    ("s3-espresso", "suggest", "Espresso", (1.5, 3), coffee("Coatepec", "Veracruz, Mexico", "Medium-dark", "Natural", 2, 4, 3, 3)
     + "Brew method: Espresso (Pressure-based extraction method that produces a concentrated shot)\nUser notes: double shot"),
    ("s4-frenchpress", "suggest", "French Press", (12, 17), coffee("Sierra Mazateca", "Oaxaca, Mexico", "Dark", "Honey", 1, 5, 3, 4)
     + "Brew method: French Press (Full immersion brewing method that produces a rich cup with body)\nUser notes: -"),
    ("s5-chemex", "suggest", "Chemex", (14, 18), coffee("Geisha El Triunfo", "Chiapas, Mexico", "Light", "Anaerobic natural", 5, 2, 5, 1)
     + "Brew method: Chemex (Pour-over brewing method known for clean, bright cups)\nUser notes: brewing for two people"),
    ("s6-coldbrew", "suggest", "Cold Brew", (4, 12), coffee("Pluma Hidalgo", "Oaxaca, Mexico", "Medium", "Washed", 3, 3, 4, 2)
     + "Brew method: Cold Brew (Long immersion brewing method using cold water, producing a smooth concentrate)\nUser notes: -"),
    ("i1-v60-sour", "improve", "V60", (14, 18), coffee("Finca La Pastoría", "Chiapas, Mexico", "Light", "Washed", 4, 2, 4, 1)
     + "Brew method: V60 (Pour-over brewing method focused on clarity, aroma, and high acidity)\n"
     + "Current recipe: 15 g coffee, 250 g water, ratio 1:16.7, grind medium-fine (Comandante 22 clicks), 92 C, 2:30, steps: bloom 40 g 30 s, pour to 250 g.\n"
     + "Recent brews (newest first):\n- rating 5/10, grind 22 clicks, 92 C, 2:20: too sour, thin\n- rating 6/10, grind 21 clicks, 93 C, 2:35: a bit sour, bright\n- rating 4/10, grind 23 clicks, 90 C, 2:10: sour, watery"),
    ("i2-fp-bitter", "improve", "French Press", (12, 17), coffee("Sierra Mazateca", "Oaxaca, Mexico", "Dark", "Honey", 1, 5, 3, 4)
     + "Brew method: French Press (Full immersion brewing method that produces a rich cup with body)\n"
     + "Current recipe: 30 g coffee, 450 g water, ratio 1:15, grind medium, 96 C, 5:00, steps: pour, stir, steep, press.\n"
     + "Recent brews (newest first):\n- rating 5/10, grind medium, 96 C, 5:30: bitter, harsh finish\n- rating 6/10, grind medium-coarse, 95 C, 5:00: bitter but less\n- rating 4/10, grind medium-fine, 96 C, 6:00: very bitter, muddy"),
]

SPANISH_HINTS = re.compile(r"\b(el|la|los|las|de|del|con|para|agua|café|vierte|vertido|molienda|minutos|segundos|hasta|y)\b", re.I)
ENGLISH_HINTS = re.compile(r"\b(the|and|with|pour|water|coffee|until|minutes|seconds|grind)\b", re.I)


def ask(model, system, user):
    body = {
        "model": model,
        "messages": [{"role": "system", "content": system}, {"role": "user", "content": user}],
        "format": SCHEMA,
        "stream": False,
        "options": {"temperature": 0.3, "num_predict": 1024},
    }
    if model.startswith("qwen3"):
        body["think"] = False  # reasoning traces add latency and are not used
    req = urllib.request.Request(OLLAMA, data=json.dumps(body).encode(), headers={"Content-Type": "application/json"})
    t0 = time.time()
    with urllib.request.urlopen(req, timeout=300) as r:
        res = json.loads(r.read())
    return res, time.time() - t0


def score(scenario, content):
    sid, kind, method, ratio_range, _ = scenario
    issues = []
    try:
        d = json.loads(content)
    except Exception:
        return {"valid": False, "issues": ["not JSON"]}
    missing = [k for k in SCHEMA["required"] if k not in d]
    if missing:
        issues.append("missing " + ",".join(missing))
    try:
        cg, wg, wt = float(d["coffeeGrams"]), float(d["waterGrams"]), int(d["waterTemp"])
        if cg <= 0 or wg <= 0:
            issues.append("non-positive grams")
        lo, hi = ratio_range
        if cg > 0 and not (lo <= wg / cg <= hi):
            issues.append(f"water/coffee {wg/cg:.1f} outside {lo}-{hi}")
        if not (70 <= wt <= 100):
            issues.append(f"waterTemp {wt} outside 70-100")
        nums = re.findall(r"(\d+(?:\.\d+)?)\s*[:/]\s*(\d+(?:\.\d+)?)", str(d.get("ratio", "")))
        if nums:
            a, b = map(float, nums[0])
            stated = b / a if a else 0
            if cg > 0 and abs(stated - wg / cg) / (wg / cg) > 0.15:
                issues.append(f"ratio '{d['ratio']}' disagrees with grams ({wg/cg:.1f})")
    except Exception as e:
        issues.append(f"bad types: {e}")
    text = f"{d.get('steps','')} {d.get('rationale','')}"
    es, en = len(SPANISH_HINTS.findall(text)), len(ENGLISH_HINTS.findall(text))
    spanish = es > en
    if not spanish:
        issues.append("not Spanish")
    return {"valid": not missing, "issues": issues, "spanish": spanish, "data": d}


def main():
    results = []
    for model in MODELS:
        try:
            ask(model, "Reply with a sample recipe.", "warm up")  # load weights before timing
        except Exception:
            pass
        for sc in SCENARIOS:
            system = (SUGGEST_SYSTEM if sc[1] == "suggest" else IMPROVE_SYSTEM) + SPANISH
            res, secs = ask(model, system, sc[4])
            content = res["message"]["content"]
            sco = score(sc, content)
            if res.get("done_reason") == "length":
                sco["issues"].append("hit the 1024-token cap (runaway)")
                sco["valid"] = False
            tps = res.get("eval_count", 0) / (res.get("eval_duration", 1) / 1e9)
            results.append({"model": model, "scenario": sc[0], "seconds": round(secs, 1), "tokens_per_s": round(tps, 1), **sco})
            print(f"{model:14} {sc[0]:14} {secs:5.1f}s {tps:5.1f} tok/s valid={sco['valid']} issues={sco['issues']}", flush=True)
    json.dump(results, open("results.json", "w"), ensure_ascii=False, indent=1)  # saved per run; see results-*.json


if __name__ == "__main__":
    main()
