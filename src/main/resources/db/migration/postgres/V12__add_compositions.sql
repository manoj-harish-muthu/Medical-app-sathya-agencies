CREATE TABLE medicine_compositions (
    id SERIAL PRIMARY KEY,
    medicine_id INT NOT NULL,
    salt_id INT NOT NULL,
    strength VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (medicine_id) REFERENCES medicines(medicine_id),
    FOREIGN KEY (salt_id) REFERENCES medicine_salts(salt_id),
    UNIQUE (medicine_id, salt_id)
);
