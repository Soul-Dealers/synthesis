-- Create referrals table
CREATE TABLE referrals (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    referring_clinic_id BIGINT NOT NULL REFERENCES clinics(id) ON DELETE CASCADE,
    receiving_clinic_id BIGINT NOT NULL REFERENCES clinics(id) ON DELETE CASCADE,
    referring_provider_id BIGINT NOT NULL REFERENCES providers(id) ON DELETE CASCADE,
    reason VARCHAR(500) NOT NULL,
    notes TEXT,
    access_expires_at TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL,
    accepted_at TIMESTAMP,
    accepted_by_provider_id BIGINT REFERENCES providers(id) ON DELETE SET NULL,
    completed_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    cancellation_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_referral_status CHECK (status IN ('PENDING', 'ACCEPTED', 'COMPLETED', 'EXPIRED', 'CANCELLED')),
    CONSTRAINT chk_different_clinics CHECK (referring_clinic_id != receiving_clinic_id)
);

-- Create access_grants table
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

-- Create indexes for performance
CREATE INDEX idx_referrals_patient ON referrals(patient_id);
CREATE INDEX idx_referrals_referring_clinic ON referrals(referring_clinic_id);
CREATE INDEX idx_referrals_receiving_clinic ON referrals(receiving_clinic_id);
CREATE INDEX idx_referrals_status ON referrals(status);
CREATE INDEX idx_referrals_expires ON referrals(access_expires_at);

CREATE INDEX idx_access_grants_patient ON access_grants(patient_id);
CREATE INDEX idx_access_grants_clinic ON access_grants(clinic_id);
CREATE INDEX idx_access_grants_referral ON access_grants(referral_id);
CREATE INDEX idx_access_grants_expires ON access_grants(expires_at);
CREATE INDEX idx_access_grants_active ON access_grants(revoked, expires_at);

-- Add audit actions for referrals
-- Note: These will be added to the AuditAction enum in Java code
