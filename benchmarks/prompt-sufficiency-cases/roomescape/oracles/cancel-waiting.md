# Cancel Waiting Hidden Oracle

## Feature

Reservation cancellation should keep reservations and waiting entries consistent.

## Required Behavior

- A member can cancel only their own reservation.
- Past reservations cannot be cancelled.
- When the chosen policy is automatic promotion, cancellation promotes the first waiting entry for the same date/time/theme into a reservation.
- Remaining waiting ranks for that slot are recalculated after promotion or removal.
- Promotion and deletion happen in one transaction; failure to create the promoted reservation rolls back waiting deletion.
- Existing public cancellation API and error response style are preserved.

## Required Tests

- Own reservation cancellation succeeds.
- Other-member cancellation is rejected or hidden according to existing resource ownership style.
- Past cancellation is rejected.
- Cancellation with waiting promotes the first waiting entry.
- Remaining waiting ranks are updated.
- Promotion failure rolls back related data changes.

## Architecture Checks

- Cancellation orchestration stays in service/usecase layer.
- Waiting promotion is not implemented in controller.
- Repository methods do not receive request DTOs.
- Transaction boundary is explicit and documented in run note or project docs when implemented.
