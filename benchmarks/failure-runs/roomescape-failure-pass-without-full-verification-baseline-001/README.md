# Pass Without Full Verification Baseline Fixture

This fixture records the baseline evaluator probe for `failure-recovery-pass-without-full-verification-001`.

The canonical benchmark records are not intentionally poisoned with a false pass row. Instead, the baseline run note records a synthetic validator probe that temporarily points `RUNS_PATH` at a generated CSV containing one strict `result_status=pass` row with `full_verification_result=not_run`.
