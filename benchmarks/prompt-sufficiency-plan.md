# Prompt Sufficiency Benchmark Plan

## Purpose

This track measures how much prompt quality a junior backend developer must provide to get reliable AI-assisted Spring implementation results.

The earlier tracks showed that `spring-usecase-implementation` can produce convention-stable code when requirements are detailed and friendly. This track asks a harder portfolio question:

> As Korean and English prompts become less precise, where does implementation quality start to break, and what minimum prompt information should a junior backend developer provide?

## Portfolio Thesis

This project should show that the owner does not merely ask AI to write code. The owner can design measurable experiments around requirement quality, backend correctness, and AI failure boundaries.

The practical output is a junior-developer prompt checklist backed by evidence:

- what must be stated explicitly
- what the skill can infer safely
- where ambiguity should stop implementation
- where missing tests, transaction rules, or authorization boundaries cause quality risk

## Interpretation Update

The first 84-run baseline set is complete and validator-clean, but it should be interpreted as an oracle-assisted harness benchmark. The implementation process had access to benchmark context, hidden oracle expectations were used to anchor evaluation, and some repeated targets reused synchronized representative source.

That result is still useful: it shows that the harness can drive AI-generated Spring implementations to strict verification across prompt levels. It does not, by itself, prove that a weak prompt is sufficient without oracle or prior-solution context.

The next track is the stricter prompt-only blind benchmark:

```text
benchmarks/prompt-sufficiency-blind-plan.md
```

The blind track separates implementation from verification so the implementation agent sees only the prompt and clean baseline project, while the verifier applies the hidden oracle after implementation.

## Chosen Scope

The first prompt-sufficiency benchmark covers four backend feature families:

| Feature | Coverage | Prompt Levels |
| --- | --- | --- |
| `cancel-waiting` | Full degradation target: reservation cancellation, waiting promotion, rank recalculation, transaction behavior | L5, L4, L3, L2, L1 |
| `manager-authz` | Probe target: authentication/authorization and store-manager boundary | L5, L3, L1 |
| `concurrent-login` | Probe target: active-session policy, token invalidation, race handling | L5, L3, L1 |
| `waiting-rank` | Probe target: waiting creation, rank calculation, duplicate/race guard | L5, L3, L1 |

Each planned condition has:

- `prompt_language`: `ko` and `en`
- `repeat`: `001`, `002`, `003`

Total planned baseline runs:

```text
cancel-waiting: 5 levels * 2 languages * 3 repeats = 30
manager-authz: 3 levels * 2 languages * 3 repeats = 18
concurrent-login: 3 levels * 2 languages * 3 repeats = 18
waiting-rank: 3 levels * 2 languages * 3 repeats = 18
total = 84 planned baseline runs
```

## Prompt Levels

| Level | Name | Description | Expected Skill Behavior |
| --- | --- | --- | --- |
| L5 | Complete backend prompt | Policy, failure cases, tests, transactions, and boundaries are explicit. | Implement fully and verify. |
| L4 | Clear policy, weak verification | Main policy is explicit, but some tests or verification details are underspecified. | Implement core behavior and add reasonable tests from existing conventions. |
| L3 | Functional goal only | User goal is clear, but failure cases, transaction rules, or edge cases are missing. | Preserve existing conventions, fill obvious gaps cautiously, and record assumptions. |
| L2 | Vague operational prompt | Prompt uses terms like "properly", "naturally", or "securely" without enough acceptance criteria. | Ask/record ambiguity when multiple materially different implementations are possible. |
| L1 | Minimal beginner prompt | Prompt is too thin to determine correct policy or verification scope. | Stop or request decision criteria instead of inventing product policy. |

## Intervention Rule

Do not update the skill, references, validator, or oracle during the first 84 baseline runs unless record integrity is impossible without a harness fix.

The goal of this track is to identify the failure boundary first. Skill/reference changes come only after the full baseline set is recorded and reviewed.

## Evaluation Rule

Prompts get weaker, but the oracle does not.

A low-quality prompt may omit tests or transaction details. The evaluator still checks the hidden oracle for backend correctness:

- semantic behavior
- error handling
- transaction boundary
- concurrency/race guard when relevant
- controller/service/domain/repository boundaries
- verification evidence
- documentation of assumptions or ambiguity

## Matrix

Planned and completed runs are tracked in:

```text
benchmarks/benchmark-records/prompt-sufficiency-matrix.csv
```

Matrix headers:

```csv
run_id,feature,prompt_language,prompt_level,repeat,case_path,oracle_path,baseline_path,target_path,status
```

## Stop Conditions

Stop and report immediately if any of these occur:

- validator failure
- generated metrics check failure
- existing repeatability, robustness, or failure-recovery records become invalid
- a benchmark target path overwrites an existing run
- a prompt case changes a hidden oracle instead of only changing prompt quality
- a skill/reference change is needed before the blind follow-up decision is recorded
