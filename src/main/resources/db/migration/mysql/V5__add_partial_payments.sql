-- Add paid_amount and balance_amount to sales
ALTER TABLE sales ADD COLUMN paid_amount DECIMAL(10,2) DEFAULT 0.00 AFTER grand_total;
ALTER TABLE sales ADD COLUMN balance_amount DECIMAL(10,2) DEFAULT 0.00 AFTER paid_amount;

-- Create customer_payments table to track individual payments
CREATE TABLE IF NOT EXISTS customer_payments (
    payment_id INT AUTO_INCREMENT PRIMARY KEY,
    sale_id INT NOT NULL,
    customer_phone VARCHAR(50),
    payment_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    amount_paid DECIMAL(10,2) NOT NULL,
    payment_mode VARCHAR(50),
    notes TEXT,
    FOREIGN KEY (sale_id) REFERENCES sales(sale_id)
);
