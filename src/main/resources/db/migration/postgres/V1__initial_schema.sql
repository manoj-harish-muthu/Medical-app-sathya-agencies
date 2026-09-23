-- Initial Database Schema for Pharmacy ERP (PostgreSQL / Neon DB Version)

CREATE TABLE users (
    user_id SERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(50),
    branch_id INT,
    active SMALLINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE branches (
    branch_id SERIAL PRIMARY KEY,
    branch_name VARCHAR(255) NOT NULL,
    address TEXT,
    phone VARCHAR(50),
    gstin VARCHAR(50),
    dl_number VARCHAR(100),
    is_main SMALLINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE medicine_categories (
    category_id SERIAL PRIMARY KEY,
    category_name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT
);

CREATE TABLE medicine_companies (
    company_id SERIAL PRIMARY KEY,
    company_name VARCHAR(255) NOT NULL UNIQUE,
    contact_person VARCHAR(255),
    phone VARCHAR(50),
    email VARCHAR(255)
);

CREATE TABLE medicines (
    medicine_id SERIAL PRIMARY KEY,
    medicine_name VARCHAR(255) NOT NULL,
    salt_name VARCHAR(255),
    company_id INT,
    category_id INT,
    hsn_code VARCHAR(50),
    gst_rate DECIMAL(10,2) DEFAULT 0.00,
    prescription_required SMALLINT DEFAULT 0,
    schedule_type VARCHAR(50),
    pack_size VARCHAR(50),
    minimum_stock INT DEFAULT 0,
    maximum_stock INT DEFAULT 0,
    reorder_level INT DEFAULT 0,
    active SMALLINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (company_id) REFERENCES medicine_companies(company_id),
    FOREIGN KEY (category_id) REFERENCES medicine_categories(category_id)
);

CREATE TABLE medicine_batches (
    batch_id SERIAL PRIMARY KEY,
    medicine_id INT NOT NULL,
    batch_number VARCHAR(100) NOT NULL,
    manufacturing_date DATE,
    expiry_date DATE NOT NULL,
    purchase_rate DECIMAL(10,2) NOT NULL,
    mrp DECIMAL(10,2) NOT NULL,
    selling_rate DECIMAL(10,2) NOT NULL,
    current_quantity INT DEFAULT 0,
    rack_info VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (medicine_id) REFERENCES medicines(medicine_id),
    UNIQUE (medicine_id, batch_number)
);

CREATE TABLE customers (
    customer_id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(50) UNIQUE,
    email VARCHAR(255),
    address TEXT,
    gstin VARCHAR(50),
    credit_limit DECIMAL(10,2) DEFAULT 0.00,
    opening_balance DECIMAL(10,2) DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE suppliers (
    supplier_id SERIAL PRIMARY KEY,
    supplier_name VARCHAR(255) NOT NULL,
    gstin VARCHAR(50),
    dl_number VARCHAR(100),
    phone VARCHAR(50),
    email VARCHAR(255),
    address TEXT,
    opening_balance DECIMAL(10,2) DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Seed basic data
INSERT INTO branches (branch_name, is_main) VALUES ('Main Store', 1);

-- Default Admin user
-- Using a valid BCrypt hash for password 'admin123'
INSERT INTO users (username, password_hash, full_name, role, branch_id) 
VALUES ('admin', '$2a$10$OebsJ27Z8d0Fv8i6iW4J8OS796/o0Kx3/1x.Y.O5Bq2f22K099vGu', 'System Administrator', 'SUPER_ADMIN', 1);
