# roomescape-authz-manager-v2-001

## Summary

- Date: 2026-07-03
- Mission: roomescape-reservation-waiting
- Cycle: auth-authorization-regen-v2
- Requirement: missions/roomescape-reservation-waiting/requirements/add-ons/auth-authorization/03-store-manager-authorization.md
- Baseline: benchmarks/roomescape-jpa-auth-cycle2-regen-v2
- Target project: benchmarks/roomescape-jpa-auth-manager-authorization-regen-v2
- Skill: spring-usecase-implementation
- Started at: not captured
- Finished at: 2026-07-03T16:56:57+09:00
- Duration minutes: not captured
- Result status: pass

## Prompt

```text
Manager authorization add-on regeneration from cycle2-regen-v2 after convention corrections.
Implement store manager authorization using the current skill and owner conventions.
```

## Expected Decision Rules

- [x] manager_authorization_boundary: manager role and managed-store checks use a dedicated authorization service.
- [x] auth_vs_authz_errors: unauthenticated requests return `UNAUTHORIZED` and unauthorized authenticated requests return `FORBIDDEN`.
- [x] controller_grouping: role-prefixed reservation endpoints stay in the resource controller.
- [x] store_domain_invariant: `Store` owns the invariant that its manager must have `Role.MANAGER`.
- [x] reservation_manager_scope: managers can manage only reservations in their store.
- [x] test_layering: domain, service, repository, and acceptance coverage is present for the new behavior.
- [x] convention_correction_validation: no generic `DomainValidator` is introduced and complex tests use clear given/when/then boundaries.

## Generated Result

### Main Files

- Domain:
  - Added `Store`.
  - Added manager comparison behavior to `Member`.
  - Linked `Theme` to an optional `Store`.
  - Added reservation-store ownership behavior through `Reservation.belongsTo(...)`.
- API:
  - Added `POST /admin/members/managers`.
  - Added `POST /admin/stores` and `GET /admin/stores`.
  - Added `GET /manager/reservations`.
  - Added `PUT /manager/reservations/{reservationId}`.
  - Added `DELETE /manager/reservations/{reservationId}`.
- Service/repository:
  - Added `ManagerAuthorizationService`.
  - Added `StoreService` and `StoreRepository`.
  - Extended `ReservationService` with manager read/change/cancel entry points.
  - Added store-based reservation lookup in `ReservationRepository`.
- Tests:
  - Added `StoreTest`.
  - Added `ManagerAuthorizationServiceTest`.
  - Added store-based repository query coverage.
  - Added `ManagerReservationAcceptanceTest`.

## Verification

- Narrow test: `./gradlew test --tests 'roomescape.domain.*Test' --tests 'roomescape.service.*Test' --tests 'roomescape.repository.ReservationRepositoryIntegrationTest' --tests 'roomescape.ManagerReservationAcceptanceTest'`
- Narrow result: pass
- Full verification: `./gradlew clean test`
- Result: pass

## Convention Comparison

| Category | Expected | Observed | Status | Severity | Evidence |
| --- | --- | --- | --- | --- | --- |
| manager_authorization_boundary | Manager authz is a dedicated service | `ManagerAuthorizationService` owns manager role and managed-store checks | pass | P1 | `src/main/java/roomescape/service/ManagerAuthorizationService.java` |
| auth_vs_authz_errors | Authentication and authorization failures are distinct | Missing auth returns `UNAUTHORIZED`; non-manager and other-store access return `FORBIDDEN` | pass | P1 | `src/test/java/roomescape/ManagerReservationAcceptanceTest.java` |
| controller_grouping | Resource-oriented controller grouping is preserved | `/manager/reservations` endpoints are in `ReservationController` | pass | P2 | `src/main/java/roomescape/controller/ReservationController.java` |
| store_domain_invariant | Store owns manager role invariant | `Store` rejects non-manager members | pass | P1 | `src/main/java/roomescape/domain/Store.java` |
| reservation_manager_scope | Managers manage only their store's reservations | Reservation lookup distinguishes missing reservation from other-store forbidden | pass | P1 | `src/main/java/roomescape/service/ReservationService.java` |
| test_layering | Layered tests cover meaningful behavior | Domain service repository and acceptance tests were added | pass | P1 | `src/test/java/roomescape` |
| convention_correction_validation | No generic domain validator and complex tests have clear phases | No `DomainValidator`; new complex tests use given/when/then comments | pass | P2 | target source and tests |
| verification | Targeted then full verification passes | Both targeted tests and `./gradlew clean test` passed | pass | P1 | this run note |

## Skill Changes

- No new skill/reference changes were needed.
- Validated pending manager authorization boundary rule from `skill-update-006`.
- Validated pending domain-owned validation and test-flow clarity rules from `skill-update-018`, `skill-update-019`, and `skill-update-020`.

## Follow-Up

- Candidate design review item: `Theme.storeId` is optional to preserve the existing admin theme API. If future requirements require every theme to belong to a store, make `storeId` required and update baseline fixtures together.
- Next benchmark candidate: concurrent login add-on regeneration on top of this manager authorization result, or a manual convention review of this manager authorization output before continuing.
