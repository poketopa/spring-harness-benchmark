# roomescape-auth-concurrent-login-v2-001

## Summary

- Date: 2026-07-03
- Mission: roomescape-reservation-waiting
- Cycle: auth-authorization-regen-v2
- Requirement: missions/roomescape-reservation-waiting/requirements/add-ons/auth-authorization/04-prevent-concurrent-login.md
- Baseline: benchmarks/roomescape-jpa-auth-manager-authorization-regen-v2
- Target project: benchmarks/roomescape-jpa-auth-concurrent-login-regen-v2
- Skill: spring-usecase-implementation
- Started at: not captured
- Finished at: 2026-07-03T17:28:55+09:00
- Duration minutes: not captured
- Result status: pass

## Prompt

```text
Proceed with the recommended next step.
Regenerate the concurrent-login prevention add-on from the manager authorization result.
Use the recommended policy: new login wins and previous token is invalidated.
```

## Expected Decision Rules

- [x] concurrent_login_policy: choose and document one explicit policy.
- [x] server_side_session_state: token authentication checks server-side active login state.
- [x] auth_boundary: argument resolver extracts the header and delegates token/session/member validation to `AuthService`.
- [x] token_uniqueness: each login issues a different token.
- [x] active_session_constraint: a member has only one active authentication session.
- [x] stale_token_error: an invalidated token returns `UNAUTHORIZED`.
- [x] test_layering: domain, service, repository, and acceptance tests cover the behavior.
- [x] verification: run targeted tests before full verification.

## Generated Result

### Main Files

- Domain:
  - Added `AuthSession`.
  - `AuthSession` validates required member and token values.
  - `AuthSession.renew(...)` replaces the active token.
- Repository:
  - Added `AuthSessionRepository`.
  - `AuthSession` has a unique member constraint so one member has one active session.
- Auth:
  - `AuthTokenProvider` now creates a unique token per login by adding a random nonce to the payload.
  - `AuthTokenProvider` still only creates and parses token payloads.
  - `AuthService.login(...)` creates or renews the active session.
  - `AuthService.authenticate(...)` validates the token payload and active session.
  - `LoginMemberArgumentResolver` remains unchanged and delegates authentication to `AuthService`.
- Tests:
  - Added `AuthSessionTest`.
  - Added `AuthServiceTest`.
  - Added `AuthSessionRepositoryIntegrationTest`.
  - Added `ConcurrentLoginAcceptanceTest`.

## Verification

- Narrow test: `./gradlew test --tests 'roomescape.domain.AuthSessionTest' --tests 'roomescape.service.AuthServiceTest' --tests 'roomescape.repository.AuthSessionRepositoryIntegrationTest' --tests 'roomescape.ConcurrentLoginAcceptanceTest' --tests 'roomescape.MemberAuthenticationAcceptanceTest'`
- Narrow result: pass
- Full verification: `./gradlew clean test`
- Result: pass

## Convention Comparison

| Category | Expected | Observed | Status | Severity | Evidence |
| --- | --- | --- | --- | --- | --- |
| concurrent_login_policy | One explicit policy is chosen | New login wins and previous token is invalidated | pass | P1 | `README.md` |
| server_side_session_state | Token auth checks server-side active state | `AuthService.authenticate(...)` checks `AuthSession` | pass | P1 | `src/main/java/roomescape/service/AuthService.java` |
| auth_boundary | Resolver delegates token session member validation | `LoginMemberArgumentResolver` still delegates to `AuthService` | pass | P1 | `src/main/java/roomescape/auth/LoginMemberArgumentResolver.java` |
| token_uniqueness | Each login issues a different token | `AuthTokenProvider` includes a random nonce | pass | P1 | `src/main/java/roomescape/auth/AuthTokenProvider.java` |
| active_session_constraint | One active session per member | `AuthSession` has a unique member constraint | pass | P1 | `src/main/java/roomescape/domain/AuthSession.java` |
| stale_token_error | Invalidated token returns `UNAUTHORIZED` | Acceptance test verifies old token rejection | pass | P1 | `src/test/java/roomescape/ConcurrentLoginAcceptanceTest.java` |
| test_layering | Layered tests cover meaningful behavior | Domain service repository and acceptance tests were added | pass | P1 | `src/test/java/roomescape` |
| verification | Targeted then full verification passes | Both targeted tests and `./gradlew clean test` passed | pass | P1 | this run note |

## Skill Changes

- No skill/reference changes were needed.

## Follow-Up

- Candidate review item: this solution intentionally makes token authentication stateful. That matches the concurrent-login requirement, but should be called out if the portfolio narrative compares stateless token auth and server-side sessions.
- Next benchmark candidate: manual convention review of this auth-session output before moving to any external integration or additional add-on.
