# Semantic Checklist: c2-combined

Canonical requirements:

- `missions/roomescape-reservation-waiting/requirements/base/03-cycle2-reservation-change-cancel-error.md`
- `missions/roomescape-reservation-waiting/requirements/base/04-cycle2-waiting-approval.md`

## Required Behaviors

- Creating a reservation in the past is rejected.
- Creating a duplicate reservation for the same date, time, and theme is rejected.
- Deleting a reservation time that has reservations is rejected.
- Invalid input is rejected.
- Invalid input includes at least blank name and invalid date format.
- Service policy violations, invalid input, and missing resources return intended error responses.
- User-visible 500 errors are avoided for expected failures.
- Error response body structure is explicitly decided.
- A user can read their own reservations.
- A user can cancel their own reservation.
- A user can change the date and time of their own reservation.
- Change/cancel error cases follow the same error response rules.
- Past reservation cancellation is rejected.
- Changing to an already occupied slot is rejected.
- Waiting approval converts a waiting entry into a reservation.
- Automatic conversion or manual approval is explicitly selected.
- If automatic conversion is selected, cancelling a reservation promotes the first waiting entry.
- Remaining waiting ranks are recalculated after approval or cancellation.
- Edge cases not explicitly listed in the requirement are identified and handled.

## Required Design Decisions

- Reservation change/cancel API contract is documented.
- Error response status and body are documented.
- Approval strategy and transaction boundary are documented.
- Data changes that must commit or roll back together are handled in one transaction.

## Required Verification

- Tests cover reservation policy failures.
- Tests cover error response consistency.
- Tests cover own reservation read/change/cancel flows.
- Tests cover waiting promotion or approval.
- Tests cover waiting rank recalculation.
- Tests cover transaction consistency for approval failure.

