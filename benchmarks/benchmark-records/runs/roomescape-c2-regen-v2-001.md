# roomescape-c2-regen-v2-001

## Summary

- Date: 2026-07-03
- Mission: roomescape-reservation-waiting
- Cycle: cycle2-regen-v2
- Requirement:
  - missions/roomescape-reservation-waiting/requirements/base/03-cycle2-reservation-change-cancel-error.md
  - missions/roomescape-reservation-waiting/requirements/base/04-cycle2-waiting-approval.md
- Baseline: benchmarks/roomescape-jpa-auth-cycle1-regen-v2
- Target project: benchmarks/roomescape-jpa-auth-cycle2-regen-v2
- Skill: spring-usecase-implementation
- Started at: not captured
- Finished at: 2026-07-03T15:18:24+09:00
- Duration minutes: not captured
- Result status: pass

## Prompt

```text
좋아 재생성하자.
```

## Expected Decision Rules

- [x] service_boundary: reservation use cases stay in `ReservationService`, waiting use cases stay in `WaitingService`, promotion orchestration has one owner.
- [x] read_model_boundary: `MyReservationService` owns the combined reservation/waiting read model.
- [x] uri_convention: waiting is a peer resource under `/waitings`.
- [x] dto_factory_naming: one-source conversions use `from`, calculated-value conversions use `of`.
- [x] authentication_boundary: resolver extracts web input and delegates authentication.
- [x] validation_helper_consistency: sibling reservation validations use named helper style.
- [x] domain_invariants: domain constructors reject single-object invalid state.
- [x] repository_query_readability: rank and reserved time mapping use direct query/bulk read patterns.
- [x] test_layering: domain, service unit/integration, repository slice, controller slice, HTTP integration, and acceptance tests are separated.
- [x] verification: targeted tests run before full verification.

## Generated Result

### Main Files

- Domain: `DomainValidator`, `Member`, `Theme`, `ReservationTime`, `Reservation`, `Waiting`
- Controller: `ReservationController`, `WaitingController`, `ReservationTimeController`
- Service: `AuthenticatedMemberService`, `ReservationService`, `WaitingService`, `MyReservationService`, `WaitingRankService`, `WaitingPromotionService`, `ReservationTimeService`
- Repository: `ThemeRepository`, `ReservationTimeRepository`, `ReservationRepository`, `WaitingRepository`
- DTO: `ReservationChangeRequest`, `MyReservationResponse`
- Tests:
  - `roomescape.domain.*Test`
  - `roomescape.service.*Test`
  - `roomescape.repository.*Test`
  - `roomescape.controller.*Test`
  - `*HttpIntegrationTest`
  - `*AcceptanceTest`
- Docs: `benchmarks/roomescape-jpa-auth-cycle2-regen-v2/README.md`

### Verification

- Narrow test: `./gradlew test --tests 'roomescape.domain.*Test' --tests 'roomescape.service.*Test' --tests 'roomescape.repository.*Test' --tests 'roomescape.controller.*Test'`
- Narrow result: pass
- Full verification: `./gradlew clean test`
- Result: pass

## Convention Comparison

| Category | Expected | Observed | Status | Severity | Evidence |
| --- | --- | --- | --- | --- | --- |
| service_boundary | Reservation/Waiting use cases are separate and promotion has one owner | `ReservationService`, `WaitingService`, and `WaitingPromotionService` have separate responsibilities | pass | P1 | `src/main/java/roomescape/service` |
| read_model_boundary | Combined my-reservations read model is owned by `MyReservationService` | `MyReservationService` queries repositories directly and maps both states | pass | P1 | `src/main/java/roomescape/service/MyReservationService.java` |
| uri_convention | Waiting uses peer `/waitings` path | `WaitingController` exposes `POST /waitings`, `DELETE /waitings/{waitingId}` only | pass | P1 | `src/main/java/roomescape/controller/WaitingController.java` |
| dto_factory_naming | Additional calculated values use `of` | `MyReservationResponse.ofWaiting(waiting, rank)` is used | pass | P2 | `src/main/java/roomescape/dto/MyReservationResponse.java` |
| authentication_boundary | Resolver delegates token/member validation to auth service | `LoginMemberArgumentResolver` extracts bearer token and calls `AuthService.authenticate` | pass | P1 | `src/main/java/roomescape/auth/LoginMemberArgumentResolver.java` |
| validation_helper_consistency | Create/change reservation validations use named helper style | `validateCreateAllowed` and `validateChangeAllowed` are both present | pass | P2 | `src/main/java/roomescape/service/ReservationService.java` |
| domain_invariants | Domain constructors reject single-object invalid state | Member/theme/time/reservation/waiting constructors validate null/blank fields | pass | P1 | `src/main/java/roomescape/domain` |
| repository_query_readability | Rank uses direct count and list mapping avoids per-row exists | `WaitingRepository.countEarlierWaitings` and `findAllByThemeAndDate` reserved id mapping are used | pass | P1 | `src/main/java/roomescape/repository`, `ReservationTimeService` |
| test_layering | Tests are separated by layer and feature | Domain unit, service mock/integration, DataJpaTest, WebMvcTest, HTTP integration, acceptance tests exist | pass | P1 | `src/test/java/roomescape` |
| full_http_naming | Full HTTP tests are not named controller integration tests | Full HTTP tests use `*HttpIntegrationTest` and slice tests use `@WebMvcTest` | pass | P1 | `src/test/java/roomescape` |
| verification | Targeted then full verification passes | Both targeted layered tests and `./gradlew clean test` passed | pass | P1 | this run note |

## Skill Changes

No new skill change was made in this run. Existing pending updates validated by this run should be marked with `roomescape-c2-regen-v2-001` where the cycle2 base scope exercised them.

## Follow-Up

- Keep: current cycle2-regen-v2 as the regenerated benchmark result for base cycle2.
- Still pending: manager authorization specific skill update needs a manager-auth regeneration/add-on run because this base cycle2 scope has no manager/store role model.
- Next benchmark: regenerate the auth/manager add-on or run a compact convention audit on `cycle2-regen-v2`.
