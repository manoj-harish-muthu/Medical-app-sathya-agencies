-- Add advanced ERP fields to medicines table
ALTER TABLE medicines ADD COLUMN packing VARCHAR(100);
ALTER TABLE medicines ADD COLUMN unit_1st VARCHAR(50);
ALTER TABLE medicines ADD COLUMN unit_2nd VARCHAR(50);
ALTER TABLE medicines ADD COLUMN decimal_allowed VARCHAR(10) DEFAULT 'No';
ALTER TABLE medicines ADD COLUMN color_type VARCHAR(50) DEFAULT 'NORMAL';
ALTER TABLE medicines ADD COLUMN item_type VARCHAR(50) DEFAULT '1 NORMAL';
ALTER TABLE medicines ADD COLUMN cgst DECIMAL(5,2) DEFAULT 0.00;
ALTER TABLE medicines ADD COLUMN sgst DECIMAL(5,2) DEFAULT 0.00;
ALTER TABLE medicines ADD COLUMN igst DECIMAL(5,2) DEFAULT 0.00;
ALTER TABLE medicines ADD COLUMN local_tax_type VARCHAR(50) DEFAULT 'Taxable';
ALTER TABLE medicines ADD COLUMN central_tax_type VARCHAR(50) DEFAULT 'Taxable';
ALTER TABLE medicines ADD COLUMN negative_allowed VARCHAR(10) DEFAULT 'No';

-- Add advanced ERP pricing fields to medicine_batches table
ALTER TABLE medicine_batches ADD COLUMN rate_a DECIMAL(10,2) DEFAULT 0.00;
ALTER TABLE medicine_batches ADD COLUMN rate_b DECIMAL(10,2) DEFAULT 0.00;
ALTER TABLE medicine_batches ADD COLUMN rate_c DECIMAL(10,2) DEFAULT 0.00;
ALTER TABLE medicine_batches ADD COLUMN cost_per_pcs DECIMAL(15,5) DEFAULT 0.00000;
ALTER TABLE medicine_batches ADD COLUMN conv_str INT DEFAULT 0;
ALTER TABLE medicine_batches ADD COLUMN conv_cas INT DEFAULT 0;
