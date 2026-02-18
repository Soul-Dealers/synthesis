-- Add region field to patients table for accurate disease trend tracking by patient location
ALTER TABLE patients ADD COLUMN region VARCHAR(100);

CREATE INDEX idx_patients_region ON patients(region);
