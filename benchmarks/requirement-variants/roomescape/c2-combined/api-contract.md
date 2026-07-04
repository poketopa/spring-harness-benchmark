# c2-combined api-contract Requirement Variant

## Variant Metadata

- Unit: c2-combined
- Variant type: api-contract
- Canonical requirement: `missions/roomescape-reservation-waiting/requirements/base/03-cycle2-reservation-change-cancel-error.md` + `missions/roomescape-reservation-waiting/requirements/base/04-cycle2-waiting-approval.md`
- Semantic checklist: `benchmarks/requirement-variants/checklists/c2-combined.md`
- Status: self-reviewed candidate

## Requirement

Define and implement backend API behavior for reservation policy, errors, own reservation management, and waiting approval.

### Required API outcomes

- Reservation creation rejects past slots.
- Reservation creation rejects duplicate date/time/theme slots.
- Reservation time deletion rejects times that are used by reservations.
- Invalid inputs are rejected, including blank names and invalid date format.
- Own reservation lookup returns the authenticated user's reservations.
- Own reservation cancellation cancels the user's own reservation.
- Own reservation change updates the user's own reservation date and time.
- Change and cancellation errors follow the same error response convention.
- Waiting approval converts waiting to reservation using a documented automatic or manual strategy.
- Waiting rank is recalculated after approval and cancellation.

### Error response contract

- Decide status codes for policy violation, invalid input, and missing resource cases.
- Decide one response body format for expected errors.
- Prevent expected failures from surfacing as generic 500 responses.
- Document the contract in the target README.

### Verification contract

- Add tests for policy rejection.
- Add tests for error response consistency.
- Add tests for own reservation read/change/cancel.
- Add tests for waiting conversion and rank recalculation.
- Add tests that protect transaction consistency for waiting approval.

## Semantic Equivalence Review

- [x] No required behavior was removed.
- [x] No required behavior was weakened.
- [x] No new required behavior was added.
- [x] Ambiguous wording was avoided or intentionally recorded.
- [x] API, test, error, and transaction expectations remain equivalent to the canonical requirement.

## Notes

- Intended wording difference: API and error contract first.
- Risk of semantic drift: low.

