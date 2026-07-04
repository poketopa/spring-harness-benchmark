# roomescape-convention-correction-003

## Summary

- Date: 2026-07-03
- Mission: roomescape-reservation-waiting
- Cycle: convention correction after DTO/auth/validation audit
- Requirement: user feedback after DTO-auth-validation audit
- Baseline: benchmarks/roomescape-jpa-auth-cycle2-regen
- Target project: benchmarks/roomescape-jpa-auth-cycle2-regen
- Skill: spring-usecase-implementation
- Started at: not captured
- Finished at: 2026-07-03T12:31:58+09:00
- Duration minutes:
- Result status: pass

## Prompt

```text
DTO 정적 팩토리 네이밍, 인증 처리 책임, 검증 메서드 분리 기준이 내가 선택한 부분인지 확인하고 아니라면 반영해줘.
```

## Ownership Classification

| Item | Classification | Decision |
| --- | --- | --- |
| `MyReservationResponse.fromWaiting(waiting, rank)` while other extra-value factories use `of` | Codex-created mismatch | Renamed to `ofWaiting(waiting, rank)` and kept `fromReservation(reservation)` for one-source conversion. |
| `LoginMemberArgumentResolver` accessing repositories and validating token/session/member | Pre-existing plus Codex-leftover mismatch | Resolver now extracts bearer token only and delegates validation to `AuthService.authenticate`. |
| `ReservationService.create` inline validation while sibling flows use named validation helpers | Codex-created style drift | Added `validateCreateAllowed` and reused `saveReservation` for create/change persistence. |

## Generated Result

### Main Files

- DTO: `MyReservationResponse`
- Auth: `LoginMemberArgumentResolver`
- Service: `AuthService`, `ReservationService`, `MyReservationService`
- Tests: `AuthServiceIntegrationTest`
- Docs: `benchmarks/roomescape-jpa-auth-cycle2-regen/README.md`
- Skill references: `controller-service-repository.md`, `review-rubric.md`

### Verification

- Narrow test: `./gradlew test --tests 'roomescape.AuthServiceIntegrationTest' --tests 'roomescape.ConcurrentLoginControllerIntegrationTest' --tests 'roomescape.ConcurrentLoginAcceptanceTest' --tests 'roomescape.ReservationServiceIntegrationTest' --tests 'roomescape.ReservationAcceptanceTest' --tests 'roomescape.WaitingAcceptanceTest'`
- Full verification: `./gradlew clean test`
- Result: pass

## Convention Comparison

| Category | Expected | Observed | Status | Severity | Evidence |
| --- | --- | --- | --- | --- | --- |
| dto_factory_naming | `from` for one-source conversion and `of` for extra calculated values | `MyReservationResponse.ofWaiting(waiting, rank)` now matches `WaitingResponse.of` and `ReservationTimeResponse.of` | pass | P2 | `MyReservationResponse` |
| authentication_boundary | Resolver extracts web input and auth service owns authentication policy | `LoginMemberArgumentResolver` delegates to `AuthService.authenticate` | pass | P1 | `LoginMemberArgumentResolver`, `AuthService` |
| validation_helper_consistency | Sibling use cases in one service use the same validation helper style | `ReservationService.create` uses `validateCreateAllowed`, matching change/waiting validation helper style | pass | P2 | `ReservationService` |
| test_layering | Auth boundary has focused service tests and existing web tests remain | `AuthServiceIntegrationTest` covers current token authentication and stale token rejection | pass | P1 | `AuthServiceIntegrationTest` |
| verification | Narrow and full verification pass | Targeted tests and `./gradlew clean test` passed | pass | P1 | command output |

## Skill Changes

Record only changes caused by this run.

- Skill/reference file: `.agents/skills/spring-usecase-implementation/references/controller-service-repository.md`
- Category: dto_factory_naming, authentication_boundary, validation_helper_consistency
- Change: added rules for DTO static factory names, resolver-to-auth-service delegation, and sibling validation helper consistency
- Skill/reference file: `.agents/skills/spring-usecase-implementation/references/review-rubric.md`
- Category: review_rubric
- Change: added final checks for DTO factory naming, web auth responsibility, and validation helper consistency
- Validation target: next regeneration benchmark

## Follow-Up

- Keep: `from...` for single-source DTO conversion; `of...` for extra calculated values.
- Keep: argument resolver only extracts web input; `AuthService` owns token/session/member validation.
- Watch next run: verify the skill applies these rules without manual correction.
