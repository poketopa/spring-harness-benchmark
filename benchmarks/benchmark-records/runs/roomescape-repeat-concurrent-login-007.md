# roomescape-repeat-concurrent-login-007

## Summary

- Date: 2026-07-03
- Mission: roomescape-reservation-waiting
- Cycle: concurrent-login-repeat
- Requirement: missions/roomescape-reservation-waiting/requirements/add-ons/auth-authorization/04-prevent-concurrent-login.md
- Baseline: benchmarks/roomescape-jpa-auth-manager-authorization-regen-v2
- Target project: benchmarks/repeat-runs/roomescape-concurrent-login-007
- Skill: spring-usecase-implementation
- Started at: 2026-07-03T21:38:28+09:00
- Finished at: 2026-07-03T21:41:10+09:00
- Duration minutes: 2.7
- Result status: pass

## Prompt

```text
concurrent-login-repeat run 007 after run-local correction 008.
Use baseline benchmarks/roomescape-jpa-auth-manager-authorization-regen-v2.
Target benchmarks/repeat-runs/roomescape-concurrent-login-007.
Requirement missions/roomescape-reservation-waiting/requirements/add-ons/auth-authorization/04-prevent-concurrent-login.md.
Use spring-usecase-implementation skill and current benchmark records only.
```

## Expected Decision Rules

- [x] concurrent_login_policy: new login wins.
- [x] stateful_auth_concurrency: login locks the member row before renewing the active session.
- [x] server_side_session_state: AuthSession stores one active session key per member.
- [x] auth_boundary: resolver delegates token/session/member validation to AuthService.
- [x] token_uniqueness: token includes session key and random nonce.
- [x] active_session_constraint: AuthSession has unique member and session-key constraints.
- [x] real_concurrent_test: acceptance test uses latches and asserts exactly one active token.
- [x] token_provider_unit_test: token provider has direct unit coverage.
- [x] login_member_identity_assertion: AuthServiceIntegrationTest asserts authenticated id/name.
- [x] verification: narrow tests and full clean test passed.

## Generated Result

### Main Files

- Domain: `AuthSession`
- Controller: none
- Service: `AuthService`
- Repository: `AuthSessionRepository`, `MemberRepository`
- DTO/Auth types: `TokenPayload`
- Tests: `AuthSessionTest`, `AuthTokenProviderTest`, `AuthServiceIntegrationTest`, `MemberAuthenticationAcceptanceTest`
- Docs: `README.md`

### Verification

- Narrow test: `./gradlew test --tests roomescape.auth.AuthTokenProviderTest --tests roomescape.domain.AuthSessionTest --tests roomescape.service.AuthServiceIntegrationTest --tests roomescape.MemberAuthenticationAcceptanceTest`
- Full verification: `./gradlew clean test`
- Result: pass

## Convention Comparison

| Category | Expected | Observed | Status | Severity | Evidence |
| --- | --- | --- | --- | --- | --- |
| concurrent_login_policy | One explicit concurrent login policy is chosen. | README documents new login wins and previous token is invalidated. | pass | P1 | benchmarks/repeat-runs/roomescape-concurrent-login-007/README.md |
| stateful_auth_concurrency | Same member session renewal is serialized or conflict handled. | AuthService locks the member row before renewing AuthSession. | pass | P1 | benchmarks/repeat-runs/roomescape-concurrent-login-007/src/main/java/roomescape/service/AuthService.java |
| server_side_session_state | Token auth checks server-side active login state. | AuthSession stores active session key and AuthService.authenticate checks it. | pass | P1 | benchmarks/repeat-runs/roomescape-concurrent-login-007/src/main/java/roomescape/domain/AuthSession.java |
| auth_boundary | Resolver delegates token/session/member validation. | LoginMemberArgumentResolver still delegates authentication to AuthService. | pass | P1 | benchmarks/repeat-runs/roomescape-concurrent-login-007/src/main/java/roomescape/auth/LoginMemberArgumentResolver.java |
| token_uniqueness | Each login issues a different token. | AuthTokenProvider includes a session key and random nonce. | pass | P1 | benchmarks/repeat-runs/roomescape-concurrent-login-007/src/main/java/roomescape/auth/AuthTokenProvider.java |
| active_session_constraint | One active session per member. | AuthSession table has unique member and session-key constraints. | pass | P1 | benchmarks/repeat-runs/roomescape-concurrent-login-007/src/main/java/roomescape/domain/AuthSession.java |
| real_concurrent_test | Concurrent login requirement has an actual concurrent test. | MemberAuthenticationAcceptanceTest uses latches and verifies exactly one active token. | pass | P1 | benchmarks/repeat-runs/roomescape-concurrent-login-007/src/test/java/roomescape/MemberAuthenticationAcceptanceTest.java |
| token_provider_unit_test | Token provider behavior is directly tested. | AuthTokenProviderTest covers uniqueness, payload extraction, and invalid tokens. | pass | P2 | benchmarks/repeat-runs/roomescape-concurrent-login-007/src/test/java/roomescape/auth/AuthTokenProviderTest.java |
| login_member_identity_assertion | Auth service verifies returned login id and name. | AuthServiceIntegrationTest asserts LoginMember id and name. | pass | P2 | benchmarks/repeat-runs/roomescape-concurrent-login-007/src/test/java/roomescape/service/AuthServiceIntegrationTest.java |
| verification | Run narrow relevant tests before full Gradle verification. | Targeted auth tests and `./gradlew clean test` passed. | pass | P1 | benchmarks/benchmark-records/runs/roomescape-repeat-concurrent-login-007.md |

## Skill Changes

- If none: no skill/reference changes.

## Follow-Up

- Keep: new-login-wins policy, AuthSession active state, member lock and flush, real concurrent acceptance test.
- Fix before next run: none.
- Add to skill/reference: none.
- Next benchmark: final summary.
- Repeated benchmark decision: clean pass; correction-008 now has three consecutive post-correction concurrent-login passes.
