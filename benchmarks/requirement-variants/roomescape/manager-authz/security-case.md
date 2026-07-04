# manager-authz security-case Requirement Variant

## Variant Metadata

- Unit: manager-authz
- Variant type: security-case
- Canonical requirement: `missions/roomescape-reservation-waiting/requirements/add-ons/auth-authorization/03-store-manager-authorization.md`
- Semantic checklist: `benchmarks/requirement-variants/checklists/manager-authz.md`
- Status: self-reviewed candidate

## Requirement

Prevent horizontal privilege escalation in store reservation management.

### Risk

A logged-in manager may try to access a reservation that belongs to another store. Authentication alone is not enough because the user identity must be checked against the reservation's store ownership.

### Required controls

- Confirm that the requester is authenticated.
- Confirm that the authenticated user is a store manager.
- Resolve the store managed by that manager.
- Resolve the store that owns the target reservation.
- Allow reservation list/change/delete only when the manager's store matches the reservation's store.
- Reject cross-store access.
- Treat unauthenticated access as authentication failure.
- Treat authenticated but unauthorized access as authorization failure.
- Keep authentication and authorization failures distinguishable.

### Implementation constraints

- Do not scatter authorization checks across controller methods.
- Document where authorization decisions live.
- Document the manager-store relationship.
- Document how login identity is connected to manager/store domain identity.
- Document whether forbidden resources are reported as forbidden or hidden.

## Semantic Equivalence Review

- [x] No required behavior was removed.
- [x] No required behavior was weakened.
- [x] No new required behavior was added.
- [x] Ambiguous wording was avoided or intentionally recorded.
- [x] API, test, error, and transaction expectations remain equivalent to the canonical requirement.

## Notes

- Intended wording difference: threat/risk oriented.
- Risk of semantic drift: low.

