CREATE TABLE medicine_substitutes (
    id SERIAL PRIMARY KEY,
    medicine_id INT NOT NULL,
    substitute_medicine_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (medicine_id) REFERENCES medicines(medicine_id),
    FOREIGN KEY (substitute_medicine_id) REFERENCES medicines(medicine_id),
    UNIQUE (medicine_id, substitute_medicine_id)
);
