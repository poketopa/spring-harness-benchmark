# Pass Without Full Verification Rerun Fixture

This fixture records the corrected evaluator probe for `failure-recovery-pass-without-full-verification-001`.

After the validator intervention, a synthetic strict `result_status=pass` row with `full_verification_result=not_run` is rejected as an error instead of accepted with a warning.
