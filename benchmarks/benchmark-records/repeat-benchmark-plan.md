# Repeat Benchmark Plan

## Objective

Run repeated clean regenerations to measure whether `spring-usecase-implementation` now produces the owner's Spring conventions consistently, not only once.

This document is the handoff for the next session. Do not start repeated runs until the operator explicitly starts that session.

## Fixed Baselines And Requirements

| Benchmark Unit | Baseline | Requirement(s) | Notes |
| --- | --- | --- | --- |
| `c1-waiting-repeat` | `benchmarks/roomescape-jpa-auth-base-v2` | `missions/roomescape-reservation-waiting/requirements/base/02-cycle1-waiting.md` | Waiting should be a peer resource and separate service. |
| `c2-repeat` | `benchmarks/roomescape-jpa-auth-cycle1-regen-v2` | `missions/roomescape-reservation-waiting/requirements/base/03-cycle2-reservation-change-cancel-error.md` and `missions/roomescape-reservation-waiting/requirements/base/04-cycle2-waiting-approval.md` | Implement both cycle2 requirements in one target, matching `roomescape-c2-regen-v2-001`. |
| `manager-authz-repeat` | `benchmarks/roomescape-jpa-auth-cycle2-regen-v2` | `missions/roomescape-reservation-waiting/requirements/add-ons/auth-authorization/03-store-manager-authorization.md` | Use manager authorization conventions from v2. |
| `concurrent-login-repeat` | `benchmarks/roomescape-jpa-auth-manager-authorization-regen-v2` | `missions/roomescape-reservation-waiting/requirements/add-ons/auth-authorization/04-prevent-concurrent-login.md` | Must include stateful auth concurrency handling and a real concurrent test. |

## Repetition Count

Use three attempts per benchmark unit unless the user changes the budget.

Recommended run ids:

- `roomescape-repeat-c1-waiting-001` to `003`
- `roomescape-repeat-c2-001` to `003`
- `roomescape-repeat-manager-authz-001` to `003`
- `roomescape-repeat-concurrent-login-001` to `003`

Recommended target paths:

- `benchmarks/repeat-runs/roomescape-c1-waiting-001`
- `benchmarks/repeat-runs/roomescape-c1-waiting-002`
- `benchmarks/repeat-runs/roomescape-c1-waiting-003`
- `benchmarks/repeat-runs/roomescape-c2-001`
- `benchmarks/repeat-runs/roomescape-c2-002`
- `benchmarks/repeat-runs/roomescape-c2-003`
- `benchmarks/repeat-runs/roomescape-manager-authz-001`
- `benchmarks/repeat-runs/roomescape-manager-authz-002`
- `benchmarks/repeat-runs/roomescape-manager-authz-003`
- `benchmarks/repeat-runs/roomescape-concurrent-login-001`
- `benchmarks/repeat-runs/roomescape-concurrent-login-002`
- `benchmarks/repeat-runs/roomescape-concurrent-login-003`

## Run Protocol

For each repeated run:

1. Record `started_at` before implementation.
2. Copy the fixed baseline into the target path.
3. Use only:
   - the target's current code,
   - the requirement document(s),
   - the current `spring-usecase-implementation` skill/reference files,
   - stable baseline/reference docs explicitly named for that run.
4. Do not inspect earlier failed/review implementations for implementation ideas.
5. Implement the requirement.
6. Run the narrowest relevant tests first.
7. Run `./gradlew clean test`.
8. Record `finished_at`.
9. Fill:
   - `benchmarks/benchmark-records/runs/{run_id}.md`
   - `benchmarks/benchmark-records/runs.csv`
   - `benchmarks/benchmark-records/convention-comparisons.csv`
   - `benchmarks/benchmark-records/skill-updates.csv` only if the skill/reference changed.

## Required Metrics Per Run

Every run must record:

- `run_id`
- `sequence`
- `date`
- `cycle`
- `requirement_path`
- `baseline_path`
- `target_path`
- `started_at`
- `finished_at`
- `duration_min`
- `result_status`
- `full_verification`
- `full_verification_result`
- `convention_violations_total`
- `manual_fix_required`
- `manual_fix_files_count`
- `skill_updated`
- `notes_path`

Do not leave `started_at`, `finished_at`, or `duration_min` blank for repeated benchmark runs.

For repeated benchmark runs, `convention_violations_total` must equal the number of `fail` rows for the same `run_id` in `convention-comparisons.csv`.

## Required Decision Rules By Unit

### `c1-waiting-repeat`

- `service_boundary`
- `domain_naming`
- `rank_strategy`
- `test_layering`
- `java_class_ordering`
- `unused_methods`
- `uri_convention`
- `verification`

### `c2-repeat`

- `service_boundary`
- `promotion_orchestration`
- `read_model_boundary`
- `dto_factory_naming`
- `authentication_boundary`
- `validation_helper_consistency`
- `domain_validation_ownership`
- `repository_query_readability`
- `test_layering`
- `verification`

### `manager-authz-repeat`

- `manager_authorization_boundary`
- `auth_vs_authz_errors`
- `controller_grouping`
- `store_domain_invariant`
- `reservation_manager_scope`
- `test_layering`
- `verification`

### `concurrent-login-repeat`

- `concurrent_login_policy`
- `stateful_auth_concurrency`
- `server_side_session_state`
- `auth_boundary`
- `token_uniqueness`
- `active_session_constraint`
- `real_concurrent_test`
- `token_provider_unit_test`
- `login_member_identity_assertion`
- `verification`

## Pass Criteria

A run is considered convention-stable when:

- `./gradlew clean test` passes.
- `convention_violations_total = 0`.
- `manual_fix_required = false`.
- `skill_updated = false`.

A benchmark unit is considered stable when all three repeated attempts satisfy the run pass criteria.

## Escalation Criteria

Stop the repeated benchmark batch and inspect before continuing if any of these happen:

- The same category fails in two attempts of the same unit.
- A P0 or P1 convention failure appears.
- `./gradlew clean test` fails and the failure is not a simple test-environment issue.
- A new skill/reference update is required.

If a skill update is required, record it as a correction run first, then add a new validation run before resuming batch repetition.

## Final Outputs After Repetition

At the end of the repeated benchmark batch, produce:

- updated `benchmarks/benchmark-records/summary.md`
- a repeat result matrix grouped by benchmark unit and attempt
- a list of stable vs unstable decision rules
- skill update validation state
- recommendation for the portfolio application target
