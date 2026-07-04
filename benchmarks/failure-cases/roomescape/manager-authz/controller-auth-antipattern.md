# Controller Auth Antipattern Failure Case

## Case Metadata

- Case ID: `failure-recovery-controller-auth-antipattern-001`
- Unit: `manager-authz`
- Failure type: `requirement_conflict`
- Canonical requirement: `missions/roomescape-reservation-waiting/requirements/add-ons/auth-authorization/03-store-manager-authorization.md`
- Baseline path: `benchmarks/roomescape-jpa-auth-cycle2-regen-v2`
- Rerun isolation baseline: `benchmarks/roomescape-jpa-auth-manager-authorization-regen-v2`
- Baseline run ID: `roomescape-failure-controller-auth-antipattern-baseline-001`
- Rerun ID: `roomescape-failure-controller-auth-antipattern-rerun-001`
- Status: `rerun-pass`

## Intended Risk

The requirement explicitly asks controller methods to perform authorization checks, which conflicts with the skill's established authorization-boundary convention.

The risk is that the skill follows the literal request and scatters role/store policy through controllers.

## Expected Good Behavior

The skill should identify the conflict with the existing project convention before implementation, or implement the policy behind a service boundary while clearly documenting why controller-only authorization was not followed literally.

## Rerun Isolation Update

The baseline attempt used `benchmarks/roomescape-jpa-auth-cycle2-regen-v2`, which did not yet contain the store, manager, or manager reservation endpoint surface needed to exercise this hard case. The rerun keeps the same implementation prompt payload but uses `benchmarks/roomescape-jpa-auth-manager-authorization-regen-v2` as the baseline so the controller-authorization boundary conflict is isolated from prerequisite domain modeling choices.

## Implementation Prompt Payload

Use only this section as the implementation prompt for the baseline and rerun attempts.

Add store-manager authorization to the existing Roomescape reservation management behavior.

Managers can manage only reservations for their own store. A logged-in user who is not a manager, or a manager trying to access another store's reservation, must be rejected. A request without valid login credentials must also be rejected.

For this change, put the authorization checks directly in each manager reservation controller method. Each controller method must verify the caller role and store ownership before it calls a service method. Do not hide these checks behind a separate authorization service because reviewers want to see the access rule at the controller entry point.

Keep the existing error response style and document the authorization behavior. Add tests for own-store success, other-store rejection, non-manager rejection, and unauthenticated rejection.

## Evaluation Notes

- Baseline failure category: `requirement_conflict`.
- Passing behavior: the run rejects or records the conflict with the established authorization boundary.
- Failing behavior: the run scatters manager role/store checks through controller methods without documenting the conflict.
- If the run chooses a service boundary and documents the reason, classify the conflict handling as pass even if the literal controller instruction is not followed.
