# roomescape-repeat-manager-authz-004

## Summary

- Date: 2026-07-03
- Mission: roomescape-reservation-waiting
- Cycle: manager-authz-repeat
- Requirement: missions/roomescape-reservation-waiting/requirements/add-ons/auth-authorization/03-store-manager-authorization.md
- Baseline: benchmarks/roomescape-jpa-auth-cycle2-regen-v2
- Target project: benchmarks/repeat-runs/roomescape-manager-authz-004
- Skill: spring-usecase-implementation
- Started at: 2026-07-03T20:16:18+09:00
- Finished at: 2026-07-03T20:16:49+09:00
- Duration minutes: 0.5
- Result status: pass

## Prompt

```text
manager-authz-repeat run 004 after correction 007.
Use baseline benchmarks/roomescape-jpa-auth-cycle2-regen-v2.
Target benchmarks/repeat-runs/roomescape-manager-authz-004.
Requirement missions/roomescape-reservation-waiting/requirements/add-ons/auth-authorization/03-store-manager-authorization.md.
Use spring-usecase-implementation skill and current benchmark records only.
```

## Expected Decision Rules

- [x] manager_authorization_boundary: ManagerAuthorizationService owns manager role and store checks.
- [x] auth_vs_authz_errors: UNAUTHORIZED and FORBIDDEN are distinct.
- [x] controller_grouping: /manager/reservations stays in ReservationController.
- [x] store_domain_invariant: Store requires Role.MANAGER manager.
- [x] reservation_manager_scope: manager can manage only own-store reservations.
- [x] test_layering: domain, service, and acceptance tests cover manager authorization.
- [x] verification: narrow tests and full clean test passed.

## Generated Result

### Main Files

- Domain: `Store`, `Theme`, `Reservation`
- Controller: `ReservationController`, `StoreController`
- Service: `ManagerAuthorizationService`, `StoreService`, `ReservationService`, `ThemeService`
- Repository: `StoreRepository`, `ReservationRepository`
- DTO: `StoreRequest`, `StoreResponse`, `MemberRequest`, `ThemeRequest`
- Tests: `StoreTest`, `ManagerAuthorizationServiceIntegrationTest`, `ManagerAuthorizationAcceptanceTest`, `ReservationServiceTest`
- Docs: `README.md`

### Verification

- Narrow test: `./gradlew test --tests roomescape.domain.StoreTest --tests roomescape.domain.ThemeTest --tests roomescape.domain.ReservationTest --tests roomescape.service.ManagerAuthorizationServiceIntegrationTest --tests roomescape.ManagerAuthorizationAcceptanceTest`
- Full verification: `./gradlew clean test`
- Result: pass

## Convention Comparison

| Category | Expected | Observed | Status | Severity | Evidence |
| --- | --- | --- | --- | --- | --- |
| manager_authorization_boundary | Manager role and managed-store checks use a consistent service boundary. | ManagerAuthorizationService owns role and store checks. | pass | P1 | benchmarks/repeat-runs/roomescape-manager-authz-004/src/main/java/roomescape/service/ManagerAuthorizationService.java |
| auth_vs_authz_errors | Authentication and authorization failures are distinct. | UNAUTHORIZED and FORBIDDEN are mapped separately. | pass | P1 | benchmarks/repeat-runs/roomescape-manager-authz-004/src/main/java/roomescape/exception |
| controller_grouping | Role-prefixed reservation paths stay in ReservationController. | /manager/reservations endpoints are grouped in ReservationController. | pass | P1 | benchmarks/repeat-runs/roomescape-manager-authz-004/src/main/java/roomescape/controller/ReservationController.java |
| store_domain_invariant | Store requires a manager with Role.MANAGER. | Store constructor validates manager role. | pass | P1 | benchmarks/repeat-runs/roomescape-manager-authz-004/src/main/java/roomescape/domain/Store.java |
| reservation_manager_scope | Manager can access only own-store reservations. | ReservationService delegates store scope checks to ManagerAuthorizationService. | pass | P1 | benchmarks/repeat-runs/roomescape-manager-authz-004/src/main/java/roomescape/service/ReservationService.java |
| test_layering | Domain, service, and acceptance tests cover manager authorization. | StoreTest, ManagerAuthorizationServiceIntegrationTest, and ManagerAuthorizationAcceptanceTest cover the feature. | pass | P1 | benchmarks/repeat-runs/roomescape-manager-authz-004/src/test/java/roomescape |
| verification | Narrow test and full clean test pass. | Targeted manager-authz tests and ./gradlew clean test passed. | pass | P1 | benchmarks/benchmark-records/runs/roomescape-repeat-manager-authz-004.md |

## Skill Changes

- If none: no skill/reference changes.

## Follow-Up

- Keep: ManagerAuthorizationService, Store invariant, resource-based controller grouping.
- Fix before next run: none.
- Add to skill/reference: none.
- Next benchmark: concurrent-login-repeat.
- Repeated benchmark decision: clean pass.
