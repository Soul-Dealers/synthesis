-- Create clinics table
CREATE TABLE clinics (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    address VARCHAR(500),
    region VARCHAR(255),
    registration_code VARCHAR(50) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    active BOOLEAN DEFAULT true
);

-- Add clinic_id column to providers
ALTER TABLE providers ADD COLUMN clinic_id BIGINT;

-- Add clinic_id column to patients
ALTER TABLE patients ADD COLUMN clinic_id BIGINT;

-- Migrate existing clinic_name data from providers into clinics table
INSERT INTO clinics (name, region, registration_code, created_at, updated_at, active)
SELECT DISTINCT
    p.clinic_name,
    p.region,
    UPPER(SUBSTR(MD5(RANDOM()::TEXT), 1, 8)),
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    true
FROM providers p
WHERE p.clinic_name IS NOT NULL AND p.clinic_name != '';

-- Migrate existing clinic_name data from patients (only names not already in clinics)
INSERT INTO clinics (name, region, registration_code, created_at, updated_at, active)
SELECT DISTINCT
    pt.clinic_name,
    pt.region,
    UPPER(SUBSTR(MD5(RANDOM()::TEXT), 1, 8)),
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    true
FROM patients pt
WHERE pt.clinic_name IS NOT NULL AND pt.clinic_name != ''
AND NOT EXISTS (SELECT 1 FROM clinics c WHERE c.name = pt.clinic_name);

-- Link providers to their clinics
UPDATE providers p
SET clinic_id = c.id
FROM clinics c
WHERE p.clinic_name = c.name;

-- Link patients to their clinics
UPDATE patients pt
SET clinic_id = c.id
FROM clinics c
WHERE pt.clinic_name = c.name;

-- Add foreign key constraints
ALTER TABLE providers
    ADD CONSTRAINT fk_provider_clinic FOREIGN KEY (clinic_id) REFERENCES clinics(id);

ALTER TABLE patients
    ADD CONSTRAINT fk_patient_clinic FOREIGN KEY (clinic_id) REFERENCES clinics(id);

-- Drop old clinic_name columns
ALTER TABLE providers DROP COLUMN IF EXISTS clinic_name;
ALTER TABLE patients DROP COLUMN IF EXISTS clinic_name;

-- Add indexes
CREATE INDEX idx_clinics_registration_code ON clinics(registration_code);
CREATE INDEX idx_clinics_name ON clinics(name);
CREATE INDEX idx_providers_clinic ON providers(clinic_id);
CREATE INDEX idx_patients_clinic ON patients(clinic_id);
