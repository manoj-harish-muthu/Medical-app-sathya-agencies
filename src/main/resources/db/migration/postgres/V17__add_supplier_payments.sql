-- Add paid_amount and balance_amount to purchases
ALTER TABLE purchases ADD COLUMN paid_amount DECIMAL(10,2) DEFAULT 0.00;
ALTER TABLE purchases ADD COLUMN balance_amount DECIMAL(10,2) DEFAULT 0.00;

-- Create supplier_payments table to track individual payments
CREATE TABLE IF NOT EXISTS supplier_payments (
    sp_id SERIAL PRIMARY KEY,
    purchase_id INT,
    supplier_id INT NOT NULL,
    payment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    amount_paid DECIMAL(10,2) NOT NULL,
    payment_mode VARCHAR(50),
    notes TEXT,
    FOREIGN KEY (purchase_id) REFERENCES purchases(purchase_id),
    FOREIGN KEY (supplier_id) REFERENCES suppliers(supplier_id)
);
