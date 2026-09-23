CREATE TABLE medicine_schedules (
    schedule_id SERIAL PRIMARY KEY,
    schedule_name VARCHAR(255) NOT NULL UNIQUE,
    requires_prescription BOOLEAN DEFAULT FALSE,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
