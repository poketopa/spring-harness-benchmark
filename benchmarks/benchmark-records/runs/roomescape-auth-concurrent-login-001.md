# roomescape-auth-concurrent-login-001

## Summary

- Date: 2026-07-03
- Mission: roomescape-reservation-waiting
- Cycle: auth-authorization add-on
- Requirement: missions/roomescape-reservation-waiting/requirements/add-ons/auth-authorization/04-prevent-concurrent-login.md
- Baseline: benchmarks/roomescape-jpa-auth-cycle2-regen
- Target project: benchmarks/roomescape-jpa-auth-cycle2-regen
- Skill: spring-usecase-implementation
- Started at: 2026-07-03T11:06:24+09:00
- Finished at: 2026-07-03T11:09:12+09:00
- Duration minutes: 2.8
- Result status: pass

## Prompt

```text
너가 추천하는대로 구현하자. 실제로 이 부분은 내가 구현해보지 않았어.
```

Context: implement `04-prevent-concurrent-login.md` with the recommended policy: newest login wins and older bearer tokens are invalidated.

## Expected Decision Rules

- [x] auth_session_state: Token auth may introduce server-side session state when operational requirements need token invalidation.
- [x] service_boundary: AuthService owns login/session rotation; resolver owns request-token validation.
- [x] domain_naming: Login state is named `AuthSession`.
- [x] test_layering: Domain, service, controller, and acceptance tests cover the new authentication behavior.
- [x] java_class_ordering: AuthSession behavior appears above getters; helpers follow public flow.
- [x] api_contract: Login response still returns `accessToken`; stale token failure returns `UNAUTHORIZED`.
- [x] exception_style: Existing ErrorResponse/ErrorCode style is reused.
- [x] transaction_boundary: Login session rotation is transactional.
- [x] documentation: README records stateless trade-off and session policy.
- [x] verification: Targeted tests and full Gradle test pass.

## Generated Result

### Main Files

- Domain: `AuthSession`
- Auth: `AuthToken`, updated `AuthTokenProvider`, updated `LoginMemberArgumentResolver`
- Service: updated `AuthService`
- Repository: `AuthSessionRepository`
- Tests: `AuthSessionTest`, `AuthServiceIntegrationTest`, `ConcurrentLoginControllerIntegrationTest`, `ConcurrentLoginAcceptanceTest`
- Docs: `benchmarks/roomescape-jpa-auth-cycle2-regen/README.md`

### Verification

- Narrow test: `./gradlew test --tests 'roomescape.domain.AuthSessionTest' --tests 'roomescape.AuthServiceIntegrationTest' --tests 'roomescape.ConcurrentLoginControllerIntegrationTest' --tests 'roomescape.ConcurrentLoginAcceptanceTest'`
- Full verification: `./gradlew clean test`
- Result: pass

## Convention Comparison

| Category | Expected | Observed | Status | Severity | Evidence |
| --- | --- | --- | --- | --- | --- |
| auth_session_state | Server-side state is explicit when bearer tokens must be invalidated | `AuthSession` stores current session key per member and tokens carry member id plus session key | pass | P1 | `AuthSession`, `AuthTokenProvider` |
| service_boundary | Login rotation stays in AuthService and request validation stays in auth resolver | `AuthService.login` rotates session; `LoginMemberArgumentResolver` validates token against current session | pass | P1 | `AuthService`, `LoginMemberArgumentResolver` |
| domain_naming | Login state uses domain-auth language | New entity is named `AuthSession` | pass | P2 | `AuthSession` |
| test_layering | Domain/service/controller/acceptance tests cover stale token behavior | New tests cover rotation, stale token rejection, and latest token success | pass | P1 | `src/test/java/roomescape` |
| java_class_ordering | Behavior before getters and helpers under public flow | `AuthSession.rotate/matches` precede getters and `AuthService.renewSession` sits below `login` | pass | P2 | `AuthSession`, `AuthService` |
| api_contract | Existing login response stays stable and stale tokens return unauthorized | `LoginResponse(accessToken)` unchanged; stale token responses use `UNAUTHORIZED` | pass | P1 | auth tests |
| exception_style | Existing ErrorResponse/ErrorCode style remains unified | Stale session uses existing `UNAUTHORIZED` through `RoomescapeException` | pass | P1 | `LoginMemberArgumentResolver` |
| transaction_boundary | Login session replacement is atomic | `AuthService.login` is write-transactional and saves/flushed current session | pass | P1 | `AuthService` |
| documentation | README explains stateless trade-off and policy | README documents newest-login-wins, AuthSession, and per-request lookup trade-off | pass | P2 | `README.md` |
| verification | Narrow and full verification pass | Targeted auth tests and `./gradlew clean test` passed | pass | P1 | command output |

## Skill Changes

Record only changes caused by this run.

- Skill/reference file: none
- Category: none
- Change: none
- Reason: no repeated convention failure was observed
- Validation target: next mission group

## Follow-Up

- Keep: newest-login-wins policy, explicit AuthSession state, stale token as `UNAUTHORIZED`.
- Fix before next run: none required.
- Add to skill/reference: none for now.
- Next benchmark: complete the JPA add-on audit and fill any remaining JPA requirement gaps.
