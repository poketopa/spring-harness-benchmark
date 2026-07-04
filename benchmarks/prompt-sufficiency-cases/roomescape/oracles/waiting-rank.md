# Waiting Rank Hidden Oracle

## Feature

Members can wait for an already reserved slot and see their waiting rank.

## Required Behavior

- Waiting can be created only for a reserved slot.
- A member cannot wait on their own reservation.
- A member cannot create duplicate waiting for the same date/time/theme.
- Waiting rank is based on request order with a deterministic tie-breaker.
- My reservations view includes both reservations and waiting entries with status/rank.
- Duplicate waiting is protected by a database constraint or equivalent final guard.

## Required Tests

- Waiting creation for an occupied slot returns rank.
- Waiting for an available slot is rejected.
- Waiting for own reservation is rejected.
- Duplicate waiting is rejected.
- Rank ordering is verified.
- Race-sensitive duplicate waiting has a final persistence guard; real concurrent test is preferred for concurrency-specific prompts.

## Architecture Checks

- Rank calculation stays in service/repository query, not controller.
- Repository does not receive request DTOs.
- Domain naming uses waiting/reservation language clearly.
