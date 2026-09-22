-- Add paid_amount and balance_amount to sales
ALTER TABLE sales ADD COLUMN paid_amount DECIMAL(10,2) DEFAULT 0.00;
ALTER TABLE sales ADD COLUMN balance_amount DECIMAL(10,2) DEFAULT 0.00;

-- Create customer_payments table to track individual payments
CREATE TABLE IF NOT EXISTS customer_payments (
    payment_id SERIAL PRIMARY KEY,
    sale_id INT NOT NULL,
    customer_phone VARCHAR(50),
    payment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    amount_paid DECIMAL(10,2) NOT NULL,
    payment_mode VARCHAR(50),
    notes TEXT,
    FOREIGN KEY (sale_id) REFERENCES sales(sale_id)
);
