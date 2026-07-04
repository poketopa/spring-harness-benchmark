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
