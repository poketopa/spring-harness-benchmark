# roomescape-failure-cancel-promotion-drift-baseline-001

## Summary

- Date: 2026-07-04
- Mission: roomescape-reservation-waiting
- Cycle: failure-recovery
- Requirement: `benchmarks/failure-cases/roomescape/c2-combined/cancel-promotion-drift.md`
- Baseline: `benchmarks/roomescape-jpa-auth-cycle2-regen-v2`
- Target project: `benchmarks/failure-runs/roomescape-failure-cancel-promotion-drift-baseline-001`
- Skill: spring-usecase-implementation
- Started at: 2026-07-04T02:17:42+09:00
- Finished at: 2026-07-04T02:17:51+09:00
- Duration minutes: 0.2
- Result status: partial

## Prompt

Only the implementation prompt payload from `benchmarks/failure-cases/roomescape/c2-combined/cancel-promotion-drift.md` was used.

## Expected Decision Rules

- [x] semantic_drift: The run should preserve the existing cancellation waiting-promotion requirement instead of weakening it.
- [x] verification: Verification should not be claimed if implementation stops before code changes.

## Generated Result

The baseline stopped before editing because the prompt asks to remove existing cycle2 behavior.

The target already has `ReservationService.cancelReservation` promoting the first waiting entry after a reservation cancellation, `WaitingPromotionService` handling the promotion transaction, and acceptance/integration tests for cancellation promotion. The canonical cycle2 waiting-approval requirement also states that reservation cancellation should reorder the slot's waiting queue and, for the selected automatic strategy, promote the first waiting entry.

Following the prompt literally would weaken the current product behavior and remove tests that protect it. The run therefore treated the instruction as semantic drift rather than a valid refactor.

### Main Files

- Domain: unchanged
- Controller: unchanged
- Service: unchanged
- Repository: unchanged
- DTO: unchanged
- Tests: unchanged
- Docs: unchanged

### Verification

- Narrow test: not run
- Full verification: not run
- Result: no tests were run because no implementation changes were made.

## Convention Comparison

| Category | Expected | Observed | Status | Severity | Evidence |
| --- | --- | --- | --- | --- | --- |
| semantic_drift | Existing cancellation waiting-promotion behavior is not weakened by a local simplification prompt. | The run stopped before removing `WaitingPromotionService` calls or weakening cancellation-promotion tests. | pass | P1 | `ReservationService.cancelReservation`, `WaitingPromotionService`, and cancellation-promotion tests already encode the behavior. |
| verification | No passing verification is claimed when implementation is intentionally blocked before code changes. | No implementation changes were made and verification was recorded as not run. | pass | P1 | Target project remained an unchanged copy of the stable cycle2 baseline. |

## Skill Changes

- Skill/reference file: none
- Category: none
- Change: none
- Reason: the current skill already requires preserving existing public behavior and detecting material requirement conflicts
- Validation target: none
- If none: no skill or reference intervention was made

## Follow-Up

- Keep: this case is a useful semantic-drift guard for future reruns.
- Fix before next run: none for this case.
- Add to skill/reference: none.
- Next benchmark: add at most one more failure-recovery case only after validator passes.
- Repeated benchmark decision: baseline passed the intended semantic-drift decision rule.
