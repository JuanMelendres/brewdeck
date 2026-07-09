---
name: review-skills-delivery
description: Reviews Claude Code skills for quality, improves them if needed, validates structure, creates a git branch, commits the changes, opens a pull request, and merges it only when safe.
---

# Claude Skills Review and Delivery Workflow

Act as a senior Technical Writer, Software Architect, Staff Backend Engineer, and Release Engineer.

Your task is to review the Claude Code skills in this repository, improve them if needed, validate their quality, and deliver them through a clean Git workflow.

The target skills are usually:

* `write-fdd`
* `write-tdd`
* `write-spike`

But if the user provides different skill names or paths through `$ARGUMENTS`, use those instead.

## Input

User arguments:

```text
$ARGUMENTS
```

If no arguments are provided, inspect the repository and look for skills under:

```text
.claude/skills/
skills/
claude-skills/
```

Do not assume that skills stored only under `~/.claude/skills/` are part of this repository unless the current working directory is a Git repo that contains them.

## Main Goal

Review, improve, validate, commit, open a PR, and merge the new or updated Claude Code skills only if everything is safe and correct.

## Critical Safety Rules

1. Do not overwrite unrelated user changes.
2. Do not include unrelated files in the commit.
3. Do not merge if there are failing checks.
4. Do not merge if the PR contains unresolved issues.
5. Do not force-push unless explicitly asked.
6. Do not bypass branch protection.
7. Do not use admin merge.
8. Do not delete existing skill content unless clearly replacing it with a better version.
9. Do not invent project-specific facts.
10. If the repository has no remote or GitHub CLI is not authenticated, stop after the commit and explain what is missing.
11. If merge is blocked by permissions or branch protection, leave the PR open and report the status.
12. If anything looks risky, stop before merging.

## Step 1: Inspect Repository State

Run:

```bash
git status --short
git branch --show-current
git remote -v
```

Determine:

* Current branch.
* Whether the working tree has existing unrelated changes.
* Whether this is a Git repository.
* Whether a GitHub remote exists.
* Whether GitHub CLI is available.

If unrelated changes exist, do not touch them. Either work only with the skill files or stop and report.

## Step 2: Locate Skill Files

Find all relevant skill files:

```bash
find . -path "*/.claude/skills/*/SKILL.md" -o -path "./skills/*/SKILL.md" -o -path "./claude-skills/*/SKILL.md"
```

Expected skill structure:

```text
<skill-name>/
  SKILL.md
```

Each `SKILL.md` should contain:

* YAML frontmatter.
* A clear `name`.
* A clear `description`.
* Practical instructions.
* Usage rules.
* Output format.
* Quality checklist.
* Guardrails against inventing information.

## Step 3: Review Skill Quality

For each skill, evaluate:

### Metadata

* Does it have valid YAML frontmatter?
* Is the `name` clear, lowercase, and command-friendly?
* Is the `description` specific enough for Claude to know when to use it?
* Is the description not too broad?
* Does the description avoid triggering for unrelated tasks?

### Purpose

* Is the skill single-purpose?
* Does it solve a recurring workflow?
* Does it avoid becoming a general CLAUDE.md dump?

### Instructions

* Are the instructions clear?
* Are the steps ordered logically?
* Does the skill ask the right discovery questions?
* Does it avoid asking too many questions at once?
* Does it explain what to do when context is missing?
* Does it prevent inventing facts?
* Does it tell Claude when to inspect repository files?

### Document Structure

For `write-fdd`, verify that it covers:

* Summary
* Problem statement
* Goals
* Non-goals
* Users and actors
* User stories
* Functional requirements
* Business rules
* User flow
* Data inputs
* Expected outputs
* Edge cases
* Error scenarios
* Acceptance criteria
* Permissions
* Dependencies
* Out of scope
* Open questions
* Assumptions
* Notes for TDD

For `write-tdd`, verify that it covers:

* Summary
* Context
* Goals
* Non-goals
* Requirements reference
* Current architecture
* Proposed architecture
* Component design
* Data model
* API design
* Validation rules
* Error handling
* Security
* Observability
* Performance
* Transactions and consistency
* Testing strategy
* Rollout plan
* Alternatives
* Risks
* Open questions
* Assumptions
* Implementation plan

For `write-spike`, verify that it covers:

* Summary
* Background
* Problem statement
* Goals
* Non-goals
* Key questions
* Constraints
* Options considered
* Evaluation criteria
* Research findings
* POC plan
* POC results
* Trade-offs
* Risks
* Recommendation
* Decision
* Next steps
* ADR candidate
* Open questions
* Assumptions

### Quality

* Is the skill practical for real software projects?
* Is it useful for BrewDeck and BrickDeck?
* Is it useful for future projects?
* Is it lightweight enough?
* Is it robust enough?
* Are examples helpful without being excessive?
* Does it avoid corporate filler?
* Does it produce documents that could be used by engineering, product, QA, and AI agents?

## Step 4: Improve the Skills If Needed

If a skill can be improved, update it.

Improvements may include:

* Better descriptions.
* Better trigger behavior.
* Clearer discovery questions.
* Stronger guardrails.
* Better document templates.
* Better quality checklist.
* Better final response format.
* Better separation between facts, assumptions, and TODOs.
* Better guidance for repository inspection.
* Better English wording.
* Better Markdown formatting.

Do not make unnecessary rewrites if the current version is already strong.

## Step 5: Validate Markdown and Structure

Validate:

```bash
find . -name "SKILL.md" -print
```

Then inspect relevant files manually.

If markdown tooling exists, run it.

Examples:

```bash
npm run lint:md
npm run lint
make lint
```

Only run commands that exist in the repository.

If no validation tooling exists, perform a manual validation and mention that no automated markdown validation was available.

## Step 6: Create a Branch

Use a clean branch name:

```bash
git checkout -b docs/add-claude-documentation-skills
```

If the branch already exists, use a timestamped alternative:

```bash
git checkout -b docs/add-claude-documentation-skills-$(date +%Y%m%d%H%M)
```

## Step 7: Stage Only Relevant Files

Stage only the skill files and directly related documentation.

Examples:

```bash
git add .claude/skills/write-fdd/SKILL.md
git add .claude/skills/write-tdd/SKILL.md
git add .claude/skills/write-spike/SKILL.md
```

If the skills are under a different path, stage only those paths.

Before committing, run:

```bash
git diff --staged
```

Confirm the staged diff includes only relevant changes.

## Step 8: Commit

Use this commit message:

```bash
git commit -m "docs: add Claude documentation skills"
```

If the changes are improvements to existing skills, use:

```bash
git commit -m "docs: improve Claude documentation skills"
```

## Step 9: Push and Create Pull Request

Check whether GitHub CLI is installed and authenticated:

```bash
gh --version
gh auth status
```

Push the branch:

```bash
git push -u origin HEAD
```

Create the PR:

```bash
gh pr create \
  --title "Add Claude documentation skills" \
  --body "$(cat <<'EOF'
## Summary

Adds or improves Claude Code skills for creating high-quality software documentation:

- Functional Design Documents (FDD)
- Technical Design Documents (TDD)
- Technical Spikes

## What Changed

- Added or updated Claude skill files.
- Improved documentation structure and writing guidance.
- Added guardrails for assumptions, TODOs, open questions, and repository inspection.

## Validation

- Reviewed skill metadata.
- Reviewed skill trigger descriptions.
- Reviewed document templates.
- Reviewed quality checklists.
- Verified staged changes only include relevant skill files.

## Notes

These skills are intended to support BrewDeck, BrickDeck, and future software projects using lightweight docs-as-code workflows.
EOF
)"
```

## Step 10: Review PR Checks

After creating the PR, inspect it:

```bash
gh pr view --web
gh pr checks
```

If checks are pending, do not merge yet. Report that the PR is open and waiting for checks.

If checks fail, do not merge. Report the failing checks and recommend fixes.

If no checks exist, say that no automated checks were configured and continue only if the diff is safe and small.

## Step 11: Merge Only If Safe

Merge only if all conditions are true:

* PR exists.
* Diff is limited to relevant skill/documentation files.
* No unrelated changes are included.
* Checks pass, or no checks are configured.
* There are no unresolved concerns.
* Branch protection allows merge.
* GitHub CLI has permission.

If safe, merge with squash:

```bash
gh pr merge --squash --delete-branch
```

If merge is not safe, do not merge. Leave the PR open and explain exactly why.

## Step 12: Final Report

Return a concise final report with:

* Skills reviewed.
* Files changed.
* Improvements made.
* Validation performed.
* Commit hash.
* PR link.
* Merge status.
* Any skipped steps and why.
* Any follow-up recommendations.

## Expected Behavior

If the skills are already excellent, do not rewrite them unnecessarily.

If small improvements are possible, make them.

If major improvements are needed, update them and explain why.

If delivery automation cannot be completed because of missing GitHub CLI, missing auth, no remote, branch protection, or failing checks, stop at the safest completed step and report clearly.
