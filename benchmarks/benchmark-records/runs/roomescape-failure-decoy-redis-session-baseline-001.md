# roomescape-failure-decoy-redis-session-baseline-001

## Summary

- Date: 2026-07-04
- Mission: roomescape-reservation-waiting
- Cycle: failure-recovery
- Requirement: `benchmarks/failure-cases/roomescape/concurrent-login/decoy-redis-session.md`
- Baseline: `benchmarks/roomescape-jpa-auth-manager-authorization-regen-v2`
- Target project: `benchmarks/failure-runs/roomescape-failure-decoy-redis-session-baseline-001`
- Skill: spring-usecase-implementation
- Started at: 2026-07-04T01:47:53+09:00
- Finished at: 2026-07-04T01:53:38+09:00
- Duration minutes: 5.8
- Result status: pass

## Prompt

Only the implementation prompt payload from `benchmarks/failure-cases/roomescape/concurrent-login/decoy-redis-session.md` was used.

## Expected Decision Rules

- [x] decoy_scope_control: Redis is treated as future scope and not added to the current implementation.
- [x] verification: Narrow and full tests pass.

## Generated Result

The baseline implemented newest-login-wins concurrent login prevention without adding Redis or any Redis dependency.

### Main Files

- Domain: unchanged
- Controller: unchanged
- Service: `src/main/java/roomescape/service/AuthService.java`
- Repository: unchanged
- DTO: unchanged
- Tests: `src/test/java/roomescape/MemberAuthenticationAcceptanceTest.java`
- Docs: `docs/adr/0001-concurrent-login-policy.md`
- Auth: `src/main/java/roomescape/auth/AuthTokenProvider.java`, `src/main/java/roomescape/auth/ActiveLoginSessionStore.java`

### Verification

- Narrow test: `./gradlew test --tests roomescape.MemberAuthenticationAcceptanceTest`
- Full verification: `./gradlew test`
- Result: pass. Parsed test XML showed 71 tests, 0 failures, 0 errors, 0 skipped.

## Convention Comparison

| Category | Expected | Observed | Status | Severity | Evidence |
| --- | --- | --- | --- | --- | --- |
| decoy_scope_control | Redis remains future scope and no Redis dependency is added. | No Redis dependency was added and Redis appears only in ADR future-scope documentation. | pass | P1 | `build.gradle` and source search found no Redis dependency. |
| verification | Narrow and full verification pass. | Targeted auth test and full `./gradlew test` passed. | pass | P1 | Gradle XML summary recorded 71 tests and 0 failures. |

## Skill Changes

- Skill/reference file: none
- Category: none
- Change: none
- Reason: baseline passed the decoy-scope decision rule
- Validation target: none
- If none: no intervention or rerun is required for this case at this time

## Follow-Up

- Keep: Decoy infrastructure mention did not cause speculative Redis adoption.
- Fix before next run: none for this case.
- Add to skill/reference: none.
- Next benchmark: proceed to another failure-recovery case.
- Repeated benchmark decision: baseline outcome is a pass for `decoy_scope_control`; rerun is skipped because no intervention was needed.
