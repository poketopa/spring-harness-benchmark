# roomescape-convention-correction-001

## Summary

- Date: 2026-07-03
- Mission: roomescape-reservation-waiting
- Cycle: convention correction after JPA add-on
- Requirement: user feedback after JPA add-on
- Baseline: benchmarks/roomescape-jpa-auth-cycle2-regen
- Target project: benchmarks/roomescape-jpa-auth-cycle2-regen
- Skill: spring-usecase-implementation
- Started at: 2026-07-03T11:33:40+09:00
- Finished at: 2026-07-03T11:42:32+09:00
- Duration minutes: 8.9
- Result status: pass

## Prompt

```text
1. /admin and /manager authorization conventions differ. Admin APIs without LoginMember are effectively public.
2. MyReservationService responsibility is only half separated because ReservationService and WaitingService still create MyReservationResponse.
3. Service method ordering is inconsistent.
4. waiting and reservation endpoints must be separated because waiting is a peer concept.
5. Compatibility and standard paths are mixed.
```

## Expected Decision Rules

- [x] admin_authorization_consistency: `/admin/**` APIs require `LoginMember` and service-level admin validation.
- [x] admin_bootstrap_surface: admin account bootstrap is not exposed as a public application endpoint.
- [x] read_model_boundary: `MyReservationService` owns the combined my-reservations read model.
- [x] uri_convention: waiting is exposed under `/waitings`, not `/reservations/waitings`.
- [x] compatibility_alias_policy: compatibility aliases are not exposed unless explicitly required and documented.
- [x] java_class_ordering: public flow is followed by first-called private helpers.
- [x] test_layering: convention corrections have focused regression tests.
- [x] documentation: README records bootstrap admin and canonical paths.
- [x] verification: targeted tests and full Gradle test pass.

## Generated Result

### Main Files

- Domain: `Member`
- Controller: `MemberController`, `ThemeController`, `ReservationTimeController`, `StoreController`, `ReservationController`, `WaitingController`
- Service: `AdminAuthorizationService`, `MemberService`, `ThemeService`, `ReservationTimeService`, `StoreService`, `MyReservationService`, `WaitingService`, `WaitingRankService`
- Repository: `ReservationRepository`, `WaitingRepository`
- DTO: none
- Tests: `AdminAuthorizationControllerIntegrationTest`, `AcceptanceTestSupport`, `JpaAddOnAcceptanceTest`, `WaitingControllerIntegrationTest`, `ReservationTimeControllerIntegrationTest`
- Docs: `benchmarks/roomescape-jpa-auth-cycle2-regen/README.md`

### Verification

- Narrow test: `./gradlew test --tests 'roomescape.AdminAuthorizationControllerIntegrationTest' --tests 'roomescape.JpaAddOnAcceptanceTest' --tests 'roomescape.WaitingControllerIntegrationTest' --tests 'roomescape.ReservationTimeControllerIntegrationTest' --tests 'roomescape.ManagerAuthorizationAcceptanceTest'`
- Full verification: `./gradlew clean test`
- Result: pass

## Convention Comparison

| Category | Expected | Observed | Status | Severity | Evidence |
| --- | --- | --- | --- | --- | --- |
| admin_authorization_consistency | All admin APIs require `LoginMember` and service-level admin validation | `AdminAuthorizationService` validates `ADMIN` for admin managers, stores, themes, times, and waitings while admin accounts are seeded outside public API | pass | P1 | `AdminAuthorizationService` |
| admin_bootstrap_surface | Admin bootstrap is not a public application API | Tests seed an admin fixture directly and the application exposes no public admin creation endpoint | pass | P1 | `AcceptanceTestSupport` |
| read_model_boundary | `MyReservationService` owns the combined my-reservations read model | `MyReservationService` now queries reservation and waiting repositories and builds `MyReservationResponse` | pass | P1 | `MyReservationService` |
| uri_convention | Waiting is a peer resource exposed under `/waitings` | `WaitingController` exposes `POST /waitings` and `DELETE /waitings/{waitingId}` only | pass | P1 | `WaitingController` |
| compatibility_alias_policy | Do not expose compatibility aliases unless explicitly required and documented | Removed `/reservations-mine` and nested waiting aliases | pass | P1 | `ReservationController`, `WaitingController` |
| java_class_ordering | Public method flow is followed by first-called private helpers | `ReservationTimeService` and touched services keep helpers directly below first public caller | pass | P2 | `ReservationTimeService` |
| test_layering | Convention fixes have focused integration and acceptance regression coverage | Added admin authorization controller integration tests and updated canonical path/admin waiting tests | pass | P1 | `src/test/java/roomescape` |
| documentation | README records admin provisioning and canonical paths | README documents admin-only `/admin/**`, no public admin bootstrap, and `/waitings` canonical path | pass | P2 | `README.md` |
| verification | Narrow and full verification pass | Targeted tests and `./gradlew clean test` passed | pass | P1 | command output |

## Skill Changes

Record only changes caused by this run.

- Skill/reference file: none
- Category: none
- Change: none
- Reason: this run corrected the benchmark output only
- Validation target: next project decision

## Follow-Up

- Keep: admin APIs require `ADMIN`, no public admin creation endpoint is exposed, waiting uses `/waitings`, `MyReservationService` owns the combined read model.
- Fix before next run: none in the target project.
- Add to skill/reference: consider adding explicit rules for admin API auth consistency, read-model service ownership, no compatibility alias by default, and waiting peer URI naming.
- Next benchmark: update skill/reference with those rules before generating the next requirement set.
