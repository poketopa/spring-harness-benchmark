# roomescape-prompt-blind-waiting-rank-ko-l1-001

## Summary

- Date: 2026-07-04
- Mission: `roomescape-reservation-waiting`
- Cycle: `prompt-sufficiency-blind`
- Feature: `waiting-rank`
- Prompt language: `ko`
- Prompt level: `L1`
- Repeat: `001`
- Prompt path: `benchmarks/prompt-sufficiency-cases/roomescape/waiting-rank/ko/l1.md`
- Hidden oracle path: `benchmarks/prompt-sufficiency-cases/roomescape/oracles/waiting-rank.md`
- Baseline: `benchmarks/roomescape-jpa-auth-base-v2`
- Isolated target project: `benchmarks/prompt-sufficiency-blind-runs/roomescape-prompt-blind-waiting-rank-ko-l1-001` copied from `/tmp/harness-blind-work/roomescape-prompt-blind-waiting-rank-ko-l1-001`
- Skill: `spring-usecase-implementation`
- Started at: `2026-07-04T15:49:45+09:00`
- Finished at: `2026-07-04T15:55:55+09:00`
- Duration minutes: 6.2
- Result status: `partial`

## Isolation Contract

### Implementation-Visible Context

- Prompt payload: the KO L1 prompt payload pasted below
- Baseline project: sanitized copy of `benchmarks/roomescape-jpa-auth-base-v2`
- Current working directory: `/tmp/harness-blind-work/roomescape-prompt-blind-waiting-rank-ko-l1-001`
- Extra files exposed: none intentionally

### Prohibited Context

- [x] Hidden oracle files
- [x] `benchmarks/prompt-sufficiency-runs`
- [x] Previous blind target projects for the same feature
- [x] Prior run notes or convention comparison rows
- [x] Stable solution target for the same feature
- [x] Representative source copied from another level/language/repeat

### Isolation Verdict

- Isolation status: `pass`

## Prompt

```text
예약 대기 기능을 추가해서 사용자가 대기 순번을 볼 수 있게 해 주세요.
```

## Implementation Outcome

- Implemented: yes
- Asked clarification: no
- Clarification question, if any: not applicable
- Assumptions made:
  - Existing `POST /reservations` creates a confirmed reservation for an open slot and creates a waiting entry when the slot is already reserved.
  - Waiting rank is ordered by same-slot waiting insertion order.
  - `/reservations/mine` can return both reservation and waiting entries using `status` and `rank` fields on `ReservationResponse`.
- Files changed:
  - `src/main/java/roomescape/domain/Waiting.java`
  - `src/main/java/roomescape/repository/WaitingRepository.java`
  - `src/main/java/roomescape/service/ReservationService.java`
  - `src/main/java/roomescape/dto/ReservationResponse.java`
  - `src/main/java/roomescape/exception/ErrorCode.java`
  - `src/main/java/roomescape/exception/ErrorCodeStatusMapper.java`
  - `src/test/java/roomescape/ReservationAcceptanceTest.java`
  - `src/test/java/roomescape/ReservationRepositoryIntegrationTest.java`

## Source Hash And Similarity

- Source hash command:

  ```bash
  find src -type f | sort | xargs shasum -a 256 | shasum -a 256
  ```

- Source hash: `b7455933badc91af09b1e62357f33e3c1d87bfce530eb14b177b2960561f37a7`
- Compared against same-feature blind runs: `roomescape-prompt-blind-waiting-rank-ko-l5-001`, `roomescape-prompt-blind-waiting-rank-ko-l3-001`
- Source similarity verdict: `unique`
- Diff command: `diff -qr benchmarks/prompt-sufficiency-blind-runs/roomescape-prompt-blind-waiting-rank-ko-l3-001/src /tmp/harness-blind-work/roomescape-prompt-blind-waiting-rank-ko-l1-001/src`
- Diff summary: source differs from both L5 and L3. Like L3, L1 integrates waiting creation into `ReservationService.create`; unlike L3, it adds `Waiting.createdAt`, uses response field `rank`, and includes duplicate waiting DB guard integration coverage.

## Verification

### Implementation-Side Verification

- Narrow command: `./gradlew test --tests roomescape.ReservationAcceptanceTest --tests roomescape.ReservationRepositoryIntegrationTest`
- Narrow result: pass
- Full command: `./gradlew clean test`
- Full result: pass

### Verifier-Only Oracle Review

- Oracle satisfied: partially
- Missing oracle requirements:
  - Available-slot waiting rejection is not represented because an open-slot request creates a reservation through `POST /reservations`.
  - Own-reservation waiting rejection is not covered by tests and is handled as duplicate reservation semantics rather than an explicit waiting rule.
- Behavioral regressions: none found by the existing test suite
- Existing behavior preserved: `./gradlew clean test` passed

## Convention Comparison

| Category | Expected | Observed | Status | Severity | Evidence |
| --- | --- | --- | --- | --- | --- |
| verification | Targeted and full Gradle verification are recorded | ReservationAcceptanceTest ReservationRepositoryIntegrationTest and `./gradlew clean test` passed | pass | P1 | `build/test-results/test` |
| prompt_blind_isolation | Implementation did not see verifier-only oracle or prior solutions | Implementation ran from sanitized tmp project and prompt without repo root oracle path run note path or prior waiting-rank targets | pass | P0 | this note |
| source_independence | Source hash/diff does not show copied representative source | L1 source hash differs from L5 and L3 and includes a distinct createdAt field plus ReservationService-integrated waiting design | pass | P0 | source hash and diff |
| oracle_waiting_rank_behavior | Hidden oracle requires occupied-slot waiting available-slot rejection own-reservation rejection duplicate rejection rank my-list and final duplicate guard | Core occupied waiting duplicate rejection rank my-list and final duplicate guard work but available-slot waiting rejection is not represented and own rejection is only duplicate reservation semantics | fail | P1 | `ReservationService.java` |
| rank_strategy | Waiting rank uses request order with deterministic tie-breaker | WaitingRepository counts same-slot waiting rows by generated id order which is deterministic for persisted requests | pass | P1 | `WaitingRepository.java` |
| persistence_guard | Duplicate waiting has a final persistence guard | Waiting has a unique member theme time date constraint and repository integration coverage for duplicate waiting | pass | P1 | `ReservationRepositoryIntegrationTest.java` |
| test_layering | Required waiting-rank tests are present | Tests cover occupied waiting duplicate rank my-list and DB guard but miss available-slot rejection and own-reservation rejection coverage | fail | P1 | `ReservationAcceptanceTest.java` |
| repository_boundary | Repository does not receive request DTOs | WaitingRepository accepts domain values and query-derived parameters not DTOs | pass | P1 | `WaitingRepository.java` |
| service_boundary | Rank calculation stays in service repository not controller | ReservationController delegates and rank is calculated in ReservationService through WaitingRepository | pass | P1 | `ReservationService.java` |

## Skill Changes

- Skill/reference/evaluator/oracle changed: `false`

## Final Verdict

- Matrix status: `partial`
- `runs.csv` result_status: `partial`
- `full_verification_result`: `pass`
- `convention_violations_total`: 2
- Portfolio note: The L1 prompt was sufficient for the obvious waiting/rank flow and duplicate guard, but not for the stricter waiting-command policy boundary. This is prompt-only evidence and should not be merged with the oracle-assisted 84-run pass rate.

## Follow-Up

- Keep: prompt-only isolation, source hashing, final target verification, and matrix recording all worked.
- Fix before next blind run: no remaining planned row in the 12-run Korean pilot.
- Add to prompt checklist: waiting-rank prompts need explicit available-slot waiting rejection and own-reservation waiting rejection if those are required.
- Expand repeats: use the 12-run pilot summary to decide whether to add repeats and English.
- Next benchmark: synthesize the 12-row blind pilot and decide expansion.
