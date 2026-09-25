-- Create purchase orders tables for MySQL

CREATE TABLE IF NOT EXISTS purchase_orders (
    po_id INT AUTO_INCREMENT PRIMARY KEY,
    supplier_id INT NOT NULL,
    expected_delivery_date DATE,
    status VARCHAR(50) DEFAULT 'Pending',
    total_items INT DEFAULT 0,
    grand_total DECIMAL(10,2) DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (supplier_id) REFERENCES suppliers(supplier_id)
);

CREATE TABLE IF NOT EXISTS purchase_order_items (
    poi_id INT AUTO_INCREMENT PRIMARY KEY,
    po_id INT NOT NULL,
    medicine_id INT NOT NULL,
    quantity INT NOT NULL,
    est_rate DECIMAL(10,2) DEFAULT 0.00,
    est_amount DECIMAL(10,2) DEFAULT 0.00,
    FOREIGN KEY (po_id) REFERENCES purchase_orders(po_id) ON DELETE CASCADE,
    FOREIGN KEY (medicine_id) REFERENCES medicines(medicine_id)
);
