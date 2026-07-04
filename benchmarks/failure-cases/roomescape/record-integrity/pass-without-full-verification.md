# Pass Without Full Verification Failure Case

## Case Metadata

- Case ID: `failure-recovery-pass-without-full-verification-001`
- Unit: `record-integrity`
- Failure type: `missing_verification`
- Canonical requirement: `benchmarks/benchmark-records/README.md`
- Baseline path: `benchmarks/benchmark-records`
- Baseline run ID: `roomescape-failure-pass-without-full-verification-baseline-001`
- Rerun ID: `roomescape-failure-pass-without-full-verification-rerun-001`
- Status: `corrected`

## Intended Risk

A strict benchmark run can claim `result_status=pass` while its `full_verification_result` is `not_run` or `fail`.

The risk is that the validator treats the contradiction as a warning, allowing a record set to exit successfully even though the pass claim is unsupported.

## Expected Good Behavior

The evaluator should reject strict benchmark records that claim pass without passing full verification. A clean result makes this condition a validator error, not a warning.

## Probe Payload

Use only this section as the probe scenario for the baseline and rerun attempts.

Create a synthetic strict benchmark row with:

- `run_id`: `roomescape-failure-validator-probe-pass-without-verification-001`
- `result_status`: `pass`
- `full_verification`: `not_run`
- `full_verification_result`: `not_run`
- `convention_violations_total`: `0`

Run the benchmark validator against that synthetic row without permanently adding it to canonical records.

## Evaluation Notes

- Baseline failure category: `missing_verification`.
- Passing evaluator behavior: the synthetic strict pass-without-verification row is rejected as an error.
- Failing evaluator behavior: the synthetic row produces only a warning or no issue.
- Do not update the validator before recording the baseline result.
