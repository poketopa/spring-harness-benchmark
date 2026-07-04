# manager-authz api-contract Requirement Variant

## Variant Metadata

- Unit: manager-authz
- Variant type: api-contract
- Canonical requirement: `missions/roomescape-reservation-waiting/requirements/add-ons/auth-authorization/03-store-manager-authorization.md`
- Semantic checklist: `benchmarks/requirement-variants/checklists/manager-authz.md`
- Status: self-reviewed candidate

## Requirement

Add authorization behavior to manager reservation APIs.

### Access rules

- Manager reservation list requests return only reservations for the logged-in manager's store.
- Manager reservation change requests are allowed only for reservations in the logged-in manager's store.
- Manager reservation delete requests are allowed only for reservations in the logged-in manager's store.
- Requests targeting another store's reservation are rejected.

### Identity rules

- The system can determine whether the logged-in user is a manager.
- The system can determine which store the manager manages.
- The system can determine which store owns the reservation being accessed.

### Failure rules

- Missing login is an authentication failure.
- Logged-in users without permission are authorization failures.
- Authentication and authorization failures use distinguishable handling.

### Design rules

- Authorization logic should not be duplicated across controllers.
- The target README must explain the authorization boundary and key policy choices.

## Semantic Equivalence Review

- [x] No required behavior was removed.
- [x] No required behavior was weakened.
- [x] No new required behavior was added.
- [x] Ambiguous wording was avoided or intentionally recorded.
- [x] API, test, error, and transaction expectations remain equivalent to the canonical requirement.

## Notes

- Intended wording difference: API behavior first.
- Risk of semantic drift: low.

