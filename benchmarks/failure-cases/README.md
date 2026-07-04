# Failure Recovery Cases

This directory contains intentionally difficult benchmark requirements for the failure-recovery track.

Failure-recovery cases are not success-only requirement variants. Each case should expose a realistic AI coding failure, record the baseline result before correction, apply exactly one targeted intervention, and verify the rerun with evidence.

## Directory Layout

- `roomescape/{unit}/{case-name}.md`: hard-case requirement documents grouped by benchmark unit.
- `../benchmark-records/failure-recovery-matrix.csv`: planned and completed failure-recovery case matrix.
- `prompt_language` in the matrix records the language of the executable prompt payload, such as `en` or `ko`.
- `../failure-runs/{target_name}`: future generated target projects for baseline and rerun attempts.

## Case Document Shape

Each hard-case document should separate benchmark metadata from the actual implementation prompt payload.

- Metadata records the case ID, unit, failure type, intended risk, expected good behavior, baseline path, and planned run IDs.
- The implementation prompt payload is the text used for the skill run.
- Evaluation notes explain how to classify the baseline and rerun outcomes.

Keeping those sections separate matters because the benchmark needs to test whether the skill detects the hard case from the prompt, not from evaluator-only notes.

## Failure Loop

Use this sequence for every case:

1. Define the hard case and expected risk before implementation.
2. Run the current skill without silently correcting the output.
3. Record the baseline result first.
4. Classify the failure with the stable taxonomy in `benchmarks/failure-recovery-plan.md`.
5. Apply exactly one intervention.
6. Rerun the same hard case.
7. Compare baseline and rerun with evidence.

Do not combine multiple interventions unless the run is blocked without them.

## Setup Boundary

The setup slice creates the case catalog, the first hard-case requirement, the matrix, and validator checks. It does not run a baseline implementation.

The first planned case is `failure-recovery-ambiguous-waiting-approval-001`.
