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

`/admin/**` APIs require an authenticated member with `ADMIN` role. Admin accounts are provisioned outside the public API; tests seed one as a fixture.

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

- `POST /waitings`
  - Authenticated.
  - Request: `date`, `timeId`, `themeId`.
  - Response: `201 Created` with waiting details and `rank`.
  - Fails with `WAITING_NOT_ALLOWED` when the slot has no reservation or belongs to the same member.
  - Fails with `DUPLICATE_WAITING` when the same member already waits for the same slot.
- `DELETE /waitings/{waitingId}`
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

## Cycle 2 Reservation Change, Cancel, And Error Handling

### Feature List

- A member can find confirmed reservations and waiting entries through `GET /reservations/mine`.
- A member can cancel their own confirmed reservation.
- A member cannot cancel a confirmed reservation that has already passed.
- A member can change the date and time of their own confirmed reservation.
- A member cannot change a confirmed reservation to a past date/time or an occupied slot.
- A member cannot change or cancel another member's confirmed reservation.
- A reservation time cannot be deleted while a confirmed reservation or waiting entry uses it.
- Invalid request values and malformed date/time inputs return the same error response body style as service policy errors.

### Error Response Spec

All handled API errors return:

```json
{
  "code": "ERROR_CODE",
  "message": "Human-readable message"
}
```

### API Decisions

- `PUT /reservations/{reservationId}`
  - Authenticated.
  - Request: `date`, `timeId`.
  - Response: `200 OK` with changed reservation details.
  - Missing or non-owned reservations are returned as `RESERVATION_NOT_FOUND`.
  - Fails with `PAST_RESERVATION` when the original reservation has passed or the requested date/time is in the past.
  - Fails with `DUPLICATE_RESERVATION` when another reservation already occupies the requested date/time/theme slot.
- `DELETE /reservations/{reservationId}`
  - Authenticated.
  - Response: `204 No Content`.
  - Missing or non-owned reservations are returned as `RESERVATION_NOT_FOUND`.
  - Fails with `PAST_RESERVATION` when the reservation has already passed.
- `DELETE /admin/times/{timeId}`
  - Response: `204 No Content`.
  - Missing reservation times are returned as `RESERVATION_TIME_NOT_FOUND`.
  - Fails with `RESERVATION_TIME_IN_USE` when a reservation or waiting entry still uses the time.
- Invalid JSON body date/time values, invalid query date values, missing required body fields, and invalid path/query types return `INVALID_INPUT`.

### Domain And Test Decisions

- Reservation keeps schedule mutation as domain behavior through `changeSchedule`.
- Reservation use cases remain in `ReservationService`; waiting use cases remain in `WaitingService`.
- Duplicate changed reservations are guarded by both service validation and the existing reservation unique constraint.
- Tests are split into reservation domain behavior, service orchestration, repository query behavior, controller validation/error contract, and acceptance user flows.

## Cycle 2 Waiting Approval

### Feature List

- The project uses automatic waiting approval instead of adding manual manager approval APIs.
- When a confirmed reservation is canceled, the first waiting entry for that slot is converted into a confirmed reservation.
- When a confirmed reservation is changed to another date/time, the first waiting entry for the previous slot is converted into a confirmed reservation.
- The promoted waiting entry is removed from the waiting list.
- Remaining waiting ranks are recalculated by the existing rank query, so the next waiting entry becomes rank 1.

### API Decisions

- No new public approval API is added for this cycle.
- `DELETE /reservations/{reservationId}`
  - Still returns `204 No Content`.
  - Runs cancellation and first-waiting approval in one transaction.
- `PUT /reservations/{reservationId}`
  - Still returns `200 OK` with the changed reservation.
  - If the old slot becomes vacant, the first waiting entry for that old slot is approved in the same transaction.

### Transaction Decisions

- Reservation cancellation/change and waiting approval are part of one use case-level transaction because the slot must not be left empty while a waiting entry for that slot remains.
- `ReservationService` keeps the reservation workflow, `WaitingService` keeps member/admin waiting use cases, and `WaitingPromotionService` owns first-waiting promotion.
- The old reservation row is flushed before creating the promoted reservation so the database unique constraint on reservation slot remains the final guard.

### Domain And Test Decisions

- `Waiting.approve()` creates the confirmed `Reservation` from the waiting member and slot.
- `Reservation.hasSchedule(...)` prevents approval from running when a reservation change request keeps the same slot.
- Reservation create/change validation uses named validation helpers so service use case steps read consistently.
- Tests cover domain behavior, first-waiting repository lookup, service-level promotion for cancel/change, transaction rollback on approval failure, and an acceptance flow showing rank recalculation.

## Auth Authorization Add-On

### 01/02 Web Login And Mobile Auth Audit

- The project already uses `POST /login` and `Authorization: Bearer {accessToken}`.
- `LoginMemberArgumentResolver` extracts the bearer token and delegates token, session, and member validation to `AuthService`.
- `AuthenticatedMemberService` centralizes service-side `LoginMember` to `Member` loading and `UNAUTHORIZED` conversion.
- Missing or invalid tokens return `UNAUTHORIZED` through the existing `ErrorResponse(code, message)` style.
- The same bearer-token contract works for browser and mobile clients because authentication state is sent on each request header.

### 03 Store Manager Authorization Feature List

- Admin can create manager members through `POST /admin/managers`.
- Admin can create stores through `POST /admin/stores`.
- Admin can attach a theme to a store by passing `storeId` to `POST /admin/themes`.
- All `/admin/**` APIs require an authenticated admin token. Admin account bootstrap is not exposed as a public application endpoint.
- A manager can list reservations for their own store through `GET /manager/reservations`.
- A manager can change reservations for their own store through `PUT /manager/reservations/{reservationId}`.
- A manager can cancel reservations for their own store through `DELETE /manager/reservations/{reservationId}`.
- A manager cannot manage reservations for another store.
- A normal member cannot use manager reservation APIs.
- Authentication failure and authorization failure are separated as `UNAUTHORIZED` and `FORBIDDEN`.
- Admin and manager authorization are both handled by dedicated authorization services.
- Controllers are grouped by resource: reservation member APIs and manager reservation APIs live in `ReservationController`; admin actions stay in their resource controllers.

### API Decisions

- `POST /admin/managers`
  - Authenticated admin only.
  - Request: `name`, `email`, `password`.
  - Response: `201 Created` with member details.
- `POST /admin/stores`
  - Authenticated admin only.
  - Request: `name`, `managerId`.
  - Response: `201 Created` with store details.
  - Fails with `MEMBER_NOT_FOUND` when the manager id does not exist.
  - Fails with `INVALID_INPUT` when the member is not a manager.
  - Fails with `MANAGER_ALREADY_HAS_STORE` when a manager is already assigned to a store.
- `POST /admin/themes`
  - Authenticated admin only.
  - Existing request fields are preserved.
  - Optional `storeId` attaches the theme to a store.
  - Missing or invalid `storeId` fails with `STORE_NOT_FOUND`.
- `GET /manager/reservations`
  - Authenticated manager only.
  - Response: `200 OK` with reservations for the manager's store.
- `PUT /manager/reservations/{reservationId}`
  - Authenticated manager only.
  - Request: `date`, `timeId`.
  - Response: `200 OK` with changed reservation details.
  - Fails with `FORBIDDEN` when the reservation belongs to another store.
- `DELETE /manager/reservations/{reservationId}`
  - Authenticated manager only.
  - Response: `204 No Content`.
  - Fails with `FORBIDDEN` when the reservation belongs to another store.

### Authorization Decisions

- A manager manages exactly one store in this benchmark.
- `Store` is modeled as a separate domain because a store is not the same concept as a theme.
- `Theme` optionally belongs to a store to preserve existing theme creation tests and APIs.
- `ReservationService` owns manager reservation workflows because they are reservation management use cases.
- Controller does not decide authorization policy; Service loads the member/reservation and delegates the store ownership check to domain behavior.
- Authorization failure uses `FORBIDDEN` instead of hiding cross-store reservations as not found, because this requirement explicitly asks authentication and authorization failures to be distinguished.

### Domain And Test Decisions

- `Member.isManager()`, `Store.isManagedBy(...)`, `Theme.isManagedBy(...)`, and `Reservation.isManagedBy(...)` express the authorization chain.
- Tests cover domain authorization behavior, store lookup, store-scoped reservation repository query, service authorization, controller auth/authz failures, and acceptance flow for manager reservation management.
- `ManagerAuthorizationService` owns manager role and store-ownership authorization, matching `AdminAuthorizationService` for admin role checks.

## Concurrent Login Prevention

### Feature List

- The project prevents two active logins for the same account.
- A new login wins over the previous login.
- The previous bearer token becomes invalid immediately after the new login succeeds.
- The latest bearer token continues to work for authenticated APIs.

### Policy Decisions

- The project keeps the existing bearer-token API contract, but tokens are no longer fully stateless.
- `AuthSession` stores the current session key per member.
- A token contains both the member id and the session key.
- On login, `AuthService` rotates the member's current session key and issues a token for the new key.
- On authenticated requests, `LoginMemberArgumentResolver` checks that the token session key matches the current session key in the database.
- Missing, malformed, or stale-session tokens return `UNAUTHORIZED`.

### Trade-Offs

- This satisfies the operational requirement to invalidate older tokens, but each authenticated request now performs a session lookup.
- Server-side session state means horizontal scaling needs shared persistence for `AuthSession`.
- Existing tokens issued before this change are intentionally treated as invalid because they do not contain a session key.

### Domain And Test Decisions

- `AuthSession` is modeled separately from `Member` because login state changes independently from member profile data.
- `AuthSession.rotate(...)` expresses the "new login wins" state transition.
- Tests cover session key rotation, stale token rejection, and successful use of the newest token.

## JPA Add-On Completion

### Scope Decision

- External API requirements were removed from this project scope.
- JPA add-on requirements are handled in this project because the current implementation is already JPA-based.
- Most JPA add-on behavior was already present through the base and auth benchmark work.
- The remaining implementation gap was admin waiting management and compatibility with the add-on API paths.

### 01 JPA Migration Audit

- `build.gradle` uses `spring-boot-starter-data-jpa`.
- Domain classes are mapped as JPA entities.
- Repositories use Spring Data `JpaRepository`.
- Reservation, waiting, auth session, store, member, theme, and time relationships are represented with JPA object references.
- No `JdbcTemplate`, `KeyHolder`, or `SimpleJdbcInsert` repository remains in the target project.

### 02 My Reservations

- Existing API: `GET /reservations/mine`.
- The compatibility alias `GET /reservations-mine` is not exposed because the project keeps one canonical REST path.
- Response includes both confirmed reservations and waiting entries.
- `MyReservationService` owns the combined read model and builds `MyReservationResponse` from repositories.

### 03 Waiting

- Standard API: `POST /waitings`, `DELETE /waitings/{waitingId}`.
- The old nested path `POST /reservations/waitings` is not exposed because waiting is modeled as a reservation-level peer concept.
- Waiting is a separate `Waiting` entity.
- Duplicate waiting is protected by service validation and a DB unique constraint.
- Rank uses `WaitingRepository.countEarlierWaitings(...)`, not full-list index lookup.
- Waiting member lists use `@EntityGraph` for theme/time fetches.

### 04 Waiting Management

- Admin can list all waitings through `GET /admin/waitings`.
- Admin can cancel any waiting through `DELETE /admin/waitings/{waitingId}`.
- Both APIs require an authenticated admin token.
- Automatic approval remains the chosen approval strategy.
- Reservation cancellation/change and first-waiting promotion are handled in one use case transaction through `WaitingPromotionService`.

### Test Decisions

- `JpaAddOnAcceptanceTest` covers the canonical JPA add-on behavior and admin waiting management.
- Existing repository/service/domain/acceptance tests cover JPA mapping, waiting rank, automatic approval, and transaction behavior.

## Test Convention Correction

- Domain constructors now reject single-object invalid state such as blank names, null schedules, null timestamps, and non-manager store ownership.
- Storage-dependent rules, such as duplicate reservation slots and waiting eligibility, remain in services/repositories instead of domain constructors.
- Domain tests run without Spring and cover constructor failures plus true/false domain behavior.
- Service unit tests cover core orchestration failures with mocks and fixed `Clock` where time matters.
- Repository tests use the JPA slice through `@DataJpaTest`.
- Controller slice tests use `@WebMvcTest` for HTTP validation and error response shape.
- Full `RANDOM_PORT` tests are named `*AcceptanceTest` or `*HttpIntegrationTest`, not `*ControllerIntegrationTest`.
