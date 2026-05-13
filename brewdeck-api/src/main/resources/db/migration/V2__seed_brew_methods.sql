INSERT INTO brew_methods (name, description)
VALUES
    (
        'AeroPress',
        'Immersion and pressure-based brewing method known for versatility, clean cups, and quick extraction.'
    ),
    (
        'V60',
        'Pour-over brewing method focused on clarity, aroma, and highlighting delicate flavor notes.'
    ),
    (
        'Espresso',
        'Pressure-based extraction method that produces a concentrated coffee with body, intensity, and crema.'
    ),
    (
        'French Press',
        'Full immersion brewing method that produces a rich cup with heavier body and more texture.'
    ),
    (
        'Chemex',
        'Pour-over brewing method known for clean, bright cups and a lighter body due to its thick paper filters.'
    ),
    (
        'Moka Pot',
        'Stovetop pressure brewing method that produces a strong and intense coffee with a heavier body.'
    ),
    (
        'Kalita Wave',
        'Flat-bottom pour-over method designed for a more even extraction and balanced flavor profile.'
    ),
    (
        'Origami Dripper',
        'Versatile pour-over dripper compatible with different filters, allowing experimentation with flow rate and clarity.'
    ),
    (
        'Cold Brew',
        'Long immersion brewing method using cold water, producing a smooth, sweet, and low-acidity coffee.'
    ),
    (
        'Clever Dripper',
        'Hybrid immersion and pour-over brewing method that offers consistency, sweetness, and easy extraction control.'
    )
ON CONFLICT (name) DO NOTHING;