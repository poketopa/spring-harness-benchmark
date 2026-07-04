# L3R Ablation Plan

## Purpose

This screening experiment asks which compact L3R prompt elements are carrying the repair effect.

Research question:

> If one L3R information cluster is removed, does the prompt-only blind result fall back to `partial`, `fail`, or `clarification_needed`?

## Separation Rules

- Do not mix original L3, L3R, L3Q, and ablation rows in the same matrix.
- Use `benchmarks/benchmark-records/prompt-sufficiency-l3r-ablation-matrix.csv` for this experiment.
- Use prompt files under each feature's `ko/ablation/` directory.
- Implementation must run only in `/tmp/harness-l3r-ablation-work/{run_id}`.
- Final copied targets go under `benchmarks/prompt-sufficiency-l3r-ablation-runs/{run_id}`.
- The implementation agent receives only the prompt payload and the isolated project.
- The implementation agent must not see hidden oracle text, oracle paths, previous target paths, run notes, comparison rows, or stable solution source.
- The verifier may read the hidden oracle only after implementation stops.
- Do not change skill, reference, evaluator, or oracle files for this screening.

## Screening Matrix

The first screening has 12 rows: four features times three ablation axes.

| Feature | Row suffix | Removed L3R element cluster | Expected signal |
| --- | --- | --- | --- |
| `cancel-waiting` | `no-rejection-policy` | past-cancel and other-member rejection policy | May regress to missing negative-path behavior/tests. |
| `cancel-waiting` | `no-transaction-rollback` | single-transaction and rollback requirement | May regress to missing rollback evidence. |
| `cancel-waiting` | `no-test-checklist` | explicit required test checklist | Tests whether behavior prose alone still causes oracle-level evidence. |
| `manager-authz` | `no-store-model` | explicit Store/Theme/Manager ownership model | May stop with `clarification_needed` or drift semantically. |
| `manager-authz` | `no-authn-authz-split` | unauthenticated vs unauthorized error split | May pass core ownership but miss auth boundary evidence. |
| `manager-authz` | `no-service-test-boundary` | explicit service-boundary and test checklist | Tests whether policy prose alone preserves architecture and evidence. |
| `concurrent-login` | `no-other-member` | other-member token preservation | May regress to the original L3 missing-evidence pattern. |
| `concurrent-login` | `no-race-atomicity` | real concurrent login/race handling and 500-avoidance | May pass simple stale-token flow but miss concurrency evidence. |
| `concurrent-login` | `no-server-state` | explicit server-side active-session/equivalent storage instruction | May produce an untestable or stateless-token-only design. |
| `waiting-rank` | `no-slot-policy` | available-slot and own-reservation rejection policy | May regress to the original L3 waiting-boundary gap. |
| `waiting-rank` | `no-my-list` | combined my-list status/rank response requirement | May implement waiting but omit read-model behavior. |
| `waiting-rank` | `no-final-guard` | DB/equivalent final duplicate guard | May rely only on service validation. |

## Run Contract

Repository cleanup note: the original run contract used local validation scripts that are no longer included in the public repository. Preserve the recorded results as historical evidence; restore equivalent validation tooling before adding new rows.

Before implementation:

1. Confirm the current record set is validation-clean with the available validation path.
2. Confirm generated metrics match the CSV records.
3. Confirm the ablation matrix row is `planned`.
4. Create the isolated workspace from the row baseline only:

```bash
rsync -a --delete --exclude build --exclude .gradle {baseline_path}/ /tmp/harness-l3r-ablation-work/{run_id}/
```

5. Start a fresh implementation agent with `fork_context=false` and only:
   - isolated workspace path
   - implementation prompt payload
   - blind benchmark instructions

After implementation stops:

1. Record whether it implemented or asked for clarification.
2. Compute source hash before verifier edits:

```bash
find src -type f | sort | xargs shasum -a 256 | shasum -a 256
```

3. Copy the target to `benchmarks/prompt-sufficiency-l3r-ablation-runs/{run_id}`.
4. Compare source against same-feature original L3R and ablation targets.
5. Verifier reads the hidden oracle and scores the row.
6. Add a run note under `benchmarks/benchmark-records/runs/{run_id}.md`.
7. Add rows to `benchmarks/benchmark-records/runs.csv` and `benchmarks/benchmark-records/convention-comparisons.csv`.
8. Update the ablation matrix row status, hash, source similarity, and notes path.
9. Run the restored record validator and metrics consistency check before marking the row complete.

## Screening Decision

Run the 12 rows only if the native subagent path can start implementation agents without inheriting this verifier context. If that isolation cannot be maintained, stop after matrix/prompt creation and mark all rows `planned`.
