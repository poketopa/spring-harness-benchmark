# Roomescape JPA Auth Manager Authorization Regen V2

This is the manager authorization add-on regeneration benchmark target for the `spring-usecase-implementation`
harness project.

## Purpose

Use this project to verify whether the updated skill extends the cycle2 reservation/waiting result with manager
authorization while preserving the owner's Spring conventions.

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

## Cycle 2 Reservation Change, Cancel, Error Handling

### Feature List

- A member can change the date and time of their own reservation.
- A member can cancel their own reservation.
- A member cannot create, change, or cancel a past reservation.
- A member cannot change a reservation to an occupied theme/date/time slot.
- Missing or non-owned reservations are returned as `RESERVATION_NOT_FOUND`.
- A reservation time cannot be deleted while a reservation or waiting entry uses it.
- Invalid date formats, invalid JSON bodies, missing parameters, and Bean Validation failures return `ErrorResponse(code, message)`.

### API Decisions

- `PUT /reservations/{reservationId}`
  - Authenticated.
  - Request: `date`, `timeId`.
  - Response: `200 OK` with changed reservation details.
  - Fails with `RESERVATION_NOT_FOUND`, `RESERVATION_TIME_NOT_FOUND`, `PAST_RESERVATION`, or `DUPLICATE_RESERVATION`.
- `DELETE /reservations/{reservationId}`
  - Authenticated.
  - Response: `204 No Content`.
  - Fails with `RESERVATION_NOT_FOUND` or `PAST_RESERVATION`.
- `DELETE /admin/times/{timeId}`
  - Response: `204 No Content`.
  - Fails with `RESERVATION_TIME_NOT_FOUND` or `RESERVATION_TIME_IN_USE`.

### Error Response

```json
{
  "code": "ERROR_CODE",
  "message": "사용자가 이해할 수 있는 메시지"
}
```

## Cycle 2 Waiting Approval

### Feature List

- Automatic approval is used: when a reservation slot is vacated, the first waiting entry for that slot becomes a reservation.
- Promotion happens when a reservation is cancelled or changed away from its previous slot.
- Promotion deletes the first waiting entry and creates a reservation in one transaction.
- If promotion fails while creating the reservation, the waiting deletion is rolled back.
- Remaining waiting ranks are recalculated from the existing count query; ranks are not stored.

### Design Decisions

- `WaitingPromotionService` owns reservation-waiting promotion so `ReservationService` does not call `WaitingService` while `WaitingService` writes reservations.
- `MyReservationService` owns the combined reservation/waiting read model.
- `WaitingRankService` owns rank calculation backed by `WaitingRepository.countEarlierWaitings(...)`.
- `ReservationTimeService.findThemeTimes(...)` loads reservations for a theme/date once and maps reserved time ids, instead of issuing one exists query per time.

## Test Convention Decisions

- Domain tests construct objects without Spring and cover constructor failures for single-object invariants.
- Domain invariant validation stays inside each domain class as private validation methods; no shared `DomainValidator` utility is used.
- Repository tests use `@DataJpaTest`.
- Controller slice tests use `@WebMvcTest`.
- Full HTTP tests are named `*HttpIntegrationTest`.
- Acceptance tests cover user-visible flows.
- Service unit tests use mocks and fixed `Clock` for core orchestration failures that do not need Spring or DB.
- Service/mock tests and longer acceptance tests use `given`, `when`, and `then` comments where setup, action, and assertion would otherwise blur together.

## Manager Authorization Add-On

### Feature List

- A manager can manage only reservations that belong to the manager's store.
- A non-manager member cannot use manager reservation APIs.
- An unauthenticated request returns `UNAUTHORIZED`.
- An authenticated but unauthorized request returns `FORBIDDEN`.

### API Decisions

- `POST /admin/members/managers`
  - Bootstrap API for creating a manager member.
  - Uses the existing `MemberRequest` shape.
- `POST /admin/stores`
  - Bootstrap API for creating a store and assigning one manager.
  - Request: `name`, `managerId`.
- `POST /admin/themes`
  - Keeps the existing fields and optionally accepts `storeId`.
  - Manager authorization applies only to reservations whose theme is connected to a store.
- `GET /manager/reservations`
  - Authenticated manager API.
  - Returns reservations for the manager's store only.
- `PUT /manager/reservations/{reservationId}`
  - Authenticated manager API.
  - Changes reservations for the manager's store only.

## Concurrent Login Add-On

### Policy Decision

New login wins. When the same member logs in again, the server renews that member's active authentication
session with a new unique token and invalidates the previously issued token.

### Design Decisions

- `AuthSession` stores exactly one active token for each member.
- Login locks the target member row before renewing the session, so simultaneous logins for the same account are serialized.
- The active-session invariant is backed by unique database constraints on `member_id` and `access_token`.
- Tokens include a random nonce so repeated logins for the same member produce different bearer tokens.
- `LoginMemberArgumentResolver` remains a web boundary: it extracts the bearer token and delegates token/session validation to `AuthService`.
- `DELETE /manager/reservations/{reservationId}`
  - Authenticated manager API.
  - Cancels reservations for the manager's store only.

### Design Decisions

- `Store` is a separate domain entity with one manager.
- `Store` validates that its manager has `Role.MANAGER`.
- `ManagerAuthorizationService` owns manager role and managed-store authorization.
- `ReservationService` keeps the reservation change/cancel workflow and adds manager entry points that reuse the same schedule change and cancellation helpers.
- `ReservationController` remains resource-oriented; role-prefixed manager paths are grouped with reservation endpoints instead of introducing a role-based controller.
- Manager authorization loads the reservation first and returns `FORBIDDEN` when the reservation exists but belongs to another store.

### Test Decisions

- `StoreTest` covers store construction invariants.
- `ManagerAuthorizationServiceTest` covers manager role and managed-store authorization.
- `ReservationRepositoryIntegrationTest` covers store-based reservation lookup.
- `ManagerReservationAcceptanceTest` covers manager read, change, cancel, forbidden, and unauthenticated flows.
