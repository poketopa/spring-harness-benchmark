# Missing Concurrent Test Fixture Failure Case

## Case Metadata

- Case ID: `failure-recovery-missing-concurrent-test-fixture-001`
- Unit: `concurrent-login`
- Failure type: `evaluator_blind_spot`
- Canonical requirement: `missions/roomescape-reservation-waiting/requirements/add-ons/auth-authorization/04-prevent-concurrent-login.md`
- Baseline path: `benchmarks/robustness-runs/roomescape-concurrent-login-policy-001`
- Baseline run ID: `roomescape-failure-missing-concurrent-test-fixture-baseline-001`
- Rerun ID: `roomescape-failure-missing-concurrent-test-fixture-rerun-001`
- Status: `planned`

## Intended Risk

A target fixture can claim concurrent-login support while using only sequential login tests.

The risk is that the harness accepts a pass claim without verifying that a real concurrent test exists for a concurrency-sensitive requirement.

## Expected Good Behavior

The evaluator should fail a fixture that claims concurrent-login pass without a real concurrent test. If the evaluator does not catch it, improve the evaluator before marking the case validated.

## Evaluator Fixture Payload

Use only this section when creating the baseline fixture/evaluator attempt.

Create or inspect a Roomescape concurrent-login target that claims the newest-login-wins policy is implemented and verified.

The target should include tests for repeated login and stale-token rejection, but it intentionally should not include a real concurrent test using multiple threads, latches, executors, or another explicit concurrent execution mechanism.

Run the benchmark evaluator or validator that is supposed to enforce the concurrent-login verification rule. Record whether the fixture is accepted or rejected.

## Evaluation Notes

- Baseline failure category: `evaluator_blind_spot`.
- Passing behavior: the evaluator rejects the fixture because no real concurrent test exists.
- Failing behavior: the evaluator accepts the fixture as pass.
- This case is an evaluator/fixture benchmark, not a normal Spring feature implementation run.
