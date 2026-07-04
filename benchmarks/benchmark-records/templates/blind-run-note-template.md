# {run_id}

## Summary

- Date:
- Mission: `roomescape-reservation-waiting`
- Cycle: `prompt-sufficiency-blind`
- Feature:
- Prompt language:
- Prompt level:
- Repeat:
- Prompt path:
- Hidden oracle path:
- Baseline:
- Isolated target project:
- Skill:
- Started at:
- Finished at:
- Duration minutes:
- Result status: `pass` | `clarification_needed` | `partial` | `fail` | `invalid`

## Isolation Contract

### Implementation-Visible Context

List exactly what the implementation agent could see.

- Prompt payload:
- Baseline project:
- Current working directory:
- Extra files exposed:

### Prohibited Context

Confirm these were not exposed to the implementation agent.

- [ ] Hidden oracle files
- [ ] `benchmarks/prompt-sufficiency-runs`
- [ ] Previous blind target projects for the same feature
- [ ] Prior run notes or convention comparison rows
- [ ] Stable solution target for the same feature
- [ ] Representative source copied from another level/language/repeat

### Isolation Verdict

- Isolation status: `pass` | `fail`
- If failed, explain and mark the matrix row `invalid`:

## Prompt

Paste the exact implementation prompt payload shown to the implementation agent.

```text
```

## Implementation Outcome

- Implemented:
- Asked clarification:
- Clarification question, if any:
- Assumptions made:
- Files changed:

## Source Hash And Similarity

Compute after implementation, before verifier edits.

- Source hash command:

  ```bash
  find src -type f | sort | xargs shasum -a 256 | shasum -a 256
  ```

- Source hash:
- Compared against same-feature blind runs:
- Source similarity verdict: `unique` | `similar_expected` | `identical_invalid` | `not_applicable`
- Diff command:
- Diff summary:

## Verification

### Implementation-Side Verification

- Narrow command:
- Narrow result:
- Full command:
- Full result:

### Verifier-Only Oracle Review

The verifier may read the hidden oracle only after implementation ends.

- Oracle satisfied:
- Missing oracle requirements:
- Behavioral regressions:
- Existing behavior preserved:

## Convention Comparison

Add one row for every checked decision rule. Use `pass`, `fail`, or `not_applicable`.

| Category | Expected | Observed | Status | Severity | Evidence |
| --- | --- | --- | --- | --- | --- |
| verification | Full verification evidence is recorded unless the run ended in clarification_needed |  |  | P1 |  |
| prompt_blind_isolation | Implementation did not see verifier-only oracle or prior solutions |  |  | P0 |  |
| source_independence | Source hash/diff does not show copied representative source |  |  | P0 |  |

## Skill Changes

Blind pilot runs should not update skill/reference/evaluator/oracle files before the failure boundary is recorded.

- Skill/reference/evaluator/oracle changed: `false`
- If changed, explain why the run is invalid or why record integrity required it:

## Final Verdict

- Matrix status:
- `runs.csv` result_status:
- `full_verification_result`:
- `convention_violations_total`:
- Portfolio note:

## Follow-Up

- Keep:
- Fix before next blind run:
- Add to prompt checklist:
- Expand repeats:
- Next benchmark:
