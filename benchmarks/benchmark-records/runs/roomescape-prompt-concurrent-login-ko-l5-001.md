# roomescape-prompt-concurrent-login-ko-l5-001

## Summary

- Date: 2026-07-04
- Mission: roomescape-reservation-waiting
- Cycle: prompt-sufficiency
- Feature: concurrent-login
- Prompt language: ko
- Prompt level: L5
- Repeat: 001
- Requirement: `benchmarks/prompt-sufficiency-cases/roomescape/concurrent-login/ko/l5.md`
- Oracle: `benchmarks/prompt-sufficiency-cases/roomescape/oracles/concurrent-login.md`
- Baseline: `benchmarks/roomescape-jpa-auth-manager-authorization-regen-v2`
- Target project: `benchmarks/prompt-sufficiency-runs/roomescape-prompt-concurrent-login-ko-l5-001`
- Skill: spring-usecase-implementation
- Started at: 2026-07-04T11:47:06+09:00
- Finished at: 2026-07-04T11:49:36+09:00
- Duration minutes: 2.5
- Result status: pass

## Prompt

Only the implementation prompt payload from `benchmarks/prompt-sufficiency-cases/roomescape/concurrent-login/ko/l5.md` was used.

```text
동시 로그인 정책을 구현해 주세요. 같은 회원이 다시 로그인하면 새 로그인만 유효하고 이전 토큰은 더 이상 사용할 수 없어야 합니다. 다른 회원의 로그인은 영향을 받지 않아야 합니다. 서버 쪽 세션 상태나 동등한 저장 구조로 active session을 검증하고, 이전 토큰 사용은 의도한 인증 실패 응답으로 처리해 주세요. 같은 회원이 동시에 로그인하는 race에서도 DB 제약 예외가 500으로 노출되지 않아야 합니다. 새 토큰 성공, 이전 토큰 실패, 다른 회원 토큰 유지, 실제 동시 로그인 테스트를 추가하고 전체 검증을 실행해 주세요.
```

## Prompt Sufficiency Notes

The L5 prompt explicitly states newest login wins, old tokens fail, other users remain logged in, server-side session state is required, race handling must avoid 500s, and full verification is required.

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
| server_side_session_state | Active login state is server-side enough to invalidate stale tokens | LoginSession stores one active session key per member and AuthService validates it on every authenticated request | pass | P1 | benchmarks/prompt-sufficiency-runs/roomescape-prompt-concurrent-login-ko-l5-001 |
| active_session_constraint | Newest login invalidates older same-member tokens without affecting other members | AuthService renews the member session under lock and old token requests return UNAUTHORIZED | pass | P1 | benchmarks/prompt-sufficiency-runs/roomescape-prompt-concurrent-login-ko-l5-001 |
| concurrent_session_race | Same-member concurrent login does not leak database uniqueness errors | Member row pessimistic lock serializes session renewal and saveAndFlush keeps failures inside the service boundary | pass | P1 | benchmarks/prompt-sufficiency-runs/roomescape-prompt-concurrent-login-ko-l5-001 |
| real_concurrent_test | Same-member login race is covered by a real concurrent test | ConcurrentLoginAcceptanceTest uses CountDownLatch and ExecutorService for simultaneous login attempts | pass | P1 | benchmarks/prompt-sufficiency-runs/roomescape-prompt-concurrent-login-ko-l5-001 |
| token_provider_unit_test | Token parsing and invalid-token behavior are directly covered | AuthTokenProviderTest covers payload extraction and invalid token UNAUTHORIZED | pass | P1 | benchmarks/prompt-sufficiency-runs/roomescape-prompt-concurrent-login-ko-l5-001 |
| authentication_boundary | Web resolver delegates token and session validation to AuthService | LoginMemberArgumentResolver extracts the bearer token and AuthService authenticates active session state | pass | P1 | benchmarks/prompt-sufficiency-runs/roomescape-prompt-concurrent-login-ko-l5-001 |
| exception_style | Old and invalid tokens use intentional authentication failure style | Stale and invalid tokens flow through RoomescapeException UNAUTHORIZED and ErrorResponse | pass | P1 | benchmarks/prompt-sufficiency-runs/roomescape-prompt-concurrent-login-ko-l5-001 |
| verification | Each target passes full Gradle verification | ./gradlew clean test passed for this run target | pass | P1 | benchmarks/prompt-sufficiency-runs/roomescape-prompt-concurrent-login-ko-l5-001 |

## Skill Changes

- Skill/reference file: none
- Category: none
- Change: none
- Reason: baseline prompt-sufficiency run only
- Validation target: none
- If none: no skill/reference/evaluator/oracle intervention was made

## Follow-Up

- Keep: concurrent-login L5 ko repeat passed the strict hidden oracle.
- Fix before next run: none.
- Add to skill/reference: none until the 84-run baseline set is complete.
- Next benchmark: continue prompt-sufficiency baseline set until complete.
- Repeated benchmark decision: clean pass.
