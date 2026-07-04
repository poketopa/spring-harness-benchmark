# Concurrent Waiting Race Failure Case

## Case Metadata

- Case ID: `failure-recovery-concurrent-waiting-race-001`
- Unit: `c1-waiting`
- Failure type: `concurrency_weakness`
- Canonical requirement: `missions/roomescape-reservation-waiting/requirements/base/02-cycle1-waiting.md`
- Baseline path: `benchmarks/roomescape-jpa-auth-base-v2`
- Baseline run ID: `roomescape-failure-concurrent-waiting-race-baseline-001`
- Rerun ID: `roomescape-failure-concurrent-waiting-race-rerun-001`
- Status: `planned`

## Intended Risk

The normal waiting requirement can pass sequential tests while still allowing duplicate waiting entries or broken rank state under concurrent requests.

The risk is that the skill relies only on service-level existence checks and sequential tests.

## Expected Good Behavior

The implementation should include a final database or locking guard for the same-member same-slot invariant and a real concurrent test that can expose the race.

## Implementation Prompt Payload

Use only this section as the implementation prompt for the baseline and rerun attempts.

Add reservation waiting to the existing Roomescape reservation behavior.

Users can apply for waiting on a date/time/theme slot that is already reserved. Waiting entries are ordered by application time for the same slot, and the user's current rank for that slot must be returned. The same user cannot have duplicate waiting entries for the same slot.

The duplicate-waiting rule must hold even when two requests for the same user and same slot arrive at nearly the same time. Sequential existence checks alone are not enough if concurrent requests can create duplicate rows.

Users can cancel their own waiting entries. The user's reservation list should include both confirmed reservations and waiting entries, with status and waiting rank. Waiting ranks for a slot must remain consistent after cancellation.

Add tests for waiting creation, duplicate waiting rejection, own waiting cancellation, combined own reservation/waiting read model, rank recalculation, and a real concurrent duplicate-waiting scenario.

## Evaluation Notes

- Baseline failure category: `concurrency_weakness`.
- Passing behavior: the run includes both a final concurrency guard and a real concurrent test.
- Failing behavior: the run uses only sequential checks or only sequential tests for the duplicate waiting invariant.
- If implementation cannot complete, record whether the blocker is design ambiguity, verification failure, or architecture drift.
