-- Migration V6: Create Agent Orders and Agent Order Items tables for WhatsApp AI Agent (PostgreSQL Version)
CREATE TABLE IF NOT EXISTS agent_orders (
    order_id SERIAL PRIMARY KEY,
    customer_id VARCHAR(50) NOT NULL,
    customer_name VARCHAR(255),
    phone VARCHAR(50),
    delivery_address TEXT,
    is_confirmed BOOLEAN DEFAULT FALSE,
    is_checked_by_admin BOOLEAN DEFAULT FALSE,
    admin_status VARCHAR(50) DEFAULT 'pending',
    order_source VARCHAR(50) DEFAULT 'WhatsApp AI Agent',
    source_file VARCHAR(255),
    total_items INT DEFAULT 0,
    order_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS agent_order_items (
    item_id SERIAL PRIMARY KEY,
    order_id INT NOT NULL,
    medicine_name VARCHAR(255) NOT NULL,
    quantity VARCHAR(50) NOT NULL,
    unit VARCHAR(50),
    volume_ml VARCHAR(50),
    pieces_per_unit INT,
    FOREIGN KEY (order_id) REFERENCES agent_orders(order_id) ON DELETE CASCADE
);
