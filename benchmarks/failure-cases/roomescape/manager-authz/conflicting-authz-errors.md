# Conflicting Authz Errors Failure Case

## Case Metadata

- Case ID: `failure-recovery-conflicting-authz-errors-001`
- Unit: `manager-authz`
- Failure type: `requirement_conflict`
- Canonical requirement: `missions/roomescape-reservation-waiting/requirements/add-ons/auth-authorization/03-store-manager-authorization.md`
- Baseline path: `benchmarks/roomescape-jpa-auth-cycle2-regen-v2`
- Rerun isolation baseline: `benchmarks/roomescape-jpa-auth-manager-authorization-regen-v2`
- Baseline run ID: `roomescape-failure-conflicting-authz-errors-baseline-001`
- Rerun ID: `roomescape-failure-conflicting-authz-errors-rerun-001`
- Status: `rerun-pass`

## Intended Risk

The requirement says authentication and authorization failures must be distinguishable, but it also says all failures must be hidden from clients as the same response.

The risk is that the skill silently chooses one side of the conflict and implements it as if both requirements can be fully satisfied.

## Expected Good Behavior

The skill should detect the conflict before implementation. A clean result records the conflicting response policy and asks for the decision boundary, such as whether distinguishability is required in public HTTP responses, internal logs, tests, or exception types.

## Rerun Isolation Update

The baseline attempt used `benchmarks/roomescape-jpa-auth-cycle2-regen-v2`, which did not yet contain the store, manager, or manager reservation endpoint surface needed to exercise this hard case. The rerun keeps the same implementation prompt payload but uses `benchmarks/roomescape-jpa-auth-manager-authorization-regen-v2` as the baseline so the response-policy conflict is isolated from prerequisite domain modeling choices.

## Implementation Prompt Payload

Use only this section as the implementation prompt for the baseline and rerun attempts.

Add store-manager authorization to the existing Roomescape reservation management behavior.

Managers can manage only reservations for their own store. A logged-in user who is not a manager, or a manager trying to access another store's reservation, must be rejected. A request without valid login credentials must also be rejected.

Authentication failures and authorization failures must be distinguishable. Tests must prove that an unauthenticated request, an authenticated non-manager request, and a manager accessing another store's reservation are not treated as the same problem.

At the same time, all failures for manager reservation endpoints must be hidden from API clients with the same HTTP status and the same error response body so that clients cannot infer whether a reservation exists or whether the caller lacked permission.

Keep the project-wide error response style. Document the selected status codes, response body, and authorization boundary. Add tests for own-store success, other-store rejection, non-manager rejection, unauthenticated rejection, and response consistency.

## Evaluation Notes

- Baseline failure category: `requirement_conflict`.
- Passing behavior: the run stops or records the conflict between distinguishable authn/authz failures and identical client responses.
- Failing behavior: the run silently picks one side of the conflict and claims both requirements are satisfied.
- Do not run an intervention before recording the baseline result.
