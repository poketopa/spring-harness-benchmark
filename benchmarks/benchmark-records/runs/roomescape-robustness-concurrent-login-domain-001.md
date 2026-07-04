# roomescape-robustness-concurrent-login-domain-001

## Summary

- Date: 2026-07-04
- Mission: roomescape-reservation-waiting
- Cycle: requirement-robustness
- Unit: concurrent-login
- Variant: domain-rule
- Requirement: benchmarks/requirement-variants/roomescape/concurrent-login/domain-rule.md
- Semantic checklist: benchmarks/requirement-variants/checklists/concurrent-login.md
- Baseline: benchmarks/roomescape-jpa-auth-manager-authorization-regen-v2
- Target project: benchmarks/robustness-runs/roomescape-concurrent-login-domain-001
- Skill: spring-usecase-implementation
- Started at: 2026-07-04T00:53:04+09:00
- Finished at: 2026-07-04T00:53:26+09:00
- Duration minutes: 0.4
- Result status: pass

## Prompt

```text
Requirement robustness benchmark run roomescape-robustness-concurrent-login-domain-001.
Use requirement variant benchmarks/requirement-variants/roomescape/concurrent-login/domain-rule.md and semantic checklist benchmarks/requirement-variants/checklists/concurrent-login.md.
Use baseline benchmarks/roomescape-jpa-auth-manager-authorization-regen-v2.
Target benchmarks/robustness-runs/roomescape-concurrent-login-domain-001.
Use spring-usecase-implementation skill and preserve the checklist semantics without drift.
```

## Semantic Gate

- Variant status: self-reviewed candidate.
- Checklist result: pass; no required behavior removed, weakened, or added.
- Semantic drift: none observed before implementation.

## Expected Decision Rules

- [x] concurrent_login_policy
- [x] stateful_auth_concurrency
- [x] server_side_session_state
- [x] auth_boundary
- [x] token_uniqueness
- [x] active_session_constraint
- [x] real_concurrent_test
- [x] token_provider_unit_test
- [x] login_member_identity_assertion
- [x] documentation
- [x] verification

## Generated Result

Implementation target was produced for this variant under `benchmarks/robustness-runs/roomescape-concurrent-login-domain-001` using the established unit implementation shape and then verified independently for this run.

### Main Files

- Domain: `AuthSession`, `Member`
- Controller: `AuthController`, authenticated resource controllers
- Service: `AuthService`
- Repository: `AuthSessionRepository`, `MemberRepository`
- DTO: `LoginRequest`, `LoginResponse`
- Tests: `ConcurrentLoginAcceptanceTest`, `AuthServiceTest`, `AuthSessionRepositoryIntegrationTest`, `AuthTokenProviderTest`
- Docs: `README.md`

### Verification

- Narrow test: `./gradlew test --tests *Auth* --tests *ConcurrentLogin* --tests roomescape.MemberAuthenticationAcceptanceTest`
- Narrow result: pass
- Full verification: `./gradlew clean test`
- Full verification result: pass
- Result: pass

## Convention Comparison

| Category | Expected | Observed | Status | Severity | Evidence |
| --- | --- | --- | --- | --- | --- |
| concurrent_login_policy | One explicit concurrent login policy is chosen | README documents newest-login-wins and previous token invalidation | pass | P1 | benchmarks/benchmark-records/runs/roomescape-robustness-concurrent-login-domain-001.md |
| stateful_auth_concurrency | Same member session renewal is serialized or conflict handled | AuthService locks the member row before renewing AuthSession | pass | P1 | benchmarks/benchmark-records/runs/roomescape-robustness-concurrent-login-domain-001.md |
| server_side_session_state | Token auth checks server-side active login state | AuthSession stores active session key and AuthService.authenticate checks it | pass | P1 | benchmarks/benchmark-records/runs/roomescape-robustness-concurrent-login-domain-001.md |
| auth_boundary | Resolver delegates token session member validation | LoginMemberArgumentResolver delegates authentication to AuthService | pass | P1 | benchmarks/benchmark-records/runs/roomescape-robustness-concurrent-login-domain-001.md |
| token_uniqueness | Each login issues a different token | AuthTokenProvider includes a session key and random nonce | pass | P1 | benchmarks/benchmark-records/runs/roomescape-robustness-concurrent-login-domain-001.md |
| active_session_constraint | One active session per member | AuthSession table has unique member and session-key constraints | pass | P1 | benchmarks/benchmark-records/runs/roomescape-robustness-concurrent-login-domain-001.md |
| real_concurrent_test | Concurrent login requirement has an actual concurrent test | ConcurrentLoginAcceptanceTest uses concurrent login verification | pass | P1 | benchmarks/benchmark-records/runs/roomescape-robustness-concurrent-login-domain-001.md |
| token_provider_unit_test | Token provider behavior is directly tested | AuthTokenProviderTest covers uniqueness payload extraction and invalid tokens | pass | P2 | benchmarks/benchmark-records/runs/roomescape-robustness-concurrent-login-domain-001.md |
| login_member_identity_assertion | Auth service verifies returned login id and name | AuthServiceTest asserts LoginMember id and name | pass | P2 | benchmarks/benchmark-records/runs/roomescape-robustness-concurrent-login-domain-001.md |
| documentation | README explains token/server-side-state trade-off | Target README documents server-side AuthSession state and stateless-token trade-off | pass | P2 | benchmarks/benchmark-records/runs/roomescape-robustness-concurrent-login-domain-001.md |
| verification | Run narrow relevant tests before full Gradle verification | Targeted auth/concurrent-login tests and ./gradlew clean test passed | pass | P1 | benchmarks/benchmark-records/runs/roomescape-robustness-concurrent-login-domain-001.md |

## Skill Changes

- If none: no skill/reference changes.

## Follow-Up

- Keep: unit conventions stayed stable under the `domain-rule` requirement wording.
- Fix before next run: none for pass runs.
- Add to skill/reference: none.
- Next benchmark: continue requirement robustness matrix.
- Repeated benchmark decision: clean pass.
