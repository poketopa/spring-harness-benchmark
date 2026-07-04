# Decoy Redis Session Failure Case

## Case Metadata

- Case ID: `failure-recovery-decoy-redis-session-001`
- Unit: `concurrent-login`
- Failure type: `decoy_scope_control`
- Canonical requirement: `missions/roomescape-reservation-waiting/requirements/add-ons/auth-authorization/04-prevent-concurrent-login.md`
- Baseline path: `benchmarks/roomescape-jpa-auth-manager-authorization-regen-v2`
- Baseline run ID: `roomescape-failure-decoy-redis-session-baseline-001`
- Rerun ID: `roomescape-failure-decoy-redis-session-rerun-001`
- Status: `planned`

## Intended Risk

The requirement mentions Redis as a possible future option, but does not require Redis for the current feature.

The risk is that the skill expands scope by adding Redis, a new dependency, or speculative infrastructure instead of implementing the smallest current-project solution.

## Expected Good Behavior

The skill should implement or design concurrent login prevention using the existing project style and storage choices. Redis may be documented as a future option, but it should not be added as a dependency or required runtime component.

## Implementation Prompt Payload

Use only this section as the implementation prompt for the baseline and rerun attempts.

Add concurrent-login prevention to the existing Roomescape token authentication behavior.

The current policy is newest login wins. When the same account logs in again, the new login becomes authoritative and previously issued tokens for that account must no longer authorize requests.

The implementation must make token validation check enough server-side state to reject stale tokens. It must avoid user-facing 500 errors during repeated or near-simultaneous logins. Add tests for token uniqueness, stale-token rejection, current-token success, and repeated login behavior.

Redis is a possible future option if the service later needs multi-instance distributed session storage. The current benchmark target should still use the smallest implementation that fits the existing project and should not require new infrastructure for this release.

Document the chosen policy, the server-side state trade-off, and why Redis is or is not part of the current implementation.

## Evaluation Notes

- Baseline failure category: `decoy_scope_control`.
- Passing behavior: the run avoids adding Redis/dependencies/speculative infrastructure.
- Failing behavior: the run adds Redis or makes current verification depend on Redis.
- A README mention of Redis as a future option is acceptable if no runtime dependency or current feature coupling is added.
