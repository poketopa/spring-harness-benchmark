# c1-waiting acceptance-criteria Requirement Variant

## Variant Metadata

- Unit: c1-waiting
- Variant type: acceptance-criteria
- Canonical requirement: `missions/roomescape-reservation-waiting/requirements/base/02-cycle1-waiting.md`
- Semantic checklist: `benchmarks/requirement-variants/checklists/c1-waiting.md`
- Status: self-reviewed candidate

## Requirement

### Feature: Waiting for an occupied reservation slot

Given a reservation slot is identified by date, time, and theme,
When another user has already reserved that slot,
Then a user can create a waiting entry for that slot.

### Acceptance Criteria

- Waiting entries are grouped by the same date, time, and theme slot.
- Waiting rank follows the order in which waiting entries were created.
- A user cannot create more than one waiting entry for the same slot.
- A user can cancel only their own waiting entry.
- The user's reservation list also includes their waiting entries.
- Reservation and waiting entries are distinguishable by status.
- Waiting entries include the user's current rank for that slot.
- The target README documents the waiting API and the rank decision.

### Verification Criteria

- Add tests for waiting creation when a slot is already reserved.
- Add tests for duplicate waiting rejection.
- Add tests for waiting cancellation.
- Add tests for combined own reservation/waiting lookup.
- Add tests that verify waiting rank is returned.

## Semantic Equivalence Review

- [x] No required behavior was removed.
- [x] No required behavior was weakened.
- [x] No new required behavior was added.
- [x] Ambiguous wording was avoided or intentionally recorded.
- [x] API, test, error, and transaction expectations remain equivalent to the canonical requirement.

## Notes

- Intended wording difference: checklist and Given/When/Then style.
- Risk of semantic drift: low.

