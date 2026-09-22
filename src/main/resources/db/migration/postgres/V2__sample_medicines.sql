-- Seed sample categories
INSERT INTO medicine_categories (category_name, description) VALUES
('Antibiotics', 'Medicines that destroy or slow down the growth of bacteria'),
('Painkillers', 'Medicines that reduce or relieve pain'),
('Vitamins', 'Nutritional supplements');

-- Seed sample companies
INSERT INTO medicine_companies (company_name, contact_person, phone) VALUES
('Sun Pharma', 'Rajesh Kumar', '9876543210'),
('Cipla', 'Amit Patel', '9876543211'),
('Pfizer', 'John Doe', '9876543212');

-- Seed sample medicines
INSERT INTO medicines (medicine_name, salt_name, company_id, category_id, hsn_code, gst_rate, pack_size, minimum_stock, reorder_level, active) VALUES
('Paracetamol 500mg', 'Paracetamol', 1, 2, '3004', 12.00, '10 Tablets', 50, 100, 1),
('Amoxicillin 250mg', 'Amoxicillin', 2, 1, '3004', 12.00, '10 Capsules', 50, 100, 1),
('Vitamin C 500mg', 'Ascorbic Acid', 3, 3, '3004', 18.00, '15 Tablets', 50, 100, 1),
('Dolo 650', 'Paracetamol', 1, 2, '3004', 12.00, '15 Tablets', 50, 100, 1);

-- Seed sample batches (stock)
INSERT INTO medicine_batches (medicine_id, batch_number, manufacturing_date, expiry_date, purchase_rate, mrp, selling_rate, current_quantity, rack_info) VALUES
(1, 'BAT-P-001', '2025-01-01', '2027-01-01', 10.00, 20.00, 18.00, 500, 'A1'),
(2, 'BAT-A-001', '2025-02-01', '2026-02-01', 30.00, 50.00, 45.00, 200, 'A2'),
(3, 'BAT-V-001', '2025-03-01', '2028-03-01', 15.00, 30.00, 25.00, 1000, 'B1'),
(4, 'BAT-D-001', '2025-01-15', '2027-01-15', 12.00, 25.00, 22.00, 300, 'A1');
