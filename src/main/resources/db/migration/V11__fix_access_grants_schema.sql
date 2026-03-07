-- Fix access_grants table schema
-- This migration handles the case where an old access_grants table exists

-- Drop the old table if it exists (development only)
DROP TABLE IF EXISTS access_grants CASCADE;

-- Recreate with correct schema
CREATE TABLE access_grants (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    clinic_id BIGINT NOT NULL REFERENCES clinics(id) ON DELETE CASCADE,
    referral_id BIGINT NOT NULL REFERENCES referrals(id) ON DELETE CASCADE,
    permission_type VARCHAR(20) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at TIMESTAMP,
    revoked_by VARCHAR(255),
    revocation_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_permission_type CHECK (permission_type IN ('READ_ONLY', 'READ_WRITE'))
);

-- Recreate indexes
CREATE INDEX idx_access_grants_patient ON access_grants(patient_id);
CREATE INDEX idx_access_grants_clinic ON access_grants(clinic_id);
CREATE INDEX idx_access_grants_referral ON access_grants(referral_id);
CREATE INDEX idx_access_grants_expires ON access_grants(expires_at);
CREATE INDEX idx_access_grants_active ON access_grants(revoked, expires_at);
