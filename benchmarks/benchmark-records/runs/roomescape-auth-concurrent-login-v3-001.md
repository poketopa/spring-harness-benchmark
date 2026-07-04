# roomescape-auth-concurrent-login-v3-001

## Summary

- Date: 2026-07-03
- Mission: roomescape-reservation-waiting
- Cycle: auth-authorization-regen-v3
- Requirement: missions/roomescape-reservation-waiting/requirements/add-ons/auth-authorization/04-prevent-concurrent-login.md
- Baseline: benchmarks/roomescape-jpa-auth-manager-authorization-regen-v2
- Target project: benchmarks/roomescape-jpa-auth-concurrent-login-regen-v3
- Skill: spring-usecase-implementation
- Started at: not captured
- Finished at: 2026-07-03T17:49:07+09:00
- Duration minutes: not captured
- Result status: pass

## Prompt

```text
Run the v3 regeneration verification.
Use the manager authorization result as baseline and regenerate concurrent-login prevention after correction-006.
Verify that the updated skill now produces the stateful auth concurrency and testing conventions without another correction.
```

## Expected Decision Rules

- [x] concurrent_login_policy: choose and document one explicit policy.
- [x] stateful_auth_concurrency: same-member session renewal is serialized or conflict-handled.
- [x] server_side_session_state: token authentication checks server-side active login state.
- [x] auth_boundary: argument resolver extracts the header and delegates token/session/member validation to `AuthService`.
- [x] token_uniqueness: each login issues a different token.
- [x] active_session_constraint: a member has only one active authentication session.
- [x] real_concurrent_test: concurrent-login behavior has an actual concurrent test.
- [x] token_provider_unit_test: custom token provider behavior has direct unit coverage.
- [x] login_member_identity_assertion: `AuthService.authenticate(...)` tests verify returned member id and name.
- [x] verification: run targeted tests before full verification.

## Generated Result

### Main Files

- Domain:
  - Added `AuthSession`.
  - `AuthSession` validates required member and token values.
  - `AuthSession.renew(...)` replaces the active token.
- Repository:
  - Added `AuthSessionRepository`.
  - Added `MemberRepository.findByEmailForUpdate(...)` with `PESSIMISTIC_WRITE`.
  - `AuthSession` has a unique member constraint so one member has one active session.
- Auth:
  - `AuthTokenProvider` creates a unique token per login by adding a random nonce to the payload.
  - `AuthService.login(...)` locks the target member row and creates or renews the active session with `saveAndFlush(...)`.
  - `AuthService.authenticate(...)` validates the token payload and active session.
  - `LoginMemberArgumentResolver` remains unchanged and delegates authentication to `AuthService`.
- Tests:
  - Added `AuthSessionTest`.
  - Added `AuthTokenProviderTest`.
  - Added `AuthServiceTest` with `LoginMember.id()` and `name()` assertions.
  - Added `AuthSessionRepositoryIntegrationTest`.
  - Added `ConcurrentLoginAcceptanceTest` with sequential token replacement and simultaneous first-login coverage.

## Verification

- Narrow test: `./gradlew test --tests 'roomescape.auth.AuthTokenProviderTest' --tests 'roomescape.domain.AuthSessionTest' --tests 'roomescape.service.AuthServiceTest' --tests 'roomescape.repository.AuthSessionRepositoryIntegrationTest' --tests 'roomescape.ConcurrentLoginAcceptanceTest' --tests 'roomescape.MemberAuthenticationAcceptanceTest'`
- Narrow result: pass
- Full verification: `./gradlew clean test`
- Result: pass

## Convention Comparison

| Category | Expected | Observed | Status | Severity | Evidence |
| --- | --- | --- | --- | --- | --- |
| concurrent_login_policy | One explicit policy is chosen | New login wins and previous token is invalidated | pass | P1 | `README.md` |
| stateful_auth_concurrency | Same-member session renewal is serialized or conflict-handled | Login locks the member row and flushes session renewal in service | pass | P1 | `AuthService`, `MemberRepository` |
| server_side_session_state | Token auth checks server-side active state | `AuthService.authenticate(...)` checks `AuthSession` | pass | P1 | `AuthService` |
| auth_boundary | Resolver delegates token session member validation | `LoginMemberArgumentResolver` still delegates to `AuthService` | pass | P1 | `LoginMemberArgumentResolver` |
| token_uniqueness | Each login issues a different token | `AuthTokenProvider` includes a random nonce | pass | P1 | `AuthTokenProvider` |
| active_session_constraint | One active session per member | `AuthSession` has a unique member constraint | pass | P1 | `AuthSession` |
| real_concurrent_test | Concurrent-login requirement has an actual concurrent test | `ConcurrentLoginAcceptanceTest` uses latches and verifies one active token | pass | P1 | `ConcurrentLoginAcceptanceTest` |
| token_provider_unit_test | Token provider behavior is directly tested | `AuthTokenProviderTest` covers uniqueness extraction and invalid tokens | pass | P2 | `AuthTokenProviderTest` |
| login_member_identity_assertion | Auth service verifies returned login id and name | `AuthServiceTest` asserts `LoginMember.id()` and `name()` | pass | P2 | `AuthServiceTest` |
| verification | Targeted then full verification passes | Both targeted tests and `./gradlew clean test` passed | pass | P1 | this run note |

## Skill Changes

- No new skill/reference changes were needed.
- Validated pending `skill-update-021`, `skill-update-022`, and `skill-update-023`.

## Follow-Up

- Concurrent-login skill stabilization passed for this run.
- Next recommended step is a manual convention review of v3 files, then move to the next research unit only if no new convention gaps are found.
