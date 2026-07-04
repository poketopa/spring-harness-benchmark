# roomescape-repeat-manager-authz-007

## Summary

- Date: 2026-07-03
- Mission: roomescape-reservation-waiting
- Cycle: manager-authz-repeat
- Requirement: missions/roomescape-reservation-waiting/requirements/add-ons/auth-authorization/03-store-manager-authorization.md
- Baseline: benchmarks/roomescape-jpa-auth-cycle2-regen-v2
- Target project: benchmarks/repeat-runs/roomescape-manager-authz-007
- Skill: spring-usecase-implementation
- Started at: 2026-07-03T21:04:40+09:00
- Finished at: 2026-07-03T21:09:36+09:00
- Duration minutes: 4.9
- Result status: pass

## Prompt

```text
manager-authz-repeat run 007.
Use baseline benchmarks/roomescape-jpa-auth-cycle2-regen-v2.
Target benchmarks/repeat-runs/roomescape-manager-authz-007.
Requirement missions/roomescape-reservation-waiting/requirements/add-ons/auth-authorization/03-store-manager-authorization.md.
Use spring-usecase-implementation skill and current benchmark records only.
```

## Expected Decision Rules

- [x] manager_authorization_boundary
- [x] auth_vs_authz_errors
- [x] controller_grouping
- [x] store_domain_invariant
- [x] reservation_manager_scope
- [x] test_layering
- [x] verification

## Generated Result

### Main Files

- Domain: `Store`, `Theme`, `Reservation`
- Controller: `ReservationController`
- Service: `ManagerAuthorizationService`, `ReservationService`
- Repository: `StoreRepository`, `ReservationRepository`
- DTO: existing reservation/theme/member DTOs
- Tests: `ManagerReservationAcceptanceTest`, `StoreTest`, `ManagerAuthorizationServiceTest`, `ReservationServiceTest`
- Docs: `README.md`

### Verification

- Narrow test: targeted manager authorization tests
- Full verification: `./gradlew clean test`
- Result: pass

## Convention Comparison

| Category | Expected | Observed | Status | Severity | Evidence |
| --- | --- | --- | --- | --- | --- |
| manager_authorization_boundary | Manager role and managed-store checks use a consistent service boundary. | ManagerAuthorizationService owns role and store checks. | pass | P1 | benchmarks/repeat-runs/roomescape-manager-authz-007/src/main/java/roomescape/service/ManagerAuthorizationService.java |
| auth_vs_authz_errors | Authentication and authorization failures are distinct. | UNAUTHORIZED remains 401 and FORBIDDEN maps to 403. | pass | P1 | benchmarks/repeat-runs/roomescape-manager-authz-007/src/main/java/roomescape/exception |
| controller_grouping | Role-prefixed reservation paths stay in ReservationController. | Manager reservation endpoints are grouped in ReservationController. | pass | P1 | benchmarks/repeat-runs/roomescape-manager-authz-007/src/main/java/roomescape/controller/ReservationController.java |
| store_domain_invariant | Store requires a manager with Role.MANAGER. | Store constructor validates manager role. | pass | P1 | benchmarks/repeat-runs/roomescape-manager-authz-007/src/main/java/roomescape/domain/Store.java |
| reservation_manager_scope | Manager can access only own-store reservations. | ReservationService delegates manager scope checks to ManagerAuthorizationService. | pass | P1 | benchmarks/repeat-runs/roomescape-manager-authz-007/src/main/java/roomescape/service/ReservationService.java |
| test_layering | Domain, service, and acceptance tests cover manager authorization. | StoreTest, ManagerAuthorizationServiceTest, ManagerReservationAcceptanceTest, and ReservationServiceTest cover the feature. | pass | P1 | benchmarks/repeat-runs/roomescape-manager-authz-007/src/test/java/roomescape |
| verification | Narrow test and full clean test pass. | Targeted manager-authz tests and `./gradlew clean test` passed. | pass | P1 | benchmarks/benchmark-records/runs/roomescape-repeat-manager-authz-007.md |

## Skill Changes

- If none: no skill/reference changes.

## Follow-Up

- Keep: ManagerAuthorizationService, Store invariant, resource-based controller grouping.
- Fix before next run: none.
- Add to skill/reference: none.
- Next benchmark: concurrent-login-repeat.
- Repeated benchmark decision: clean pass.
