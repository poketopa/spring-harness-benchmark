# c1-waiting api-contract Requirement Variant

## Variant Metadata

- Unit: c1-waiting
- Variant type: api-contract
- Canonical requirement: `missions/roomescape-reservation-waiting/requirements/base/02-cycle1-waiting.md`
- Semantic checklist: `benchmarks/requirement-variants/checklists/c1-waiting.md`
- Status: self-reviewed candidate

## Requirement

Add backend API behavior for reservation waiting.

### Waiting creation

- A waiting request targets a slot defined by date, time, and theme.
- The request is valid only when the slot is already reserved by another user.
- The same user cannot create another waiting entry for the same slot.
- Waiting rank is determined by creation order within the same slot.

### Waiting cancellation

- A user can cancel a waiting entry that belongs to them.
- Cancelling a waiting entry must not cancel the reservation for the slot.

### My reservations read model

- Extend the existing my-reservation read API so it returns both reservations and waiting entries.
- Each item must expose a status that distinguishes reservation from waiting.
- Waiting items must expose the current waiting rank.

### Documentation and tests

- The target README must document the chosen endpoints, request/response shape, and rank decision.
- Requirement tests must verify creation, duplicate rejection, cancellation, combined lookup, and rank display.

## Semantic Equivalence Review

- [x] No required behavior was removed.
- [x] No required behavior was weakened.
- [x] No new required behavior was added.
- [x] Ambiguous wording was avoided or intentionally recorded.
- [x] API, test, error, and transaction expectations remain equivalent to the canonical requirement.

## Notes

- Intended wording difference: API-contract first.
- Risk of semantic drift: low.

