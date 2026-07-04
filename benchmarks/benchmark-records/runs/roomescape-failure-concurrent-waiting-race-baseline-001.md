# roomescape-failure-concurrent-waiting-race-baseline-001

## Summary

- Date: 2026-07-04
- Mission: roomescape-reservation-waiting
- Cycle: failure-recovery
- Requirement: `benchmarks/failure-cases/roomescape/c1-waiting/concurrent-waiting-race.md`
- Baseline: `benchmarks/roomescape-jpa-auth-base-v2`
- Target project: `benchmarks/failure-runs/roomescape-failure-concurrent-waiting-race-baseline-001`
- Skill: spring-usecase-implementation
- Started at: 2026-07-04T01:47:53+09:00
- Finished at: 2026-07-04T01:53:38+09:00
- Duration minutes: 5.8
- Result status: pass

## Prompt

Only the implementation prompt payload from `benchmarks/failure-cases/roomescape/c1-waiting/concurrent-waiting-race.md` was used.

## Expected Decision Rules

- [x] concurrency_weakness: Duplicate waiting prevention has a final concurrency guard and not only sequential service checks.
- [x] real_concurrent_test: A real concurrent duplicate-waiting test exists.
- [x] verification: Narrow and full tests pass.

## Generated Result

The baseline implemented reservation waiting with rank calculation, duplicate waiting rejection, own waiting cancellation, combined own reservation/waiting read model, rank recalculation, and a real concurrent duplicate-waiting acceptance test.

### Main Files

- Domain: `src/main/java/roomescape/domain/Waiting.java`
- Controller: `src/main/java/roomescape/controller/ReservationController.java`
- Service: `src/main/java/roomescape/service/WaitingService.java`, `src/main/java/roomescape/service/ReservationService.java`
- Repository: `src/main/java/roomescape/repository/WaitingRepository.java`
- DTO: `src/main/java/roomescape/dto/ReservationResponse.java`
- Tests: `src/test/java/roomescape/WaitingAcceptanceTest.java`, `src/test/java/roomescape/WaitingRepositoryIntegrationTest.java`
- Docs: unchanged

### Verification

- Narrow test: `./gradlew test --tests roomescape.WaitingAcceptanceTest --tests roomescape.WaitingRepositoryIntegrationTest`
- Full verification: `./gradlew test`
- Result: pass. Parsed test XML showed 12 tests, 0 failures, 0 errors, 0 skipped.

## Convention Comparison

| Category | Expected | Observed | Status | Severity | Evidence |
| --- | --- | --- | --- | --- | --- |
| concurrency_weakness | Duplicate waiting prevention has a final concurrency guard. | `Waiting` uses a same member/date/time/theme unique constraint. | pass | P1 | `src/main/java/roomescape/domain/Waiting.java` |
| real_concurrent_test | A real concurrent duplicate-waiting test exists. | `WaitingAcceptanceTest` uses `ExecutorService` and `CountDownLatch`. | pass | P1 | `src/test/java/roomescape/WaitingAcceptanceTest.java` |
| verification | Narrow and full verification pass. | Targeted waiting tests and full `./gradlew test` passed. | pass | P1 | Gradle XML summary recorded 12 tests and 0 failures. |

## Skill Changes

- Skill/reference file: none
- Category: none
- Change: none
- Reason: baseline passed the concurrency weakness decision rules
- Validation target: none
- If none: no intervention or rerun is required for this case at this time

## Follow-Up

- Keep: The implementation included both a DB guard and a real concurrent test.
- Fix before next run: none for this case.
- Add to skill/reference: none.
- Next benchmark: proceed to another failure-recovery case.
- Repeated benchmark decision: baseline outcome is a pass for `concurrency_weakness`; rerun is skipped because no intervention was needed.
