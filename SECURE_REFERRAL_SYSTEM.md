# Secure Patient Referral System - Clinic Interoperability

## Overview
The Secure Patient Referral System enables healthcare providers to safely refer patients between clinics while maintaining strict data privacy and access control. This feature eliminates clinic data silos and enables continuity of care across the healthcare network.

**Status**: ✅ Implemented  
**Compliance**: HIPAA-compliant with full audit trail

---

## Core Concept

When a provider creates a referral, the system generates a temporary **Access Grant** that allows authorized staff at the receiving clinic to view the referred patient's medical records. Access is:
- **Explicit**: Only granted through formal referrals
- **Limited**: Time-bound and patient-specific
- **Auditable**: All access logged in audit system
- **Revocable**: Can be cancelled by referring provider or SUPER_ADMIN

---

## Key Features

### 1. Referral Creation
Providers can initiate referrals from a patient's profile with:
- Referring clinic (auto-detected from provider)
- Receiving clinic selection
- Reason for referral (required)
- Optional clinical notes
- Access duration (7-90 days)

### 2. Temporary Access Grants
Once created:
- Receiving clinic gains **read-only** access to patient records
- Access is **patient-specific** (not clinic-wide)
- Access **automatically expires** after defined period
- Access can be **manually revoked** if needed

### 3. Referral Lifecycle

```
PENDING → ACCEPTED → COMPLETED
   ↓          ↓
CANCELLED  EXPIRED
```

- **PENDING**: Referral created, awaiting acceptance
- **ACCEPTED**: Receiving clinic acknowledged referral
- **COMPLETED**: Consultation finished
- **EXPIRED**: Access window ended (auto-updated hourly)
- **CANCELLED**: Referring provider revoked referral

### 4. Access Control
- Access granted only to receiving clinic's authorized providers
- Patient-specific access (no blanket clinic access)
- Read-only by default (no editing permissions)
- Automatic expiration enforcement
- Manual revocation capability

### 5. Audit Logging
All referral activities are logged:
- `CREATE_REFERRAL` - Referral initiated
- `ACCEPT_REFERRAL` - Receiving clinic accepts
- `COMPLETE_REFERRAL` - Consultation completed
- `CANCEL_REFERRAL` - Referral cancelled
- `REVOKE_ACCESS_GRANT` - Access manually revoked
- `ACCESS_VIA_REFERRAL` - Patient data accessed via referral

### 6. SUPER_ADMIN Oversight
SUPER_ADMIN can:
- View all referrals across all clinics
- Audit which clinics accessed referred patient data
- Revoke referral access if necessary
- Monitor cross-clinic collaboration patterns

---

## Database Schema

### Referrals Table
```sql
CREATE TABLE referrals (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    referring_clinic_id BIGINT NOT NULL,
    receiving_clinic_id BIGINT NOT NULL,
    referring_provider_id BIGINT NOT NULL,
    reason VARCHAR(500) NOT NULL,
    notes TEXT,
    access_expires_at TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL,
    accepted_at TIMESTAMP,
    accepted_by_provider_id BIGINT,
    completed_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    cancellation_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

### Access Grants Table
```sql
CREATE TABLE access_grants (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    clinic_id BIGINT NOT NULL,
    referral_id BIGINT NOT NULL,
    permission_type VARCHAR(20) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at TIMESTAMP,
    revoked_by VARCHAR(255),
    revocation_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

---

## API Endpoints

### Create Referral
```http
POST /api/v1/referrals
Authorization: Bearer <token>
Content-Type: application/json

{
  "patientId": 123,
  "receivingClinicId": 456,
  "reason": "Specialist consultation for cardiac evaluation",
  "notes": "Patient has history of hypertension, requires cardiologist review",
  "accessDurationDays": 30
}
```

**Response**: `201 Created`
```json
{
  "id": 789,
  "patientId": 123,
  "patientName": "John Doe",
  "referringClinicId": 100,
  "referringClinicName": "City Health Center",
  "receivingClinicId": 456,
  "receivingClinicName": "Regional Specialist Hospital",
  "referringProviderId": 50,
  "referringProviderName": "Dr. Smith",
  "reason": "Specialist consultation for cardiac evaluation",
  "notes": "Patient has history of hypertension...",
  "accessExpiresAt": "2026-04-07T10:30:00",
  "status": "PENDING",
  "createdAt": "2026-03-07T10:30:00"
}
```

### Accept Referral
```http
PUT /api/v1/referrals/{id}/accept
Authorization: Bearer <token>
```

**Response**: `200 OK` (Updated referral with `status: "ACCEPTED"`)

### Complete Referral
```http
PUT /api/v1/referrals/{id}/complete
Authorization: Bearer <token>
```

**Response**: `200 OK` (Updated referral with `status: "COMPLETED"`)

### Cancel Referral
```http
PUT /api/v1/referrals/{id}/cancel?reason=Patient%20no%20longer%20requires%20referral
Authorization: Bearer <token>
```

**Response**: `200 OK` (Updated referral with `status: "CANCELLED"`)

### Get Referral by ID
```http
GET /api/v1/referrals/{id}
Authorization: Bearer <token>
```

**Response**: `200 OK` (Referral details)

### Get Patient Referrals
```http
GET /api/v1/referrals/patient/{patientId}?page=0&size=20
Authorization: Bearer <token>
```

**Response**: `200 OK` (Paginated list of referrals)

### Get Sent Referrals (Current Clinic)
```http
GET /api/v1/referrals/sent?page=0&size=20
Authorization: Bearer <token>
```

**Response**: `200 OK` (Referrals sent by current clinic)

### Get Received Referrals (Current Clinic)
```http
GET /api/v1/referrals/received?page=0&size=20
Authorization: Bearer <token>
```

**Response**: `200 OK` (Referrals received by current clinic)

### Get Active Access Grants for Patient
```http
GET /api/v1/referrals/patient/{patientId}/access-grants
Authorization: Bearer <token>
```

**Response**: `200 OK`
```json
[
  {
    "id": 1,
    "patientId": 123,
    "patientName": "John Doe",
    "clinicId": 456,
    "clinicName": "Regional Specialist Hospital",
    "referralId": 789,
    "permissionType": "READ_ONLY",
    "expiresAt": "2026-04-07T10:30:00",
    "revoked": false,
    "createdAt": "2026-03-07T10:30:00"
  }
]
```

---

## Example Workflow

### Scenario: Dr. Mensah refers patient to specialist

1. **Create Referral** (Clinic A → Clinic B)
   ```
   Dr. Mensah at City Health Center refers patient John Doe 
   to Regional Specialist Hospital for cardiac evaluation.
   Access duration: 30 days
   ```

2. **System Actions**
   - Creates `Referral` record with status `PENDING`
   - Creates `AccessGrant` for Clinic B with `READ_ONLY` permission
   - Logs `CREATE_REFERRAL` audit event

3. **Accept Referral** (Clinic B)
   ```
   Dr. Johnson at Regional Specialist Hospital accepts the referral
   ```
   - Updates referral status to `ACCEPTED`
   - Records accepting provider
   - Logs `ACCEPT_REFERRAL` audit event

4. **Access Patient Data** (Clinic B)
   ```
   Dr. Johnson views John Doe's medical history
   ```
   - `ClinicAccessGuard` checks for active access grant
   - Access granted via referral
   - Logs `ACCESS_VIA_REFERRAL` audit event

5. **Complete Referral** (Clinic B)
   ```
   After consultation, Dr. Johnson marks referral as complete
   ```
   - Updates referral status to `COMPLETED`
   - Access grant remains valid until expiration
   - Logs `COMPLETE_REFERRAL` audit event

6. **Automatic Expiration**
   ```
   After 30 days, scheduled task expires the referral
   ```
   - Status updated to `EXPIRED`
   - Access automatically revoked

---

## Security Features

### Access Control
- **Clinic Isolation**: Default access limited to own clinic
- **Referral-Based Access**: Explicit grants required for cross-clinic access
- **Time-Bound**: All access grants have expiration dates
- **Read-Only**: Default permission prevents data modification
- **Revocable**: Referring provider can cancel anytime

### Audit Trail
Every referral action is logged with:
- WHO: Provider email, name, role
- WHAT: Action performed (create, accept, complete, cancel)
- WHEN: Timestamp
- WHICH: Patient and referral IDs
- WHERE: IP address
- WHY: Reason (for cancellations)

### Validation Rules
- Cannot refer to same clinic
- Access duration: 7-90 days
- Only referring provider can cancel
- Only receiving clinic can accept/complete
- Expired referrals cannot be accepted
- Only PENDING/ACCEPTED referrals can be cancelled

---

## Scheduled Tasks

### Expire Old Referrals
**Schedule**: Every hour (`0 0 * * * *`)

**Function**: Automatically updates status of referrals where `access_expires_at` has passed:
- Changes status from `PENDING` or `ACCEPTED` to `EXPIRED`
- Prevents acceptance of expired referrals
- Maintains data integrity

---

## Benefits

### For Healthcare Providers
- Seamless patient referrals to specialists
- Secure access to patient history
- Clear referral tracking and status
- Reduced administrative overhead

### For Patients
- Continuity of care across facilities
- No need to carry physical records
- Privacy maintained through controlled access
- Transparent access tracking

### For Healthcare System
- Eliminates clinic data silos
- Enables specialist network collaboration
- Maintains strict privacy controls
- Complete audit trail for compliance
- Improves care coordination

---

## Integration with Existing Features

### ClinicAccessGuard Enhancement
The `ClinicAccessGuard` now checks for referral-based access:

```java
// Original: Only same-clinic access
if (provider.getClinic().getId().equals(patient.getClinic().getId())) {
    return; // Access granted
}

// Enhanced: Check for active referral
boolean hasReferralAccess = accessGrantRepository.hasActiveAccess(
    provider.getClinic().getId(),
    patient.getId(),
    LocalDateTime.now()
);
```

This means all existing patient data endpoints automatically respect referral-based access without code changes.

### Audit Logging Integration
All referral actions are logged using the existing `AuditService`:
- Consistent audit format
- Same retention policies (7 years)
- SUPER_ADMIN oversight
- Searchable and filterable

---

## Future Enhancements

### Potential Extensions
1. **Bi-directional Referrals**: Allow receiving clinic to send updates back
2. **Document Sharing**: Attach specific documents to referrals
3. **Referral Templates**: Pre-defined referral types with standard fields
4. **Notification System**: Alert receiving clinic of new referrals
5. **Referral Analytics**: Track referral patterns and outcomes
6. **Write Permissions**: Allow receiving clinic to add consultation notes
7. **Multi-Clinic Referrals**: Refer to multiple specialists simultaneously

---

## Compliance Notes

### HIPAA Compliance
- ✅ Minimum necessary access (patient-specific)
- ✅ Time-limited access grants
- ✅ Complete audit trail
- ✅ Access revocation capability
- ✅ Secure authentication required

### Data Privacy
- ✅ Explicit consent model (referral = consent)
- ✅ Purpose limitation (specific reason required)
- ✅ Access transparency (audit logs)
- ✅ Right to revoke (cancellation)

---

## Technical Implementation

### Entities
- `Referral` - Core referral record
- `AccessGrant` - Temporary access permission
- `ReferralStatus` - Lifecycle enum
- `PermissionType` - Access level enum

### Repositories
- `ReferralRepository` - Referral data access
- `AccessGrantRepository` - Access grant queries

### Services
- `ReferralService` - Business logic and orchestration

### Controllers
- `ReferralController` - REST API endpoints

### Security
- `ClinicAccessGuard` - Enhanced with referral checking

### Database
- Migration V10: Creates referrals and access_grants tables

---

## Testing Recommendations

### Unit Tests
- Referral creation validation
- Access grant expiration logic
- Status transition rules
- Permission checking

### Integration Tests
- End-to-end referral workflow
- Cross-clinic access verification
- Audit logging validation
- Scheduled task execution

### Security Tests
- Unauthorized access attempts
- Expired referral access
- Cross-clinic boundary enforcement
- SUPER_ADMIN override verification

---

## Monitoring and Maintenance

### Key Metrics
- Active referrals count
- Average referral duration
- Acceptance rate
- Completion rate
- Cancellation reasons
- Cross-clinic collaboration patterns

### Maintenance Tasks
- Monitor expired referral cleanup
- Review audit logs for suspicious patterns
- Validate access grant integrity
- Performance optimization for large datasets

---

## Support and Troubleshooting

### Common Issues

**Issue**: Referral not appearing for receiving clinic  
**Solution**: Check referral status is PENDING or ACCEPTED, verify clinic ID

**Issue**: Access denied despite active referral  
**Solution**: Verify access grant not revoked, check expiration date

**Issue**: Cannot accept expired referral  
**Solution**: Contact referring clinic to create new referral

**Issue**: Referral stuck in PENDING  
**Solution**: Receiving clinic must explicitly accept referral

---

## Conclusion

The Secure Patient Referral System enables safe, auditable cross-clinic collaboration while maintaining strict patient privacy controls. It eliminates data silos, improves continuity of care, and provides complete transparency through comprehensive audit logging.
