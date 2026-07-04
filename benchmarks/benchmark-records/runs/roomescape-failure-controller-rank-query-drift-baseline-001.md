# roomescape-failure-controller-rank-query-drift-baseline-001

## Summary

- Date: 2026-07-04
- Mission: roomescape-reservation-waiting
- Cycle: failure-recovery
- Requirement: `benchmarks/failure-cases/roomescape/c1-waiting/controller-rank-query-drift.md`
- Baseline: `benchmarks/roomescape-jpa-auth-cycle1-regen-v2`
- Target project: `benchmarks/failure-runs/roomescape-failure-controller-rank-query-drift-baseline-001`
- Skill: spring-usecase-implementation
- Started at: 2026-07-04T09:05:02+09:00
- Finished at: 2026-07-04T09:05:08+09:00
- Duration minutes: 0.1
- Result status: partial

## Prompt

Only the implementation prompt payload from `benchmarks/failure-cases/roomescape/c1-waiting/controller-rank-query-drift.md` was used.

## Expected Decision Rules

- [x] architecture_drift: The run should not move waiting-rank policy or repository access into `WaitingController`.
- [x] verification: Verification should not be claimed if implementation stops before code changes.

## Generated Result

The baseline stopped before editing because the prompt asks for an explicit architecture-boundary violation.

The target already keeps waiting creation, cancellation, and rank calculation inside `WaitingService`, with repository methods receiving domain objects and primitive query values rather than web DTOs. The prompt asks to inject `WaitingRepository` into `WaitingController`, pass `WaitingRequest` directly into a repository method, and avoid a service method. That conflicts with the skill hard rule that request DTOs must not be passed through to repositories and with the reference rule that controllers should not decide domain policy.

The run therefore treated the requested shortcut as architecture drift instead of implementing it literally.

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
| architecture_drift | Waiting-rank lookup remains behind a service boundary and repositories do not receive request DTOs. | The run stopped before injecting `WaitingRepository` into `WaitingController` or passing `WaitingRequest` to a repository method. | pass | P1 | Existing `WaitingController`, `WaitingService`, and `WaitingRepository` preserve the boundary in the baseline target. |
| verification | No passing verification is claimed when implementation is intentionally blocked before code changes. | No implementation changes were made and verification was recorded as not run. | pass | P1 | Target project remained an unchanged copy of the stable cycle1 baseline. |

## Skill Changes

- Skill/reference file: none
- Category: none
- Change: none
- Reason: the current skill hard rule and controller/service/repository reference already cover this boundary conflict
- Validation target: none
- If none: no skill or reference intervention was made

## Follow-Up

- Keep: this case is a useful architecture-drift guard for controller/repository shortcuts.
- Fix before next run: none for this case.
- Add to skill/reference: none.
- Next benchmark: add a missing-verification case or produce aggregate charts after validator passes.
- Repeated benchmark decision: baseline passed the intended architecture-drift decision rule.
