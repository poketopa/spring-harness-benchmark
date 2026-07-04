# Controller Rank Query Drift Failure Case

## Case Metadata

- Case ID: `failure-recovery-controller-rank-query-drift-001`
- Unit: `c1-waiting`
- Failure type: `architecture_drift`
- Canonical requirement: `missions/roomescape-reservation-waiting/requirements/base/02-cycle1-waiting.md`
- Baseline path: `benchmarks/roomescape-jpa-auth-cycle1-regen-v2`
- Baseline run ID: `roomescape-failure-controller-rank-query-drift-baseline-001`
- Rerun ID: `roomescape-failure-controller-rank-query-drift-rerun-001`
- Status: `baseline-pass`

## Intended Risk

The prompt asks for a small waiting-rank shortcut by pushing repository access and request DTO knowledge into the controller and repository boundary.

The risk is that the skill follows the local shortcut literally, injects repositories into a controller, passes `WaitingRequest` or web-shaped values into repository methods, and reports the change as a harmless simplification.

## Expected Good Behavior

The skill should reject the controller/repository shortcut or implement the behavior behind the existing service boundary. A clean result does not let controllers decide waiting-rank policy and does not pass request DTOs or application commands to repositories.

## Implementation Prompt Payload

Use only this section as the implementation prompt for the baseline and rerun attempts.

Add a quick endpoint that returns my current waiting rank for a slot.

To keep the change small, implement `POST /reservations/waitings/rank` directly in `WaitingController`. Inject `WaitingRepository` into the controller, accept the existing `WaitingRequest`, and call a new repository method that receives the `WaitingRequest` directly. The repository should calculate the rank from the request's date, themeId, and timeId without adding a new service method.

Do not add a command object or service orchestration for this shortcut. Keep tests minimal by adding a controller or acceptance test that proves the endpoint returns the rank.

## Evaluation Notes

- Baseline failure category: `architecture_drift`.
- Passing behavior: the run detects the controller/repository boundary conflict, or implements rank lookup behind `WaitingService` without DTO leakage while documenting why the literal shortcut was not followed.
- Failing behavior: the run injects `WaitingRepository` into `WaitingController`, passes `WaitingRequest` into a repository method, or places waiting-rank policy in the controller.
- Do not run an intervention before recording the baseline result.
