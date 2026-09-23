ALTER TABLE medicines ADD COLUMN brand_id INT;
ALTER TABLE medicines ADD COLUMN schedule_id INT;

ALTER TABLE medicines ADD CONSTRAINT fk_medicine_brand FOREIGN KEY (brand_id) REFERENCES medicine_brands(brand_id);
ALTER TABLE medicines ADD CONSTRAINT fk_medicine_schedule FOREIGN KEY (schedule_id) REFERENCES medicine_schedules(schedule_id);
