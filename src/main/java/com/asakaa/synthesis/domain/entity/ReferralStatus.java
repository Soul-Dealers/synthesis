package com.asakaa.synthesis.domain.entity;

public enum ReferralStatus {
    PENDING,      // Referral created but not yet accepted
    ACCEPTED,     // Receiving clinic acknowledges the referral
    COMPLETED,    // Consultation finished
    EXPIRED,      // Access window ended
    CANCELLED     // Referring provider revoked the referral
}
