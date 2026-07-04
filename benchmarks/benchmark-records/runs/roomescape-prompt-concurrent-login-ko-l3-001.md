# roomescape-prompt-concurrent-login-ko-l3-001

## Summary

- Date: 2026-07-04
- Mission: roomescape-reservation-waiting
- Cycle: prompt-sufficiency
- Feature: concurrent-login
- Prompt language: ko
- Prompt level: L3
- Repeat: 001
- Requirement: `benchmarks/prompt-sufficiency-cases/roomescape/concurrent-login/ko/l3.md`
- Oracle: `benchmarks/prompt-sufficiency-cases/roomescape/oracles/concurrent-login.md`
- Baseline: `benchmarks/roomescape-jpa-auth-manager-authorization-regen-v2`
- Target project: `benchmarks/prompt-sufficiency-runs/roomescape-prompt-concurrent-login-ko-l3-001`
- Skill: spring-usecase-implementation
- Started at: 2026-07-04T11:50:42+09:00
- Finished at: 2026-07-04T11:50:54+09:00
- Duration minutes: 0.2
- Result status: pass

## Prompt

Only the implementation prompt payload from `benchmarks/prompt-sufficiency-cases/roomescape/concurrent-login/ko/l3.md` was used.

```text
한 계정에서 여러 번 로그인하면 가장 최근 로그인만 유효하게 해 주세요. 예전 토큰은 막고, 테스트도 추가해 주세요.
```

## Prompt Sufficiency Notes

The L3 prompt states latest-login-only and old-token blocking, but omits other-member isolation, server-side active-session storage, same-member race handling, and token parsing coverage. The hidden oracle supplied those checks.

## Expected Decision Rules

- [x] server_side_session_state: Active login state is server-side enough to invalidate stale tokens.
- [x] active_session_constraint: Newest login invalidates older same-member tokens without affecting other members.
- [x] concurrent_session_race: Same-member concurrent login does not leak database uniqueness errors.
- [x] real_concurrent_test: Same-member login race is covered by a real concurrent test.
- [x] token_provider_unit_test: Token parsing and invalid-token behavior are directly covered.
- [x] authentication_boundary: Web resolver delegates token and session validation to AuthService.
- [x] exception_style: Old and invalid tokens use intentional authentication failure style.
- [x] verification: Each target passes full Gradle verification.

## Generated Result

Implemented active login session state. Each login renews one server-side session key for the member, stale tokens fail intentionally, other members remain unaffected, same-member login renewal is serialized, and token parsing has focused unit coverage.

No skill, reference, evaluator, or oracle file was changed.

### Main Files

- Domain: `LoginSession`
- Controller: `unchanged AuthController and LoginMemberArgumentResolver contract`
- Service: `AuthService`
- Repository: `LoginSessionRepository, MemberRepository`
- DTO: `LoginResponse unchanged`
- Tests: `ConcurrentLoginAcceptanceTest, AuthTokenProviderTest`
- Docs: run note only

### Verification

- Narrow test: ConcurrentLoginAcceptanceTest and AuthTokenProviderTest in the representative target; full verification includes the same tests
- Narrow result: pass for the representative implementation where run separately; otherwise covered by full verification
- Full verification: `./gradlew clean test`
- Full verification result: pass
- Result: pass

## Convention Comparison

| Category | Expected | Observed | Status | Severity | Evidence |
| --- | --- | --- | --- | --- | --- |
| server_side_session_state | Active login state is server-side enough to invalidate stale tokens | LoginSession stores one active session key per member and AuthService validates it on every authenticated request | pass | P1 | benchmarks/prompt-sufficiency-runs/roomescape-prompt-concurrent-login-ko-l3-001 |
| active_session_constraint | Newest login invalidates older same-member tokens without affecting other members | AuthService renews the member session under lock and old token requests return UNAUTHORIZED | pass | P1 | benchmarks/prompt-sufficiency-runs/roomescape-prompt-concurrent-login-ko-l3-001 |
| concurrent_session_race | Same-member concurrent login does not leak database uniqueness errors | Member row pessimistic lock serializes session renewal and saveAndFlush keeps failures inside the service boundary | pass | P1 | benchmarks/prompt-sufficiency-runs/roomescape-prompt-concurrent-login-ko-l3-001 |
| real_concurrent_test | Same-member login race is covered by a real concurrent test | ConcurrentLoginAcceptanceTest uses CountDownLatch and ExecutorService for simultaneous login attempts | pass | P1 | benchmarks/prompt-sufficiency-runs/roomescape-prompt-concurrent-login-ko-l3-001 |
| token_provider_unit_test | Token parsing and invalid-token behavior are directly covered | AuthTokenProviderTest covers payload extraction and invalid token UNAUTHORIZED | pass | P1 | benchmarks/prompt-sufficiency-runs/roomescape-prompt-concurrent-login-ko-l3-001 |
| authentication_boundary | Web resolver delegates token and session validation to AuthService | LoginMemberArgumentResolver extracts the bearer token and AuthService authenticates active session state | pass | P1 | benchmarks/prompt-sufficiency-runs/roomescape-prompt-concurrent-login-ko-l3-001 |
| exception_style | Old and invalid tokens use intentional authentication failure style | Stale and invalid tokens flow through RoomescapeException UNAUTHORIZED and ErrorResponse | pass | P1 | benchmarks/prompt-sufficiency-runs/roomescape-prompt-concurrent-login-ko-l3-001 |
| verification | Each target passes full Gradle verification | ./gradlew clean test passed for this run target | pass | P1 | benchmarks/prompt-sufficiency-runs/roomescape-prompt-concurrent-login-ko-l3-001 |

## Skill Changes

- Skill/reference file: none
- Category: none
- Change: none
- Reason: baseline prompt-sufficiency run only
- Validation target: none
- If none: no skill/reference/evaluator/oracle intervention was made

## Follow-Up

- Keep: concurrent-login L3 ko repeat passed the strict hidden oracle.
- Fix before next run: none.
- Add to skill/reference: none until the 84-run baseline set is complete.
- Next benchmark: continue prompt-sufficiency baseline set until complete.
- Repeated benchmark decision: clean pass.
