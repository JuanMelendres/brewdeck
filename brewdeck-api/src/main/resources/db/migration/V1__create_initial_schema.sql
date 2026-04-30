CREATE TABLE coffees (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    brand VARCHAR(120),
    origin VARCHAR(120),
    region VARCHAR(120),
    farm VARCHAR(120),
    producer VARCHAR(120),
    variety VARCHAR(120),
    process VARCHAR(80),
    roast_level VARCHAR(50),
    notes_primary TEXT,
    notes_secondary TEXT,
    acidity VARCHAR(50),
    body VARCHAR(50),
    sweetness VARCHAR(50),
    bitterness VARCHAR(50),
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE brew_methods (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(80) NOT NULL UNIQUE,
  description TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE recipes (
     id BIGSERIAL PRIMARY KEY,
     coffee_id BIGINT NOT NULL REFERENCES coffees(id),
     method_id BIGINT NOT NULL REFERENCES brew_methods(id),
     name VARCHAR(120) NOT NULL,
     coffee_grams DECIMAL(6,2),
     water_grams DECIMAL(6,2),
     ratio VARCHAR(20),
     grind_setting VARCHAR(80),
     water_temp INTEGER,
     brew_time VARCHAR(40),
     steps TEXT,
     expected_taste TEXT,
     favorite BOOLEAN NOT NULL DEFAULT false,
     created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
     updated_at TIMESTAMP
);

CREATE TABLE brew_sessions (
    id BIGSERIAL PRIMARY KEY,
    recipe_id BIGINT NOT NULL REFERENCES recipes(id),
    brewed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actual_grind VARCHAR(80),
    actual_temp INTEGER,
    actual_time VARCHAR(40),
    taste_result TEXT,
    rating INTEGER,
    adjustment_notes TEXT
);