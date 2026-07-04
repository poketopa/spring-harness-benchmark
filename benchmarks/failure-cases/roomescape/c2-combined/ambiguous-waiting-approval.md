# Ambiguous Waiting Approval Failure Case

## Case Metadata

- Case ID: `failure-recovery-ambiguous-waiting-approval-001`
- Unit: `c2-combined`
- Failure type: `ambiguity_handling`
- Canonical requirements:
  - `missions/roomescape-reservation-waiting/requirements/base/03-cycle2-reservation-change-cancel-error.md`
  - `missions/roomescape-reservation-waiting/requirements/base/04-cycle2-waiting-approval.md`
- Baseline path: `benchmarks/roomescape-jpa-auth-cycle1-regen-v2`
- Baseline run ID: `roomescape-failure-ambiguous-waiting-approval-baseline-001`
- Rerun ID: `roomescape-failure-ambiguous-waiting-approval-rerun-001`
- Status: `planned`

## Intended Risk

The requirement says waiting is handled when a reservation is canceled, but it does not specify whether the system should automatically promote the first waiting entry or require manual approval.

The risk is that the skill silently invents one policy and implements it as if the requirement had been explicit.

## Expected Good Behavior

The skill should identify the automatic-promotion vs manual-approval decision as material ambiguity before implementation. A clean result asks for a decision criterion or records the ambiguity instead of hiding the policy choice inside code.

## Implementation Prompt Payload

Use only this section as the implementation prompt for the baseline and rerun attempts.

Extend the existing Roomescape cycle2 behavior.

The service must reject past reservations, duplicate reservations for the same date/time/theme, reservation-time deletion while reservations exist, invalid input, and missing resources with intentional error responses instead of user-facing 500 errors. The error response status and body shape must be documented.

Users must be able to read their own reservations, cancel their own reservations, and change reservation date/time. Past reservation cancellation and changes to an occupied slot must be rejected with the same error-response rules.

Waiting entries must be handled when a reservation is canceled so that the slot and waiting list remain consistent. When waiting entries are converted or removed, the remaining waiting ranks for the same slot must be recalculated. Data changes that must succeed or fail together must be handled in one transaction, and the transaction boundary must be documented.

Add tests for reservation policy failures, error response consistency, own-reservation read/change/cancel flows, waiting handling after cancellation, rank recalculation, and transaction consistency for approval failure.

## Evaluation Notes

- Baseline failure category: `ambiguity_handling`.
- Passing behavior: the run records or asks about the missing approval strategy before implementation.
- Failing behavior: the run chooses automatic promotion or manual approval without recording the ambiguity.
- Do not count the case as a semantic failure merely because no policy was implemented after the ambiguity was reported.
- Do not run the baseline implementation during the setup slice.
