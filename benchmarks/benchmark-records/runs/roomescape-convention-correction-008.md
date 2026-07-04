# roomescape-convention-correction-008

## Summary

- Date: 2026-07-03
- Mission: roomescape-reservation-waiting
- Cycle: convention-correction
- Requirement: concurrent-login-repeat 004 verification failure correction
- Baseline: benchmarks/repeat-runs/roomescape-concurrent-login-004
- Target project: benchmarks/repeat-runs/roomescape-concurrent-login-004
- Skill: spring-usecase-implementation
- Started at: 2026-07-03T21:00:56+09:00
- Finished at: 2026-07-03T21:01:42+09:00
- Duration minutes: 0.8
- Result status: pass

## Prompt

```text
Treat roomescape-repeat-concurrent-login-004 as a run-local correction.
Fix the stale-token assertion helper that parsed an unauthorized error response as a List, verify, record the correction, and continue.
```

## Expected Decision Rules

- [x] verification: stale-token assertions check status without forcing success-response body parsing.

## Generated Result

### Main Files

- Tests: `MemberAuthenticationAcceptanceTest`

### Verification

- Narrow test: `./gradlew test --tests roomescape.auth.AuthTokenProviderTest --tests roomescape.MemberAuthenticationAcceptanceTest`
- Full verification: `./gradlew clean test`
- Result: pass

## Convention Comparison

| Category | Expected | Observed | Status | Severity | Evidence |
| --- | --- | --- | --- | --- | --- |
| verification | Stale-token test assertions can inspect `401 Unauthorized` without deserializing the error body as a success list. | `findMineWithStatus` now reads `/reservations/mine` as `String.class` and asserts only response status. | pass | P1 | benchmarks/repeat-runs/roomescape-concurrent-login-004/src/test/java/roomescape/MemberAuthenticationAcceptanceTest.java |

## Skill Changes

- If none: no skill/reference changes.

## Follow-Up

- Keep: run-local correction classification; no skill/reference update for a single local helper mistake.
- Fix before next run: none.
- Add to skill/reference: none.
- Next benchmark: resume additional repeated benchmark runs.
- Repeated benchmark decision: correction passed; resume repeated benchmark.
