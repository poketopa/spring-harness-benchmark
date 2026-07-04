# Prompt-Only Blind Execution Protocol

## Goal

Run the prompt-only blind pilot without repeating the contamination risk from the completed oracle-assisted 84-run baseline.

The implementation agent must be evaluated on:

- the selected prompt
- the clean baseline project
- normal local project discovery inside that isolated target

The verifier may use:

- hidden oracle
- source diff and source hash
- full test output
- convention rubric
- historical benchmark records

## Roles

### Planning Session

The current session prepares files, matrix rows, templates, and handoff prompts. It does not implement pilot runs.

### Implementation Session

The implementation session should be started with a workspace rooted at the isolated target project whenever possible. It receives the selected prompt payload and may inspect only that target project.

It must not be told the oracle path or previous solution paths.

Use `benchmarks/benchmark-records/templates/blind-implementation-prompt-template.md` to prepare the implementation prompt.

### Verification Session

The verification session can read the hidden oracle and historical benchmark records. It records outcomes, hashes, diffs, convention comparisons, and matrix status.

## Run Order

Use one dry-run first:

1. `roomescape-prompt-blind-cancel-waiting-ko-l5-001`

If isolation, source hash, note template, and matrix update all work, continue:

2. `roomescape-prompt-blind-cancel-waiting-ko-l3-001`
3. `roomescape-prompt-blind-cancel-waiting-ko-l1-001`
4. `roomescape-prompt-blind-manager-authz-ko-l5-001`
5. `roomescape-prompt-blind-manager-authz-ko-l3-001`
6. `roomescape-prompt-blind-manager-authz-ko-l1-001`
7. `roomescape-prompt-blind-concurrent-login-ko-l5-001`
8. `roomescape-prompt-blind-concurrent-login-ko-l3-001`
9. `roomescape-prompt-blind-concurrent-login-ko-l1-001`
10. `roomescape-prompt-blind-waiting-rank-ko-l5-001`
11. `roomescape-prompt-blind-waiting-rank-ko-l3-001`
12. `roomescape-prompt-blind-waiting-rank-ko-l1-001`

## Pre-Run Checklist

Before each run:

1. Confirm the record set is validation-clean with the current validation path.
2. Public repository cleanup removed the original validation scripts, so restore an equivalent validator before adding new rows.
3. Confirm the matrix row is `planned`.
4. Create the isolated target from the baseline only.
5. Create a run note from `benchmarks/benchmark-records/templates/blind-run-note-template.md`.
6. Mark the matrix row `in_progress`.
7. Start the implementation session with no oracle path and no historical run references.

## Target Creation

Use the baseline path from `benchmarks/benchmark-records/prompt-sufficiency-blind-matrix.csv`.

Recommended command shape:

```bash
rsync -a --delete --exclude build --exclude .gradle {baseline_path}/ {target_path}/
```

Do not copy from:

- `benchmarks/prompt-sufficiency-runs`
- `benchmarks/prompt-sufficiency-blind-runs`
- any stable solution target for the same feature
- any previous run target

## Implementation Prompt Shape

The implementation prompt should include:

- the exact prompt payload from `case_path`
- the fact that the workspace is the target project
- the instruction to implement or ask a material clarification question if the prompt lacks required product policy
- the instruction to run appropriate tests if implementation proceeds

It should not include:

- hidden oracle text
- oracle path
- convention comparison rows
- previous solution path
- source snippets from previous runs
- expected hidden failure list

The coordinator/verifier session should not implement directly after reading the oracle or historical records. If it does, the run is not blind and must be marked `invalid`.

## Source Hash

After implementation ends and before verifier edits, record:

```bash
find src -type f | sort | xargs shasum -a 256 | shasum -a 256
```

Also compare same-feature blind source trees:

```bash
diff -qr {previous_same_feature_target}/src {current_target}/src
```

If a lower-level run is source-identical to a higher-level run without an independently justified reason, mark source similarity as `identical_invalid` and the matrix status as `invalid`.

## Scoring

Use the matrix `status` field as the blind outcome.

- `pass`: oracle and conventions satisfied, full verification passed, isolation passed.
- `clarification_needed`: implementation correctly stopped for missing material policy.
- `partial`: implementation made progress but missed oracle or convention requirements.
- `fail`: implementation broke required behavior, failed verification, or made unsafe assumptions.
- `invalid`: isolation, source independence, or record integrity failed.

In L1 prompts, `clarification_needed` is often stronger evidence than a speculative pass.

## Required Record Updates

After each run:

1. Fill `benchmarks/benchmark-records/runs/{run_id}.md`.
2. Add a `runs.csv` row.
3. Add `convention-comparisons.csv` rows.
4. Update `benchmarks/benchmark-records/prompt-sufficiency-blind-matrix.csv`.
5. Regenerate metrics with the restored metrics generator.
6. Run the restored record validator.
7. Confirm generated metrics match the CSV records.

## Stop Conditions

Stop and report before continuing to the next run if:

- implementation context included hidden oracle or prior solution source
- source hash/diff shows unexplained identical source reuse
- validator fails
- metrics check fails
- a run requires skill/reference/evaluator/oracle changes
- target creation overwrites an existing run
- a prompt or oracle needs to be changed

## Portfolio Note

Each report should preserve the distinction:

- Oracle-assisted baseline: proves the harness can drive strict verification.
- Prompt-only blind pilot: tests whether prompt quality alone is sufficient.

This distinction is part of the portfolio argument. It shows the owner can challenge favorable results instead of overstating them.
