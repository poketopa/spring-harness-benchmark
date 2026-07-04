# roomescape-repeat-concurrent-login-004

## Summary

- Date: 2026-07-03
- Mission: roomescape-reservation-waiting
- Cycle: concurrent-login-repeat
- Requirement: missions/roomescape-reservation-waiting/requirements/add-ons/auth-authorization/04-prevent-concurrent-login.md
- Baseline: benchmarks/roomescape-jpa-auth-manager-authorization-regen-v2
- Target project: benchmarks/repeat-runs/roomescape-concurrent-login-004
- Skill: spring-usecase-implementation
- Started at: 2026-07-03T20:50:27+09:00
- Finished at: 2026-07-03T20:51:54+09:00
- Duration minutes: 1.5
- Result status: fail

## Prompt

```text
concurrent-login-repeat run 004.
Use baseline benchmarks/roomescape-jpa-auth-manager-authorization-regen-v2.
Target benchmarks/repeat-runs/roomescape-concurrent-login-004.
Requirement missions/roomescape-reservation-waiting/requirements/add-ons/auth-authorization/04-prevent-concurrent-login.md.
Use spring-usecase-implementation skill and current benchmark records only.
Stop immediately on any test failure, P0/P1 convention issue, ambiguity, or skill/reference update need.
```

## Expected Decision Rules

- [ ] concurrent_login_policy: new login policy should be explicit.
- [ ] stateful_auth_concurrency: same-member session renewal should be serialized or conflict handled.
- [ ] server_side_session_state: token auth should check active server-side session state.
- [ ] auth_boundary: resolver should delegate token/session/member validation to AuthService.
- [ ] token_uniqueness: each login should issue a distinct token.
- [ ] active_session_constraint: one active session per member should be constrained.
- [ ] real_concurrent_test: concurrent login requirement should have a real concurrent test.
- [ ] token_provider_unit_test: token provider behavior should be directly tested.
- [ ] login_member_identity_assertion: authenticated member identity should be asserted.
- [x] verification: narrow verification failed, so full verification was not run.

## Generated Result

### Main Files

- Domain: `AuthSession`
- Controller: none
- Service: `AuthService`
- Repository: `AuthSessionRepository`, `MemberRepository`
- DTO: none
- Tests: `MemberAuthenticationAcceptanceTest`, `AuthTokenProviderTest`, `TestEntityIdSetter`
- Docs: `README.md`

### Verification

- Narrow test: `./gradlew test --tests roomescape.auth.AuthTokenProviderTest --tests roomescape.MemberAuthenticationAcceptanceTest`
- Full verification: not run
- Result: fail

The narrow command failed in `MemberAuthenticationAcceptanceTest`.
`AuthTokenProviderTest` passed, but two authentication acceptance tests failed because the test helper requested `/reservations/mine` as `List.class` even when the stale-token response body was the project error object. Spring failed to deserialize the error object into a list and raised `HttpMessageNotReadableException`.

## Convention Comparison

| Category | Expected | Observed | Status | Severity | Evidence |
| --- | --- | --- | --- | --- | --- |
| verification | Run narrow relevant tests before full Gradle verification and continue only when they pass. | Narrow verification failed in `MemberAuthenticationAcceptanceTest`; full `./gradlew clean test` was not run. | fail | P1 | benchmarks/benchmark-records/runs/roomescape-repeat-concurrent-login-004.md |

## Skill Changes

- If none: no skill/reference changes.

## Follow-Up

- Keep: failed run as benchmark evidence.
- Fix before next run: repair the stale-token assertion helper so it can assert `401 Unauthorized` without trying to parse the error response as a list, then rerun the concurrent-login repeat.
- Add to skill/reference: none yet; this is a run-local verification failure until reviewed.
- Next benchmark: stop and inspect before continuing.
- Repeated benchmark decision: failed; batch stopped.
