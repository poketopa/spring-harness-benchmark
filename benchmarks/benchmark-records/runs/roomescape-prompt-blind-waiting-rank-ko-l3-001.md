# roomescape-prompt-blind-waiting-rank-ko-l3-001

## Summary

- Date: 2026-07-04
- Mission: `roomescape-reservation-waiting`
- Cycle: `prompt-sufficiency-blind`
- Feature: `waiting-rank`
- Prompt language: `ko`
- Prompt level: `L3`
- Repeat: `001`
- Prompt path: `benchmarks/prompt-sufficiency-cases/roomescape/waiting-rank/ko/l3.md`
- Hidden oracle path: `benchmarks/prompt-sufficiency-cases/roomescape/oracles/waiting-rank.md`
- Baseline: `benchmarks/roomescape-jpa-auth-base-v2`
- Isolated target project: `benchmarks/prompt-sufficiency-blind-runs/roomescape-prompt-blind-waiting-rank-ko-l3-001` copied from `/tmp/harness-blind-work/roomescape-prompt-blind-waiting-rank-ko-l3-001`
- Skill: `spring-usecase-implementation`
- Started at: `2026-07-04T15:38:43+09:00`
- Finished at: `2026-07-04T15:45:27+09:00`
- Duration minutes: 6.7
- Result status: `partial`

## Isolation Contract

### Implementation-Visible Context

- Prompt payload: the KO L3 prompt payload pasted below
- Baseline project: sanitized copy of `benchmarks/roomescape-jpa-auth-base-v2`
- Current working directory: `/tmp/harness-blind-work/roomescape-prompt-blind-waiting-rank-ko-l3-001`
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
- If failed, explain and mark the matrix row `invalid`:

## Prompt

```text
예약이 꽉 찬 시간에는 대기를 신청할 수 있게 하고, 내가 몇 번째 대기자인지 보여 주세요. 중복 대기는 막고 테스트도 추가해 주세요.
```

## Implementation Outcome

- Implemented: yes
- Asked clarification: no
- Clarification question, if any: not applicable
- Assumptions made:
  - Existing `POST /reservations` creates a confirmed reservation for an open slot and creates a waiting entry only when the slot is already reserved.
  - Waiting rank is ordered by generated waiting id.
  - `/reservations/mine` can return both reservation and waiting entries using `status` and `waitingRank` fields on `ReservationResponse`.
- Files changed:
  - `src/main/java/roomescape/domain/Waiting.java`
  - `src/main/java/roomescape/repository/WaitingRepository.java`
  - `src/main/java/roomescape/service/ReservationService.java`
  - `src/main/java/roomescape/dto/ReservationResponse.java`
  - `src/main/java/roomescape/exception/ErrorCode.java`
  - `src/main/java/roomescape/exception/ErrorCodeStatusMapper.java`
  - `src/test/java/roomescape/ReservationAcceptanceTest.java`

## Source Hash And Similarity

- Source hash command:

  ```bash
  find src -type f | sort | xargs shasum -a 256 | shasum -a 256
  ```

- Source hash: `2007d27f987e2b5dc22f9c2e7bb09672e33f81768e36a1a659075e4036bc6b7d`
- Compared against same-feature blind runs: `roomescape-prompt-blind-waiting-rank-ko-l5-001`
- Source similarity verdict: `unique`
- Diff command: `diff -qr benchmarks/prompt-sufficiency-blind-runs/roomescape-prompt-blind-waiting-rank-ko-l5-001/src /tmp/harness-blind-work/roomescape-prompt-blind-waiting-rank-ko-l3-001/src`
- Diff summary: source differs from L5. L3 has no `WaitingController`, `WaitingService`, `WaitingRequest`, or `WaitingResponse`; it integrates waiting creation into `ReservationService.create` and expands `ReservationResponse`.

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
  - Duplicate waiting final guard is implemented with a unique constraint and `saveAndFlush` catch but no waiting-specific repository or race-sensitive test covers it.
- Behavioral regressions: none found by the existing test suite
- Existing behavior preserved: `./gradlew clean test` passed

## Convention Comparison

| Category | Expected | Observed | Status | Severity | Evidence |
| --- | --- | --- | --- | --- | --- |
| verification | Targeted and full Gradle verification are recorded | ReservationAcceptanceTest ReservationRepositoryIntegrationTest and `./gradlew clean test` passed | pass | P1 | `build/test-results/test` |
| prompt_blind_isolation | Implementation did not see verifier-only oracle or prior solutions | Implementation ran from sanitized tmp project and prompt without repo root oracle path run note path or prior waiting-rank targets | pass | P0 | this note |
| source_independence | Source hash/diff does not show copied representative source | L3 source hash differs from L5 and uses a distinct ReservationService-integrated waiting design | pass | P0 | source hash and diff |
| oracle_waiting_rank_behavior | Hidden oracle requires occupied-slot waiting available-slot rejection own-reservation rejection duplicate rejection rank my-list and final duplicate guard | Core occupied waiting duplicate rejection rank and my-list work but available-slot waiting rejection is not represented and own rejection is only duplicate reservation semantics | fail | P1 | `ReservationService.java` |
| rank_strategy | Waiting rank uses request order with deterministic tie-breaker | WaitingRepository counts same-slot waiting rows by generated id order which is deterministic for persisted requests | pass | P1 | `WaitingRepository.java` |
| persistence_guard | Duplicate waiting has a final persistence guard | Waiting has a unique member theme time date constraint and ReservationService catches saveAndFlush integrity violations | pass | P1 | `Waiting.java` |
| test_layering | Required waiting-rank tests are present | Tests cover occupied waiting duplicate rank and my-list but miss available-slot rejection own-reservation rejection and waiting duplicate final-guard coverage | fail | P1 | `ReservationAcceptanceTest.java` |
| repository_boundary | Repository does not receive request DTOs | WaitingRepository accepts domain values and query-derived parameters not DTOs | pass | P1 | `WaitingRepository.java` |
| service_boundary | Rank calculation stays in service repository not controller | ReservationController delegates and rank is calculated in ReservationService through WaitingRepository | pass | P1 | `ReservationService.java` |

## Skill Changes

- Skill/reference/evaluator/oracle changed: `false`
- If changed, explain why the run is invalid or why record integrity required it:

## Final Verdict

- Matrix status: `partial`
- `runs.csv` result_status: `partial`
- `full_verification_result`: `pass`
- `convention_violations_total`: 2
- Portfolio note: The L3 prompt was enough for core waiting and rank behavior but not enough to force the stricter waiting command boundary or full oracle test set. This is prompt-only evidence and should not be merged with the oracle-assisted 84-run pass rate.

## Follow-Up

- Keep: prompt-only isolation and source hashing worked; core waiting/rank behavior emerged from L3.
- Fix before next blind run: no harness fix required.
- Add to prompt checklist: waiting-rank prompts need explicit available-slot waiting rejection and own-reservation rejection if those are required product policy.
- Expand repeats: after L1 completes and validator remains clean.
- Next benchmark: `roomescape-prompt-blind-waiting-rank-ko-l1-001`
