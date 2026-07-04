# Roomescape JPA Auth Baseline

This is the controlled benchmark baseline for the `spring-usecase-implementation` harness project.

## Purpose

Use this project as the starting point for Spring convention benchmark loops.

The baseline intentionally includes:

- Spring Boot 4.1
- JPA + H2
- lightweight bearer-token authentication without Spring Security
- `Member`
- `Theme`
- `ReservationTime`
- `Reservation`
- code-based `ErrorCode` + `RoomescapeException`
- `ErrorResponse(code, message)`
- E2E/acceptance tests and a domain unit test

The baseline intentionally excludes:

- `Waiting`
- waiting rank calculation
- waiting cancellation
- waiting approval or auto-promotion
- manager authorization
- external payment API integration

## Verified Baseline

Run:

```bash
gradle test
```

Current result:

```text
BUILD SUCCESSFUL
```

## Existing APIs

- `POST /members`
- `POST /login`
- `POST /admin/themes`
- `GET /themes`
- `POST /admin/times`
- `GET /themes/{themeId}/times?date=yyyy-MM-dd`
- `POST /reservations`
- `GET /reservations/mine`

Authenticated APIs use:

```http
Authorization: Bearer {accessToken}
```

## First Benchmark Target

Use `missions/roomescape-reservation-waiting/requirements/base/02-cycle1-waiting.md`.

Recommended first scope:

- Add `Waiting` as a separate domain/entity.
- Allow a logged-in member to request waiting for an already reserved slot.
- Reject duplicate waiting by the same member for the same theme/date/time slot.
- Add behavior-focused E2E/acceptance tests and domain unit tests.

Keep API paths consistent with the existing project instead of forcing a pre-decided path.

## Cycle 1 Waiting Implementation

### Feature List

- A member can create a waiting entry for a slot already reserved by another member.
- A member cannot create duplicate waiting entries for the same theme, date, and time slot.
- A member can cancel their own waiting entry.
- `GET /reservations/mine` returns both confirmed reservations and waiting entries with a `status`.
- Waiting entries include the member's current rank for the slot.

### API Decisions

- `POST /reservations/waitings`
  - Authenticated.
  - Request: `date`, `timeId`, `themeId`.
  - Response: `201 Created` with waiting details and `rank`.
  - Fails with `WAITING_NOT_ALLOWED` when the slot has no reservation or belongs to the same member.
  - Fails with `DUPLICATE_WAITING` when the same member already waits for the same slot.
- `DELETE /reservations/waitings/{waitingId}`
  - Authenticated.
  - Response: `204 No Content`.
  - Missing or non-owned waiting entries are returned as `WAITING_NOT_FOUND`.
- `GET /reservations/mine`
  - Authenticated.
  - Response items include `status` as `RESERVED` or `WAITING`.
  - `rank` is present for waiting entries and `null` for confirmed reservations.

### Domain And Test Decisions

- Waiting is modeled as a separate `Waiting` entity because a waiting entry is not a confirmed reservation.
- Waiting use cases are handled by `WaitingService`, separate from reservation use cases.
- Waiting order uses creation time and id ordering. Rank is calculated by counting earlier waitings for the same slot.
- Duplicate waiting is guarded by both service validation and a database unique constraint.
- Tests are split into domain unit tests, focused controller/service/repository integration tests, and acceptance tests.

## Cycle 2 Reservation Change, Cancel, And Waiting Approval

### Feature List

- A member can change the date and time of their own reservation.
- A member can cancel their own reservation.
- Missing or non-owned reservations return `RESERVATION_NOT_FOUND`.
- Past reservations cannot be changed or cancelled.
- A reservation time cannot be deleted while reservations use it.
- When a reservation is cancelled, the first waiting for that slot is automatically promoted to a reservation.
- Waiting rank is recalculated from persisted waiting order after cancellation or promotion.

### API Decisions

- `PUT /reservations/{reservationId}`
  - Authenticated.
  - Request: `date`, `timeId`.
  - Response: `200 OK` with reservation details.
  - Fails with `DUPLICATE_RESERVATION` when the target slot is occupied.
- `DELETE /reservations/{reservationId}`
  - Authenticated.
  - Response: `204 No Content`.
  - Missing and non-owned reservations are both `RESERVATION_NOT_FOUND`.
- `DELETE /admin/times/{timeId}`
  - Response: `204 No Content`.
  - Fails with `RESERVATION_TIME_IN_USE` when a reservation uses the time.

### Transaction Decisions

- Reservation cancellation and first-waiting promotion are one transaction because the vacancy and promotion are one consistency boundary.
- `WaitingPromotionService` owns reservation-waiting conversion so `ReservationService` does not write waiting promotion details itself.
- `MyReservationService` owns the combined read model and reads reservation and waiting repositories directly.
