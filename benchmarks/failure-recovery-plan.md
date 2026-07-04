# Failure Recovery Benchmark Plan

## Purpose

This benchmark is the next research track after repeatability and requirement robustness.

The previous tracks answered:

- Repeatability: does the same skill produce convention-stable output for the same requirement text?
- Requirement robustness: does the same skill preserve semantics and conventions when equivalent requirements are rewritten in different styles?

This track answers a harder portfolio question:

> Can the harness expose realistic AI coding failures, convert those failures into measurable rules, improve the skill/reference/evaluator, and verify the improvement with reruns?

The goal is not to show that every run passes. The goal is to show a disciplined failure loop:

```text
hard case -> baseline failure -> recorded evidence -> targeted improvement -> rerun -> measured result
```

## Portfolio Thesis

This project should be presented as an AI coding skill evaluation harness, not as a Spring service project.

The strongest story is:

> I built a benchmark harness for AI-assisted coding skills. It measures whether a coding skill keeps implementation quality stable across repeated prompts, paraphrased requirements, and intentionally difficult failure cases. Failures are recorded, classified, fixed through skill/reference/evaluator changes, and verified through reruns.

## Required Failure Loop

Every failure-recovery case must follow this sequence.

1. Define the hard case and expected risk before implementation.
2. Run the current skill without silently correcting the output.
3. If the run fails, record the failure first.
4. Classify the failure with a stable category.
5. Decide exactly one intervention:
   - skill instruction update
   - reference rule update
   - evaluator/validator check
   - benchmark record schema/check update
6. Rerun the same hard case.
7. Compare baseline and rerun with evidence.

Do not collapse multiple interventions into one rerun unless the run is blocked without them. The point is to make the improvement attributable.

## Stop Conditions

Stop and report before continuing if any of these occur:

- P0/P1 convention violation
- test failure
- semantic drift
- validator failure
- evaluator failure that cannot be explained
- skill/reference gap
- hard-case requirement ambiguity that changes the intended benchmark question

Failure-recovery cases are allowed to fail, but failures must be recorded before any correction.

## Failure Taxonomy

Use these categories for the first benchmark set.

| Category | Failure Definition | Expected Good Behavior |
| --- | --- | --- |
| `ambiguity_handling` | Requirement leaves a material policy unspecified. | The skill records ambiguity or asks for a decision criterion instead of silently inventing policy. |
| `requirement_conflict` | Requirement conflicts with established project convention or another requirement. | The skill detects and documents the conflict before implementation. |
| `decoy_scope_control` | Requirement includes examples/future ideas that are not current scope. | The skill avoids speculative implementation and records non-scope notes only. |
| `semantic_drift` | Output weakens, removes, or adds required behavior. | The semantic checklist catches drift before the run is marked pass. |
| `architecture_drift` | Output violates controller/service/domain/repository boundaries. | Convention comparison or evaluator records the violation. |
| `missing_verification` | Required narrow/full/concurrency tests are missing or misclassified. | The run fails verification comparison until tests exist. |
| `concurrency_weakness` | Service validation passes sequentially but races violate invariants. | Real concurrent test and DB/locking final guard exist. |
| `record_integrity_gap` | Benchmark records claim pass while evidence is missing or inconsistent. | Validator catches the mismatch. |
| `evaluator_blind_spot` | The harness fails to catch an intentional bad fixture. | Evaluator is improved and rerun against the fixture. |

## Initial Hard Cases

Start with six hard cases. Do not add more until the first six have baseline results.

| Case ID | Unit | Type | Intended Risk | First Expected Outcome |
| --- | --- | --- | --- | --- |
| `failure-recovery-ambiguous-waiting-approval-001` | `c2-combined` | ambiguity | Requirement says waiting is handled on cancellation but does not specify automatic promotion vs manual approval. | Current skill may choose a policy without recording ambiguity. |
| `failure-recovery-conflicting-authz-errors-001` | `manager-authz` | conflict | Requirement says authn/authz must be distinguishable but also says all failures must be hidden as the same response. | Skill should detect conflict and record decision boundary. |
| `failure-recovery-decoy-redis-session-001` | `concurrent-login` | decoy | Requirement mentions Redis as a possible future option without requiring it. | Skill should avoid adding Redis/dependency/speculative infrastructure. |
| `failure-recovery-controller-auth-antipattern-001` | `manager-authz` | conflict | Requirement explicitly asks controller methods to perform authorization checks. | Skill should reject or document conflict with authorization boundary. |
| `failure-recovery-concurrent-waiting-race-001` | `c1-waiting` | concurrency | Two users or duplicate requests race to create waiting entries for the same slot. | Sequential checks alone should be considered insufficient if invariant can race. |
| `failure-recovery-missing-concurrent-test-fixture-001` | `concurrent-login` | evaluator blind spot | A fixture claims concurrent-login pass without a real concurrent test. | Evaluator should fail the fixture; if not, improve evaluator. |

## Recommended First Case

Start with:

`failure-recovery-ambiguous-waiting-approval-001`

Reason:

- It tests a core AI-agent capability: knowing when not to invent an unstated policy.
- It builds directly on the existing waiting/cycle2 requirement family.
- It can fail usefully without requiring production-scale infrastructure.
- It creates a clear portfolio story about ambiguity detection.

## Required New Artifacts

Create these before the first hard-case run:

- `benchmarks/failure-cases/README.md`
- `benchmarks/failure-cases/roomescape/c2-combined/ambiguous-waiting-approval.md`
- `benchmarks/benchmark-records/failure-recovery-matrix.csv`
- `benchmarks/reports/failure-recovery-report.md` only after at least one baseline/rerun pair exists

Recommended matrix headers:

```csv
case_id,unit,failure_type,prompt_language,requirement_path,baseline_path,target_path,baseline_run_id,baseline_status,intervention_type,intervention_path,rerun_id,rerun_status,validated
```

## Baseline Run Recording

Failure-recovery run IDs should be explicit:

- Baseline: `roomescape-failure-ambiguous-waiting-approval-baseline-001`
- Rerun: `roomescape-failure-ambiguous-waiting-approval-rerun-001`

Run notes still live under:

```text
benchmarks/benchmark-records/runs/{run_id}.md
```

Target projects should live under:

```text
benchmarks/failure-runs/{target_name}
```

## First Session Execution Boundary

The next session should not try to finish the whole failure-recovery benchmark.

The first session should complete only this setup slice:

1. Read the mandatory files listed in `benchmarks/benchmark-records/next-session-handoff.md`.
2. Confirm the current validator passes.
3. Create the failure-cases README.
4. Create the first ambiguous waiting approval hard-case requirement.
5. Create `failure-recovery-matrix.csv` with six planned cases.
6. Extend the current record validator to validate the new matrix headers, path existence, unique case IDs, and status values.
7. Run the current record validator.
8. Stop if validator fails.

Only after that setup is valid should a later step run the first baseline implementation.

## Exact First Message For New Session

Use this exact message to start the next Codex session:

```text
/Users/lhs/Desktop/harness 프로젝트에서 계속 진행한다.

이번 세션의 목표는 Failure Recovery Benchmark를 시작하기 위한 setup slice를 완료하는 것이다. 이 프로젝트는 Spring 서비스 프로젝트가 아니라 AI coding skill 평가 하네스 프로젝트로 다룬다.

먼저 반드시 다음 문서를 읽는다:
1. README.md
2. benchmarks/README.md
3. benchmarks/benchmark-records/README.md
4. benchmarks/benchmark-records/summary.md
5. benchmarks/reports/requirement-robustness-report.md
6. benchmarks/failure-recovery-plan.md
7. .agents/skills/spring-usecase-implementation/SKILL.md

이번 세션에서 할 일:
1. 현재 기록 검증 수단으로 시작 상태를 확인한다.
2. `benchmarks/failure-cases/README.md`를 만든다.
3. 첫 hard case인 `benchmarks/failure-cases/roomescape/c2-combined/ambiguous-waiting-approval.md`를 만든다.
4. `benchmarks/benchmark-records/failure-recovery-matrix.csv`를 만들고, `benchmarks/failure-recovery-plan.md`의 6개 initial hard case를 planned 상태로 기록한다.
5. 현재 기록 검증 수단을 확장해서 failure-recovery matrix의 header, unique case_id, path existence, status value를 검증하게 한다.
6. 기록 검증을 다시 실행한다.

이번 세션에서는 아직 baseline implementation run을 수행하지 않는다. 목표는 failure-recovery benchmark setup이 validator로 검증되는 상태까지 만드는 것이다.

P0/P1 위반, validator 실패, 기존 robustness/repeatability 기록 손상, skill/reference gap이 있으면 즉시 멈추고 보고한다.
```
