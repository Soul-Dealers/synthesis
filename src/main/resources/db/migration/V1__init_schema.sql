-- Providers table
CREATE TABLE providers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    role VARCHAR(100) NOT NULL,
    clinic_name VARCHAR(255),
    region VARCHAR(255),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    active BOOLEAN DEFAULT true
);

-- Patients table
CREATE TABLE patients (
    id BIGSERIAL PRIMARY KEY,
    national_id VARCHAR(100) UNIQUE,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    date_of_birth DATE NOT NULL,
    gender VARCHAR(50),
    blood_group VARCHAR(10),
    allergies TEXT,
    clinic_name VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    active BOOLEAN DEFAULT true
);

-- Consultations table
CREATE TABLE consultations (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    provider_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    chief_complaint TEXT,
    vitals JSONB,
    notes TEXT,
    opened_at TIMESTAMP NOT NULL,
    closed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    active BOOLEAN DEFAULT true,
    CONSTRAINT fk_consultation_patient FOREIGN KEY (patient_id) REFERENCES patients(id),
    CONSTRAINT fk_consultation_provider FOREIGN KEY (provider_id) REFERENCES providers(id)
);

-- Diagnoses table
CREATE TABLE diagnoses (
    id BIGSERIAL PRIMARY KEY,
    consultation_id BIGINT NOT NULL,
    condition_name VARCHAR(255) NOT NULL,
    confidence_score DECIMAL(5,2),
    reasoning TEXT,
    source VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    active BOOLEAN DEFAULT true,
    CONSTRAINT fk_diagnosis_consultation FOREIGN KEY (consultation_id) REFERENCES consultations(id)
);

-- Treatments table
CREATE TABLE treatments (
    id BIGSERIAL PRIMARY KEY,
    diagnosis_id BIGINT NOT NULL,
    type VARCHAR(100),
    drug_name VARCHAR(255),
    dosage VARCHAR(100),
    duration VARCHAR(100),
    instructions TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    active BOOLEAN DEFAULT true,
    CONSTRAINT fk_treatment_diagnosis FOREIGN KEY (diagnosis_id) REFERENCES diagnoses(id)
);

-- Indexes for common queries
CREATE INDEX idx_patients_national_id ON patients(national_id);
CREATE INDEX idx_patients_clinic ON patients(clinic_name);
CREATE INDEX idx_consultations_patient ON consultations(patient_id);
CREATE INDEX idx_consultations_provider ON consultations(provider_id);
CREATE INDEX idx_consultations_status ON consultations(status);
CREATE INDEX idx_diagnoses_consultation ON diagnoses(consultation_id);
CREATE INDEX idx_treatments_diagnosis ON treatments(diagnosis_id);
