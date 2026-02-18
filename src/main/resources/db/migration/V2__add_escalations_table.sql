-- Escalations table
CREATE TABLE escalations (
    id BIGSERIAL PRIMARY KEY,
    consultation_id BIGINT NOT NULL,
    referral_id VARCHAR(255) NOT NULL,
    specialist_type VARCHAR(100) NOT NULL,
    urgency_level VARCHAR(50) NOT NULL DEFAULT 'ROUTINE',
    status VARCHAR(50) NOT NULL DEFAULT 'SUBMITTED',
    case_summary TEXT,
    referral_notes TEXT,
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    active BOOLEAN DEFAULT true,
    CONSTRAINT fk_escalation_consultation FOREIGN KEY (consultation_id) REFERENCES consultations(id)
);

-- Indexes
CREATE INDEX idx_escalations_consultation ON escalations(consultation_id);
CREATE INDEX idx_escalations_status ON escalations(status);
CREATE INDEX idx_escalations_referral_id ON escalations(referral_id);
