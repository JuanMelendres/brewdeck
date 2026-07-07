ALTER TABLE coffees DROP COLUMN acidity;
ALTER TABLE coffees DROP COLUMN body;
ALTER TABLE coffees DROP COLUMN sweetness;
ALTER TABLE coffees DROP COLUMN bitterness;

ALTER TABLE coffees ADD COLUMN acidity_score INTEGER;
ALTER TABLE coffees ADD COLUMN body_score INTEGER;
ALTER TABLE coffees ADD COLUMN sweetness_score INTEGER;
ALTER TABLE coffees ADD COLUMN bitterness_score INTEGER;
