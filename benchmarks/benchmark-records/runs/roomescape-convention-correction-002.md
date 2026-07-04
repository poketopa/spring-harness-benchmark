# roomescape-convention-correction-002

## Summary

- Date: 2026-07-03
- Mission: roomescape-reservation-waiting
- Cycle: convention correction after AI audit
- Requirement: user feedback after AI convention audit
- Baseline: benchmarks/roomescape-jpa-auth-cycle2-regen
- Target project: benchmarks/roomescape-jpa-auth-cycle2-regen
- Skill: spring-usecase-implementation
- Started at: not captured
- Finished at: 2026-07-03T12:04:56+09:00
- Duration minutes:
- Result status: pass

## Prompt

```text
AI를 통해 일치하지 않는 컨벤션을 조사했다. 각 요소가 이전에 사용자가 선택해 무너뜨린 부분인지, 아니면 Codex가 임의로 만든 어긋난 컨벤션인지 확인하고 바로잡자.
```

## Ownership Classification

| Item | Classification | Decision |
| --- | --- | --- |
| Admin authorization service vs manager private helper | Codex-created mismatch | Added `ManagerAuthorizationService` and reused `AuthenticatedMemberService`. |
| Reservation cancellation/change calling waiting approval and `WaitingService` creating reservations | Codex-created mismatch | Moved first-waiting promotion into `WaitingPromotionService`. |
| Repeated `LoginMember -> Member` lookup | Codex-created mismatch | Added `AuthenticatedMemberService`. |
| Repeated theme/time lookup exception conversion | Codex-created drift risk | Added `getByIdOrThrow` helpers to theme/time repositories where not-found meaning is stable. |
| Reservation time list using per-row `exists` | Codex-created inefficient implementation | Changed to one `findAllByThemeAndDate` read plus reserved time id mapping. |
| Role-based manager controller mixed with resource-based admin controllers | Codex-created mismatch | Moved manager reservation endpoints into `ReservationController`. |
| `MemberService` create-helper-createManager ordering | User-selected convention | No code change; this follows public flow followed by first-called private helper. |

## Generated Result

### Main Files

- Controller: `ReservationController`
- Service: `AuthenticatedMemberService`, `AdminAuthorizationService`, `ManagerAuthorizationService`, `ReservationService`, `WaitingService`, `WaitingPromotionService`, `MyReservationService`, `ReservationTimeService`
- Repository: `ThemeRepository`, `ReservationTimeRepository`, `ReservationRepository`
- Tests: `WaitingApprovalTransactionIntegrationTest`, `ReservationManagerAuthorizationControllerIntegrationTest`
- Docs: `benchmarks/roomescape-jpa-auth-cycle2-regen/README.md`
- Skill references: `controller-service-repository.md`, `review-rubric.md`

### Verification

- Narrow test: `./gradlew test --tests 'roomescape.ReservationManagerAuthorizationControllerIntegrationTest' --tests 'roomescape.ManagerReservationServiceIntegrationTest' --tests 'roomescape.WaitingApprovalTransactionIntegrationTest' --tests 'roomescape.ReservationTimeControllerIntegrationTest' --tests 'roomescape.ReservationAcceptanceTest' --tests 'roomescape.WaitingAcceptanceTest'`
- Full verification: `./gradlew clean test`
- Result: pass

## Convention Comparison

| Category | Expected | Observed | Status | Severity | Evidence |
| --- | --- | --- | --- | --- | --- |
| authorization_boundary | Admin and manager authorization use the same boundary style | `AdminAuthorizationService` and `ManagerAuthorizationService` both use `AuthenticatedMemberService` | pass | P1 | `src/main/java/roomescape/service` |
| promotion_orchestration | Reservation-waiting promotion has one usecase owner | `WaitingPromotionService` owns first waiting promotion and reservation creation | pass | P1 | `WaitingPromotionService` |
| lookup_policy | Repeated authenticated member and reference lookup should not drift | `AuthenticatedMemberService` centralizes authenticated member loading; theme/time stable not-found helpers are repository defaults | pass | P2 | `AuthenticatedMemberService`, repositories |
| reservation_time_query | List mapping avoids per-row exists checks when a bulk query exists | `ReservationTimeService` loads reservations for theme/date once and maps reserved time ids | pass | P2 | `ReservationTimeService` |
| controller_grouping | Controllers follow one resource-oriented grouping rule | Manager reservation endpoints moved into `ReservationController` and `ManagerReservationController` was removed | pass | P2 | `ReservationController` |
| java_class_ordering | User's helper placement convention remains intact | `MemberService` was not changed because helper placement follows the selected convention | pass | P3 | `MemberService` |
| verification | Narrow and full verification pass | Targeted tests and `./gradlew clean test` passed | pass | P1 | command output |

## Skill Changes

Record only changes caused by this run.

- Skill/reference file: `.agents/skills/spring-usecase-implementation/references/controller-service-repository.md`
- Category: authorization_boundary, promotion_orchestration, controller_grouping, repository_query_readability
- Change: added rules for consistent role authorization services, authenticated member lookup, waiting promotion ownership, controller grouping, compatibility alias policy, and bulk reads over per-row exists mapping
- Skill/reference file: `.agents/skills/spring-usecase-implementation/references/review-rubric.md`
- Category: review_rubric
- Change: added final review checks for the same convention failures
- Validation target: next regeneration benchmark

## Follow-Up

- Keep: `WaitingService` separate from `ReservationService`; `WaitingPromotionService` owns conversion.
- Keep: public method followed by first-called private helper ordering.
- Watch next run: confirm the skill now generates consistent authorization boundaries, resource controller grouping, no compatibility aliases by default, and bulk list mapping.
