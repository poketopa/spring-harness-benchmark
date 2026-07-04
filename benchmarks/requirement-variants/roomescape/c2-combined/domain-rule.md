# c2-combined domain-rule Requirement Variant

## Variant Metadata

- Unit: c2-combined
- Variant type: domain-rule
- Canonical requirement: `missions/roomescape-reservation-waiting/requirements/base/03-cycle2-reservation-change-cancel-error.md` + `missions/roomescape-reservation-waiting/requirements/base/04-cycle2-waiting-approval.md`
- Semantic checklist: `benchmarks/requirement-variants/checklists/c2-combined.md`
- Status: self-reviewed candidate

## Requirement

Model the reservation domain around these rules.

### Reservation policy

- A reservation cannot be created for a past date/time.
- A reservation cannot duplicate another reservation with the same date, time, and theme.
- A reservation time cannot be deleted while reservations still use it.
- Invalid input is rejected. At minimum, blank names and invalid date formats are invalid.

### Error model

- Expected failures return intentional error responses.
- Expected failures include policy violations, invalid input, and missing resources.
- Expected failures must not leak as user-facing 500 responses.
- Error status codes and response body shape must be documented.

### Own reservation lifecycle

- A user can read their own reservations.
- A user can cancel their own reservation.
- A user can change their own reservation date and time.
- Past reservation cancellation and changes to an occupied slot are rejected.

### Waiting lifecycle

- A waiting entry can become a reservation by the selected approval strategy.
- The approval strategy is either automatic conversion or manual approval, and the choice must be documented.
- Remaining waiting ranks are recalculated after approval, reservation cancellation, or waiting cancellation.
- Data changes that must remain consistent are handled in one transaction.

## Semantic Equivalence Review

- [x] No required behavior was removed.
- [x] No required behavior was weakened.
- [x] No new required behavior was added.
- [x] Ambiguous wording was avoided or intentionally recorded.
- [x] API, test, error, and transaction expectations remain equivalent to the canonical requirement.

## Notes

- Intended wording difference: domain invariant and lifecycle oriented.
- Risk of semantic drift: low.

