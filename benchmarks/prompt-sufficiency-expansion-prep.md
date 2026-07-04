# Prompt Sufficiency Expansion Preparation

## Purpose

This document defines what must be written before starting the next prompt-sufficiency experiment.

The completed data has two separate meanings:

- The 84-run prompt-sufficiency baseline is oracle-assisted harness evidence.
- The 12-run Korean prompt-only blind pilot is prompt-only evidence under stricter isolation.

Do not merge these claims. The next experiment should begin only after the blind pilot evidence has been synthesized into portfolio-safe conclusions.

## Current Result

The Korean prompt-only blind pilot is complete and validator-clean.

| Metric | Value |
| --- | ---: |
| Blind rows | 12 |
| Pass | 4 |
| Clarification needed | 1 |
| Partial | 6 |
| Fail | 1 |
| Invalid | 0 |

Feature-level summary:

| Feature | L5 | L3 | L1 | Boundary signal |
| --- | --- | --- | --- | --- |
| `cancel-waiting` | pass | partial | partial | Weaker prompts inferred promotion but missed past-cancel rejection and rollback-test evidence. |
| `manager-authz` | pass | clarification_needed | fail | Missing store and manager-store ownership policy should trigger clarification; implementing from L1 caused a wrong admin/owner interpretation. |
| `concurrent-login` | pass | partial | partial | Weaker prompts implemented core stale-token behavior and concurrency control but missed explicit different-member token retention coverage. |
| `waiting-rank` | pass | partial | partial | Weaker prompts implemented core waiting/rank behavior but missed available-slot waiting rejection and own-reservation rejection coverage. |

Portfolio-safe interpretation:

> Detailed L5 prompts were sufficient in the first isolated Korean blind pilot. Lower-quality prompts often produced useful core behavior, but they did not reliably force backend negative paths, ownership policy, rollback evidence, response/test contracts, or final guard coverage. One L3 result correctly stopped for clarification instead of inventing missing product policy.

## Required Pre-Experiment Artifacts

Do not start the next implementation run until these artifacts are added or updated.

1. Prompt-quality boundary table
   - Location: `benchmarks/reports/prompt-sufficiency-report.md`
   - Must show L5/L3/L1 outcome by feature.
   - Must include the concrete missed oracle categories, not just pass rates.
   - Must explicitly state that L5 evidence is one-repeat Korean blind evidence, not proof across languages or repeats.

2. Junior backend prompt checklist
   - Location: `benchmarks/reports/prompt-sufficiency-report.md` or a linked checklist document under `benchmarks/reports/`.
   - Must convert observed failures into prompt-writing checks.
   - Required categories:
     - actor and ownership policy
     - allowed and rejected operations
     - negative paths and error contracts
     - response shape and status fields
     - DB constraint or final guard requirements
     - transaction, rollback, and concurrency expectations
     - required test list
     - ambiguity rule: ask clarification instead of inventing policy

3. Expansion decision memo
   - Location: `benchmarks/prompt-sufficiency-blind-plan.md`
   - Must compare at least these options:
     - Korean blind 3-repeat expansion
     - English blind pilot rows
     - stricter L0/context-reduction rows
   - Must choose one next experiment and explain why it is the right next validity control.

4. Next-experiment record design
   - Location: `benchmarks/prompt-sufficiency-blind-plan.md` plus any required CSV matrix update.
   - Must define run IDs, prompt paths, baseline paths, target paths, and run order before implementation starts.
   - Must specify whether new rows reuse `prompt-sufficiency-blind-matrix.csv` or use a new expansion matrix.
   - Must keep implementation context as sanitized repo-outside target plus prompt only.

## Recommended Next Experiment

Recommended next experiment: Korean blind 3-repeat expansion.

Reasoning:

- The current 12-row pilot has only one repeat per condition.
- The strongest validity gap is repeatability of the prompt-only blind outcomes, not language transfer yet.
- Korean repeats can test whether the observed boundary pattern is stable:
  - L5 pass stability
  - L3/L1 partial stability
  - clarification behavior under underspecified manager-authz prompts
  - source independence across repeats

Recommended scope:

- Add repeats `002` and `003` for the existing 12 Korean blind conditions.
- Total new rows: 24.
- Run in small batches, not all at once:
  1. one feature gradient at a time, or
  2. one prompt level across features if comparing level effects.
- Stop after any validator failure, isolation failure, source-identical result without justification, or required oracle/skill/evaluator change.

Do not start English expansion until the Korean repeat evidence is summarized, unless the explicit goal changes to language-transfer evidence.

## Execution Gate

Before any next implementation agent is started:

1. `python3 scripts/validate-benchmark-records.py` passes.
2. `python3 scripts/generate-benchmark-metrics.py --check` passes.
3. The three synthesis artifacts above are drafted.
4. The next experiment's run IDs and matrix rows are defined.
5. The implementation prompt excludes hidden oracle, prior target paths, run notes, and convention comparisons.
6. The coordinator/verifier session does not implement after reading hidden oracle or historical records.

## Stop Conditions

Stop before implementation if any of these are true:

- The next experiment objective is unclear.
- The selected expansion would mix oracle-assisted and prompt-only evidence.
- The run matrix is not defined.
- The implementation agent would see hidden oracle, previous solution source, run notes, or comparison rows.
- A skill/reference/evaluator/oracle change seems necessary before recording the current failure boundary.
