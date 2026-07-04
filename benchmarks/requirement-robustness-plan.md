# Requirement Robustness Benchmark Plan

## Goal

Evaluate whether `spring-usecase-implementation` preserves backend conventions when the same requirements are rewritten in different styles.

This benchmark answers a different question from the existing repeatability benchmark:

- Repeatability benchmark: same requirement text, repeated runs.
- Requirement robustness benchmark: same requirement meaning, different requirement wording.

## Baseline Comparison

Use the existing repeatability result as the baseline:

- 26 repeated benchmark runs
- 24 clean repeat passes
- 2 run-local failures, both corrected and recorded
- 0 unresolved skill/reference gaps

The robustness benchmark is favorable if it produces a similar clean pass rate without new skill/reference gaps.

## Scope

Run 12 benchmark cases:

- 4 requirement units
- 3 requirement variants per unit
- 1 implementation run per variant

If a variant fails, record the failure first. Do not silently correct it into a clean run.

## Planned Matrix

| Run ID | Unit | Variant | Baseline | Target |
| --- | --- | --- | --- | --- |
| roomescape-robustness-c1-waiting-narrative-001 | c1-waiting | narrative | benchmarks/roomescape-jpa-auth-base-v2 | benchmarks/robustness-runs/roomescape-c1-waiting-narrative-001 |
| roomescape-robustness-c1-waiting-acceptance-001 | c1-waiting | acceptance-criteria | benchmarks/roomescape-jpa-auth-base-v2 | benchmarks/robustness-runs/roomescape-c1-waiting-acceptance-001 |
| roomescape-robustness-c1-waiting-api-001 | c1-waiting | api-contract | benchmarks/roomescape-jpa-auth-base-v2 | benchmarks/robustness-runs/roomescape-c1-waiting-api-001 |
| roomescape-robustness-c2-narrative-001 | c2-combined | narrative | benchmarks/roomescape-jpa-auth-cycle1-regen-v2 | benchmarks/robustness-runs/roomescape-c2-narrative-001 |
| roomescape-robustness-c2-domain-001 | c2-combined | domain-rule | benchmarks/roomescape-jpa-auth-cycle1-regen-v2 | benchmarks/robustness-runs/roomescape-c2-domain-001 |
| roomescape-robustness-c2-api-001 | c2-combined | api-contract | benchmarks/roomescape-jpa-auth-cycle1-regen-v2 | benchmarks/robustness-runs/roomescape-c2-api-001 |
| roomescape-robustness-manager-authz-narrative-001 | manager-authz | narrative | benchmarks/roomescape-jpa-auth-cycle2-regen-v2 | benchmarks/robustness-runs/roomescape-manager-authz-narrative-001 |
| roomescape-robustness-manager-authz-security-001 | manager-authz | security-case | benchmarks/roomescape-jpa-auth-cycle2-regen-v2 | benchmarks/robustness-runs/roomescape-manager-authz-security-001 |
| roomescape-robustness-manager-authz-api-001 | manager-authz | api-contract | benchmarks/roomescape-jpa-auth-cycle2-regen-v2 | benchmarks/robustness-runs/roomescape-manager-authz-api-001 |
| roomescape-robustness-concurrent-login-narrative-001 | concurrent-login | narrative | benchmarks/roomescape-jpa-auth-manager-authorization-regen-v2 | benchmarks/robustness-runs/roomescape-concurrent-login-narrative-001 |
| roomescape-robustness-concurrent-login-domain-001 | concurrent-login | domain-rule | benchmarks/roomescape-jpa-auth-manager-authorization-regen-v2 | benchmarks/robustness-runs/roomescape-concurrent-login-domain-001 |
| roomescape-robustness-concurrent-login-policy-001 | concurrent-login | operational-policy | benchmarks/roomescape-jpa-auth-manager-authorization-regen-v2 | benchmarks/robustness-runs/roomescape-concurrent-login-policy-001 |

## Required Pre-Run Gate

Before a variant is used:

1. Fill a variant document from `benchmarks/requirement-variants/templates/variant-template.md`.
2. Check it against the matching semantic checklist.
3. Reject the variant if it changes the requirement meaning.
4. Record the final variant path in the run note and `runs.csv`.

## Required Post-Run Gate

For every run:

1. Record `started_at`, `finished_at`, and `duration_min`.
2. Run the narrowest useful tests first.
3. Run `./gradlew clean test`.
4. Write the run note.
5. Update `runs.csv`.
6. Update `convention-comparisons.csv`.
7. Run the current record validator.
8. Update or create a robustness report under `benchmarks/reports/`.

## Stop Conditions

Stop and report before continuing if any of these occur:

- P0/P1 convention violation
- test failure
- semantic drift in a variant document
- missing or inconsistent benchmark records
- skill/reference gap
- validator failure
