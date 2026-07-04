# roomescape-repeat-concurrent-login-003

## Summary

- Date: 2026-07-03
- Mission: roomescape-reservation-waiting
- Cycle: concurrent-login-repeat
- Requirement: missions/roomescape-reservation-waiting/requirements/add-ons/auth-authorization/04-prevent-concurrent-login.md
- Baseline: benchmarks/roomescape-jpa-auth-manager-authorization-regen-v2
- Target project: benchmarks/repeat-runs/roomescape-concurrent-login-003
- Skill: spring-usecase-implementation
- Started at: 2026-07-03T20:24:14+09:00
- Finished at: 2026-07-03T20:24:43+09:00
- Duration minutes: 0.5
- Result status: pass

## Prompt

```text
concurrent-login-repeat run 003.
Use baseline benchmarks/roomescape-jpa-auth-manager-authorization-regen-v2.
Target benchmarks/repeat-runs/roomescape-concurrent-login-003.
Requirement missions/roomescape-reservation-waiting/requirements/add-ons/auth-authorization/04-prevent-concurrent-login.md.
Use spring-usecase-implementation skill and current benchmark records only.
```

## Expected Decision Rules

- [x] concurrent_login_policy: new login wins.
- [x] stateful_auth_concurrency: login locks the member row and flushes session renewal.
- [x] server_side_session_state: AuthSession stores one active session key per member.
- [x] auth_boundary: resolver delegates token/session/member validation to AuthService.
- [x] token_uniqueness: token includes random nonce and session key.
- [x] active_session_constraint: AuthSession has unique member constraint.
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
- DTO: none
- Tests: `AuthSessionTest`, `AuthTokenProviderTest`, `AuthServiceIntegrationTest`, `ConcurrentLoginAcceptanceTest`
- Docs: `README.md`

### Verification

- Narrow test: `./gradlew test --tests roomescape.domain.AuthSessionTest --tests roomescape.auth.AuthTokenProviderTest --tests roomescape.service.AuthServiceIntegrationTest --tests roomescape.ConcurrentLoginAcceptanceTest`
- Full verification: `./gradlew clean test`
- Result: pass

## Convention Comparison

| Category | Expected | Observed | Status | Severity | Evidence |
| --- | --- | --- | --- | --- | --- |
| concurrent_login_policy | One explicit concurrent login policy is chosen. | New login wins and previous token is invalidated. | pass | P1 | benchmarks/repeat-runs/roomescape-concurrent-login-003/README.md |
| stateful_auth_concurrency | Same member session renewal is serialized or conflict handled. | AuthService.login locks the member row and flushes session renewal. | pass | P1 | benchmarks/repeat-runs/roomescape-concurrent-login-003/src/main/java/roomescape/service/AuthService.java |
| server_side_session_state | Token auth checks server-side active login state. | AuthSession stores active session and AuthService.authenticate checks it. | pass | P1 | benchmarks/repeat-runs/roomescape-concurrent-login-003/src/main/java/roomescape/domain/AuthSession.java |
| auth_boundary | Resolver delegates token/session/member validation. | LoginMemberArgumentResolver still delegates authentication to AuthService. | pass | P1 | benchmarks/repeat-runs/roomescape-concurrent-login-003/src/main/java/roomescape/auth/LoginMemberArgumentResolver.java |
| token_uniqueness | Each login issues a different token. | AuthTokenProvider includes a random nonce and session key. | pass | P1 | benchmarks/repeat-runs/roomescape-concurrent-login-003/src/main/java/roomescape/auth/AuthTokenProvider.java |
| active_session_constraint | One active session per member. | AuthSession has a unique member constraint. | pass | P1 | benchmarks/repeat-runs/roomescape-concurrent-login-003/src/main/java/roomescape/domain/AuthSession.java |
| real_concurrent_test | Concurrent login requirement has an actual concurrent test. | ConcurrentLoginAcceptanceTest uses latches and verifies exactly one active token. | pass | P1 | benchmarks/repeat-runs/roomescape-concurrent-login-003/src/test/java/roomescape/ConcurrentLoginAcceptanceTest.java |
| token_provider_unit_test | Token provider behavior is directly tested. | AuthTokenProviderTest covers uniqueness extraction and invalid tokens. | pass | P2 | benchmarks/repeat-runs/roomescape-concurrent-login-003/src/test/java/roomescape/auth/AuthTokenProviderTest.java |
| login_member_identity_assertion | Auth service verifies returned login id and name. | AuthServiceIntegrationTest asserts LoginMember id and name. | pass | P2 | benchmarks/repeat-runs/roomescape-concurrent-login-003/src/test/java/roomescape/service/AuthServiceIntegrationTest.java |
| verification | Run narrow relevant tests before full Gradle verification. | Targeted auth tests and ./gradlew clean test passed. | pass | P1 | benchmarks/benchmark-records/runs/roomescape-repeat-concurrent-login-003.md |

## Skill Changes

- If none: no skill/reference changes.

## Follow-Up

- Keep: new-login-wins policy, AuthSession active state, member lock and flush, real concurrent acceptance test.
- Fix before next run: none.
- Add to skill/reference: none.
- Next benchmark: final summary.
- Repeated benchmark decision: clean pass.
