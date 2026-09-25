-- Create purchase returns tables for Postgres

CREATE TABLE IF NOT EXISTS purchase_returns (
    pr_id SERIAL PRIMARY KEY,
    supplier_id INT NOT NULL,
    original_invoice_no VARCHAR(50),
    return_date DATE,
    reason VARCHAR(50),
    total_items INT DEFAULT 0,
    grand_total DECIMAL(10,2) DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (supplier_id) REFERENCES suppliers(supplier_id)
);

CREATE TABLE IF NOT EXISTS purchase_return_items (
    pri_id SERIAL PRIMARY KEY,
    pr_id INT NOT NULL,
    medicine_id INT NOT NULL,
    batch_number VARCHAR(100),
    quantity INT NOT NULL,
    purchase_rate DECIMAL(10,2) DEFAULT 0.00,
    return_amount DECIMAL(10,2) DEFAULT 0.00,
    FOREIGN KEY (pr_id) REFERENCES purchase_returns(pr_id) ON DELETE CASCADE,
    FOREIGN KEY (medicine_id) REFERENCES medicines(medicine_id)
);
