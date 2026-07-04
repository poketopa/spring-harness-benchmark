# roomescape-authz-manager-001

## Summary

- Date: 2026-07-03
- Mission: roomescape-reservation-waiting
- Cycle: auth-authorization add-on
- Requirement: missions/roomescape-reservation-waiting/requirements/add-ons/auth-authorization/03-store-manager-authorization.md
- Baseline: benchmarks/roomescape-jpa-auth-cycle2-regen
- Target project: benchmarks/roomescape-jpa-auth-cycle2-regen
- Skill: spring-usecase-implementation
- Started at: 2026-07-03T10:45:01+09:00
- Finished at: 2026-07-03T10:51:35+09:00
- Duration minutes: 6.6
- Result status: pass

## Prompt

```text
좋아. 시작하자.
```

Context: continue after the base mission. `01-web-login.md` and `02-mobile-auth.md` were treated as an audit because the latest project already has bearer-token login and `LoginMemberArgumentResolver`. Implement the next meaningful benchmark requirement, `03-store-manager-authorization.md`.

## Expected Decision Rules

- [x] service_boundary: Manager reservation workflows stay in reservation application flow; store bootstrap stays in StoreService.
- [x] domain_naming: Store is modeled as `Store`, not merged into `Theme`.
- [x] authorization_boundary: Controller does not decide authz; Service coordinates lookup and domain methods answer ownership.
- [x] test_layering: Domain, repository, service, controller, and acceptance tests cover the new authorization behavior.
- [x] java_class_ordering: Public flows are followed by first-called helpers and domain behavior stays above getters.
- [x] api_contract: Authentication and authorization failures are separated as `UNAUTHORIZED` and `FORBIDDEN`.
- [x] exception_style: Existing ErrorResponse/ErrorCode style is reused.
- [x] transaction_boundary: Reservation update/delete flows keep existing transactional behavior.
- [x] repository_query_readability: Store-scoped reservation lookup remains readable.
- [x] documentation: README records audit, feature list, API decisions, and authz decisions.
- [x] verification: Targeted tests and full Gradle test pass.

## Generated Result

### Main Files

- Domain: `Store`, `Member.isManager()`, `Theme.isManagedBy(...)`, `Reservation.isManagedBy(...)`
- Controller: `StoreController`, `ManagerReservationController`, manager creation in `MemberController`
- Service: `StoreService`, manager reservation methods in `ReservationService`, store-aware theme creation in `ThemeService`
- Repository: `StoreRepository`, store-scoped reservation query in `ReservationRepository`
- DTO: `ManagerRequest`, `StoreRequest`, `StoreResponse`, optional `ThemeRequest.storeId`
- Tests: `StoreTest`, `ThemeTest`, `StoreRepositoryIntegrationTest`, `ManagerReservationServiceIntegrationTest`, `ManagerReservationControllerIntegrationTest`, `ManagerAuthorizationAcceptanceTest`, repository extension test
- Docs: `benchmarks/roomescape-jpa-auth-cycle2-regen/README.md`

### Verification

- Narrow test: `./gradlew test --tests 'roomescape.domain.StoreTest' --tests 'roomescape.domain.ThemeTest' --tests 'roomescape.StoreRepositoryIntegrationTest' --tests 'roomescape.ReservationRepositoryIntegrationTest' --tests 'roomescape.ManagerReservationServiceIntegrationTest' --tests 'roomescape.ManagerReservationControllerIntegrationTest' --tests 'roomescape.ManagerAuthorizationAcceptanceTest'`
- Full verification: `./gradlew clean test`
- Result: pass

## Convention Comparison

| Category | Expected | Observed | Status | Severity | Evidence |
| --- | --- | --- | --- | --- | --- |
| service_boundary | Store bootstrap and manager reservation management have clear service homes | StoreService owns stores; ReservationService owns reservation management flows | pass | P1 | `StoreService`, `ReservationService` |
| domain_naming | Store is represented with the domain noun | New entity is named `Store` | pass | P1 | `Store` |
| authorization_boundary | Controller does not decide manager authorization | ManagerReservationController delegates; ReservationService loads member/reservation; domain methods answer managed ownership | pass | P1 | `ManagerReservationController`, `ReservationService`, domain classes |
| test_layering | Core authz behavior has layered tests | Domain, repository, service, controller, and acceptance tests were added | pass | P1 | `src/test/java/roomescape` |
| java_class_ordering | Public method flow is followed by helpers and domain behavior appears before getters | New services and domains follow the ordering convention; Member behavior was moved above getters | pass | P2 | `Member`, `Store`, `ReservationService` |
| api_contract | Authn/authz failures are separate and existing APIs stay compatible | Missing token returns `UNAUTHORIZED`; non-manager/cross-store access returns `FORBIDDEN`; existing theme request remains compatible with missing `storeId` | pass | P1 | controller tests, acceptance tests |
| exception_style | ErrorResponse/ErrorCode style remains consistent | Added `FORBIDDEN`, `STORE_NOT_FOUND`, `MEMBER_NOT_FOUND`, `MANAGER_ALREADY_HAS_STORE` to central mapper | pass | P1 | `ErrorCodeStatusMapper` |
| transaction_boundary | Reservation mutation uses existing transaction behavior | Manager change/delete call the same transactional reservation mutation flow | pass | P1 | `ReservationService` |
| repository_query_readability | Store-scoped reservation lookup is clear | `findAllByThemeStoreOrderByDateAscTimeStartAtAsc` remains readable | pass | P2 | `ReservationRepository` |
| documentation | README documents audit and decisions | README includes auth/mobile audit and manager authorization API/domain decisions | pass | P2 | `README.md` |
| verification | Narrow and full verification pass | Targeted tests and `./gradlew clean test` passed | pass | P1 | command output |

## Skill Changes

Record only changes caused by this run.

- Skill/reference file: none
- Category: none
- Change: none
- Reason: no repeated convention failure was observed
- Validation target: next auth add-on requirement

## Follow-Up

- Keep: Store as separate domain, explicit `FORBIDDEN`, manager reservation endpoints separated under `/manager/reservations`.
- Fix before next run: none required.
- Add to skill/reference: none for now.
- Next benchmark: `04-prevent-concurrent-login.md` if continuing auth add-ons.
