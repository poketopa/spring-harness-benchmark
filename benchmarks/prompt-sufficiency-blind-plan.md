# Prompt-Only Blind Benchmark Plan

## Purpose

The completed 84-run prompt-sufficiency baseline proved that the benchmark harness can drive Spring implementations to a validator-clean state when hidden oracles, prior benchmark context, and existing stable targets are available.

It does not yet prove that a weak junior-developer prompt is sufficient by itself.

This follow-up track isolates the real prompt-quality question:

> When the implementation agent can only see the user prompt and a clean baseline project, how much prompt detail is required before AI-assisted Spring implementation remains reliable?

## Portfolio Positioning

For a competitive junior backend portfolio, the strongest claim is not "AI passed every prompt." The stronger claim is:

> I detected that my first prompt-sufficiency result could be inflated by oracle and context leakage, documented the validity risk, and designed a stricter blind evaluation protocol.

This shows backend judgment, experiment design, and evidence discipline. The portfolio value comes from proving that the owner can evaluate AI-generated Spring code rather than trusting a successful-looking run.

## Current Situation

- The main benchmark harness has 167 recorded runs and 145 passing runs.
- Requirement robustness is complete: 12/12 pass.
- Failure recovery is complete and validator-clean: 10 hard cases plus 4 reruns.
- Prompt sufficiency baseline is complete: 84/84 pass.
- Prompt sufficiency baseline had 0 convention failures and no skill/reference/evaluator/oracle changes.
- The latest prompt-sufficiency baseline run is `roomescape-prompt-waiting-rank-en-l1-003`.
- The first prompt-only blind dry-run is complete: `roomescape-prompt-blind-cancel-waiting-ko-l5-001` passed with source hash `1ccbfbc6c575b1bf8fe3bf66d90beeaccef399aed4249f078a12ab6d42e2d471`.
- `roomescape-prompt-blind-cancel-waiting-ko-l3-001` and `roomescape-prompt-blind-cancel-waiting-ko-l1-001` ran from sanitized repo-outside `/tmp` projects and are recorded as `partial`: full verification passed, but hidden oracle checks found missing past-cancel rejection and rollback-test coverage.
- `roomescape-prompt-blind-manager-authz-ko-l5-001` ran from a sanitized repo-outside `/tmp` project and passed with source hash `c9020246341c76a218b7ae4270db966dee601d915605abe66724edf006daccfd`.
- `roomescape-prompt-blind-manager-authz-ko-l3-001` ran from a sanitized repo-outside `/tmp` project and stopped with `clarification_needed` because the prompt did not define whether store means existing `Theme` or a new `Store` concept, nor how manager-store ownership is determined. Its source hash is `a0dcd96afc38dac5b62eadd0dd17d27c266fdb878eb6a1bfb814b818b4c0afd3`.
- `roomescape-prompt-blind-manager-authz-ko-l1-001` ran from a sanitized repo-outside `/tmp` project and is recorded as `fail`: full verification passed, but the implementation invented an admin-vs-own-reservation policy instead of the oracle's store-manager authorization. Its source hash is `dd9cbb66dc9d2f00bea5bf7ec3ad14d916172dccc97e12f2c4bbe7ea26d6f926`.
- `roomescape-prompt-blind-concurrent-login-ko-l5-001` ran from a sanitized repo-outside `/tmp` project and passed the concurrent-login oracle. Its source hash is `c382edd69e0a2425b8c14f29746f678fdd5c539555ad1b7af5a203d77553aea3`.
- `roomescape-prompt-blind-concurrent-login-ko-l3-001` ran from a sanitized repo-outside `/tmp` project and is recorded as `partial`: core concurrent-login behavior and full verification passed, but the hidden oracle's required different-member token retention test was missing. Its source hash is `535c446ab3a1f7ac24462c6f40916d0ac34836b8cb48911ab771cfd14f077bfa`.
- `roomescape-prompt-blind-concurrent-login-ko-l1-001` ran from a sanitized repo-outside `/tmp` project and is recorded as `partial`: core concurrent-login behavior and full verification passed, but the hidden oracle's required different-member token retention test was missing. Its source hash is `905c77874b70b96a12d27b1d8487aa8f862b345d8e91185849f95aea7105f07f`.
- `roomescape-prompt-blind-waiting-rank-ko-l5-001` ran from a sanitized repo-outside `/tmp` project and passed the waiting-rank oracle. Its source hash is `3d61caea1e8bdc36ad857eea4a54fdddae281a41420ef2198456f437e30e994f`.
- `roomescape-prompt-blind-waiting-rank-ko-l3-001` ran from a sanitized repo-outside `/tmp` project and is recorded as `partial`: full verification passed and core waiting/rank behavior worked, but available-slot waiting rejection was not represented and required own-reservation rejection plus final-guard test coverage were missing. Its source hash is `2007d27f987e2b5dc22f9c2e7bb09672e33f81768e36a1a659075e4036bc6b7d`.
- `roomescape-prompt-blind-waiting-rank-ko-l1-001` ran from a sanitized repo-outside `/tmp` project and is recorded as `partial`: full verification passed and core waiting/rank plus duplicate DB guard coverage worked, but available-slot waiting rejection and own-reservation rejection coverage were missing. Its source hash is `b7455933badc91af09b1e62357f33e3c1d87bfce530eb14b177b2960561f37a7`.
- Remaining blind pilot rows: 0 planned Korean rows.
- Blind pilot matrix: `benchmarks/benchmark-records/prompt-sufficiency-blind-matrix.csv`.
- Blind run template: `benchmarks/benchmark-records/templates/blind-run-note-template.md`.
- Blind implementation prompt template: `benchmarks/benchmark-records/templates/blind-implementation-prompt-template.md`.
- Execution protocol: `benchmarks/prompt-sufficiency-blind-execution-protocol.md`.

## Threats To Validity In The 84-Run Baseline

The completed prompt-sufficiency baseline should be interpreted as an oracle-assisted harness benchmark, not as prompt-only evidence.

Known validity risks:

- Hidden oracles anchored requirements that L1 prompts did not state.
- Previous stable targets and prompt-sufficiency run outputs existed in the same repository.
- Several repeat and level targets used synchronized representative source rather than independent fresh implementations.
- L1/L3/L5 source equality means repeated pass counts are not independent samples.
- The skill had already been strengthened by earlier benchmark phases.

The result remains useful, but the claim must be scoped:

> In an oracle-assisted benchmark harness, the system produced validator-clean Spring implementations across all planned prompt levels.

The completed blind pilot tests:

> In an oracle-blind implementation setting, which prompt levels still produce correct behavior, and where does the agent appropriately stop to ask for policy decisions?

## Experimental Design

Use a two-agent separation:

- Implementation agent: sees only the prompt, the selected baseline project, and normal project files inside that isolated target.
- Verification agent: sees the hidden oracle, run output, diffs, tests, and convention rubric after implementation is complete.

The implementation agent must not see:

- hidden oracle files
- previous prompt-sufficiency run targets
- run notes
- stable solution targets for the same feature
- representative implementations from other levels or languages
- comparison rows from previous runs

## Isolation Rules

Each blind run must use an isolated target directory.

Required controls:

- Create the target from the intended baseline only.
- Do not copy `src` from any previous prompt-sufficiency run.
- Hide or exclude `benchmarks/prompt-sufficiency-runs`, existing solution targets, run notes, and oracle files from the implementation prompt.
- Record a source hash after implementation.
- Compare source hashes and diffs across same-feature blind runs.
- Treat identical source across levels or repeats as evidence of leakage or non-independence unless independently justified.

## Pilot Scope

Start with a small pilot before expanding.

| Feature | Prompt Levels | Language | Repeats | Runs |
| --- | --- | --- | ---: | ---: |
| `cancel-waiting` | L5, L3, L1 | ko | 1 | 3 |
| `manager-authz` | L5, L3, L1 | ko | 1 | 3 |
| `concurrent-login` | L5, L3, L1 | ko | 1 | 3 |
| `waiting-rank` | L5, L3, L1 | ko | 1 | 3 |
| Total | mixed | ko | 1 | 12 |

Korean is the first pilot language because the project owner wants a junior backend portfolio that can explain Korean prompt ambiguity as well as implementation correctness. English expansion comes after the pilot validates the blind protocol.

## Scoring

A blind run can end in more than one valid state.

| Outcome | Meaning |
| --- | --- |
| `pass` | Implementation satisfies hidden oracle and project conventions after full verification. |
| `clarification_needed` | Agent refuses to invent missing product policy and asks a material question. This is a good outcome for L1/L2 ambiguity. |
| `partial` | Core behavior works but one or more oracle/convention requirements fail. |
| `fail` | Implementation violates a hidden oracle, breaks existing behavior, or cannot verify. |
| `invalid` | Isolation, record integrity, or source-leakage rules were broken. |

For L1 prompts, `clarification_needed` may be better evidence than a lucky implementation pass.

## Evidence To Record

Each blind run should record:

- prompt path
- baseline path
- isolated target path
- implementation-visible file list or context summary
- hidden oracle path used only by verifier
- result status
- targeted verification command and result
- full verification command and result
- source hash
- source similarity to same-feature prior blind runs
- whether the agent asked for clarification
- oracle failures, if any
- convention failures, if any
- whether any skill/reference/evaluator/oracle file changed

## Pilot Success Criteria

The pilot is successful if it produces trustworthy evidence, not necessarily if all runs pass.

Success criteria:

- All 12 pilot runs have complete records.
- Validator and metrics checks pass.
- No implementation agent sees hidden oracle or previous solution code.
- Source hashes/diffs prove that level results are independent.
- The report separates pass, fail, and clarification-needed outcomes.
- The portfolio narrative explains what the owner learned about prompt quality and ambiguity boundaries.

## Expansion Decision

The 12-run Korean pilot now shows real divergence between L5 and weaker prompts:

- L5 passed all four features once.
- L3/L1 produced six partial results and one fail.
- One L3 result correctly stopped with `clarification_needed`.
- No isolation invalid row, skill/reference change, evaluator change, or oracle change was required.

Decision: expand the Korean prompt-only blind conditions to three repeats before starting English transfer or L0/context-reduction rows.

| Option | What it tests | Benefit | Main cost/risk | Decision |
| --- | --- | --- | --- | --- |
| Korean blind 3-repeat expansion | Whether the observed L5 pass, L3/L1 partial, and clarification boundary patterns repeat under the same language and isolation protocol. | Controls the largest current validity gap: one-repeat instability. Tests source independence across repeats before adding new variables. | Adds 24 implementation rows and requires careful isolation/source-diff discipline. | Recommended next experiment. |
| English blind rows | Whether the Korean boundary transfers to English prompts. | Useful portfolio comparison after Korean repeatability is known. | Adds language as a second variable before repeat stability is measured. A language effect could be confused with one-repeat noise. | Defer until Korean 3-repeat evidence is summarized. |
| Stricter L0/context-reduction rows | Whether very weak prompts or even less context cause clarification/failure earlier. | Good stress test if L1 still looks too favorable after repeats. | Increases ambiguity before the current boundary is statistically stable. May mostly measure prompt emptiness rather than backend prompt quality. | Defer until after Korean repeat expansion; add if L1 remains too successful or too lucky. |

Rationale:

- The current validity gap is not language transfer. It is that each blind condition has only one independent Korean repeat.
- The strongest next claim is repeat stability: L5 pass stability, L3/L1 partial stability, manager-authz clarification behavior, and source independence across same-feature repeats.
- English rows should be introduced after the Korean repeat pattern is known, so language is measured as a separate factor.
- No post-baseline skill/reference/evaluator/oracle update should occur before the repeat expansion records the current prompt-quality boundary.

## Next-Experiment Record Design

The next experiment is the Korean blind 3-repeat expansion. It adds repeats `002` and `003` for the existing 12 Korean blind conditions, for 24 new rows. This plan defines the rows and path rules, but no implementation run starts in this session.

### Matrix Use

Use the existing blind matrix schema in `benchmarks/benchmark-records/prompt-sufficiency-blind-matrix.csv`.

Before starting the next implementation session, append the 24 expansion rows with:

- `status`: `planned`
- `implementation_context`: blank until target creation, then `sanitized_tmp_project_and_prompt_only`
- `verifier_context`: blank until verification, then `oracle_after_implementation`
- `source_hash`: blank until implementation ends
- `source_similarity`: blank until source comparison
- `clarification_needed`: blank until scoring
- `isolation_status`: `pending`
- `notes_path`: `benchmarks/benchmark-records/runs/{run_id}.md`

Do not append or run any English, L0, L2, or L4 blind expansion rows until the Korean 3-repeat expansion is summarized.

### Run ID Rule

Use the existing prompt-only blind format:

```text
roomescape-prompt-blind-{feature}-{language}-{level}-{repeat}
```

For this expansion:

- `feature`: `cancel-waiting`, `manager-authz`, `concurrent-login`, `waiting-rank`
- `language`: `ko`
- `level`: `l5`, `l3`, `l1`
- `repeat`: `002`, `003`

### Prompt, Baseline, Oracle, And Target Paths

Reuse the existing prompt and oracle files for each feature/level. The implementation agent still receives only the prompt payload and the isolated target project; the hidden oracle path remains verifier-only.

| Feature | Prompt paths | Oracle path | Baseline path | Target path rule |
| --- | --- | --- | --- | --- |
| `cancel-waiting` | `benchmarks/prompt-sufficiency-cases/roomescape/cancel-waiting/ko/l{5,3,1}.md` | `benchmarks/prompt-sufficiency-cases/roomescape/oracles/cancel-waiting.md` | `benchmarks/roomescape-jpa-auth-cycle1-regen-v2` | `benchmarks/prompt-sufficiency-blind-runs/{run_id}` |
| `manager-authz` | `benchmarks/prompt-sufficiency-cases/roomescape/manager-authz/ko/l{5,3,1}.md` | `benchmarks/prompt-sufficiency-cases/roomescape/oracles/manager-authz.md` | `benchmarks/roomescape-jpa-auth-cycle2-regen-v2` | `benchmarks/prompt-sufficiency-blind-runs/{run_id}` |
| `concurrent-login` | `benchmarks/prompt-sufficiency-cases/roomescape/concurrent-login/ko/l{5,3,1}.md` | `benchmarks/prompt-sufficiency-cases/roomescape/oracles/concurrent-login.md` | `benchmarks/roomescape-jpa-auth-manager-authorization-regen-v2` | `benchmarks/prompt-sufficiency-blind-runs/{run_id}` |
| `waiting-rank` | `benchmarks/prompt-sufficiency-cases/roomescape/waiting-rank/ko/l{5,3,1}.md` | `benchmarks/prompt-sufficiency-cases/roomescape/oracles/waiting-rank.md` | `benchmarks/roomescape-jpa-auth-base-v2` | `benchmarks/prompt-sufficiency-blind-runs/{run_id}` |

Implementation workspaces must be repo-outside sanitized copies:

```text
/tmp/harness-blind-work/{run_id}
```

Create each workspace only from the baseline path:

```bash
rsync -a --delete --exclude build --exclude .gradle {baseline_path}/ /tmp/harness-blind-work/{run_id}/
```

After implementation ends and before oracle review, record:

```bash
find src -type f | sort | xargs shasum -a 256 | shasum -a 256
```

Then copy or sync the final target into:

```text
benchmarks/prompt-sufficiency-blind-runs/{run_id}
```

### Planned Run Order

Run one feature gradient at a time. Within each feature, run repeat `002` across L5/L3/L1, then repeat `003` across L5/L3/L1.

| Order | Feature | Repeat | Run IDs in order |
| ---: | --- | --- | --- |
| 1 | `cancel-waiting` | `002` | `roomescape-prompt-blind-cancel-waiting-ko-l5-002`, `roomescape-prompt-blind-cancel-waiting-ko-l3-002`, `roomescape-prompt-blind-cancel-waiting-ko-l1-002` |
| 2 | `cancel-waiting` | `003` | `roomescape-prompt-blind-cancel-waiting-ko-l5-003`, `roomescape-prompt-blind-cancel-waiting-ko-l3-003`, `roomescape-prompt-blind-cancel-waiting-ko-l1-003` |
| 3 | `manager-authz` | `002` | `roomescape-prompt-blind-manager-authz-ko-l5-002`, `roomescape-prompt-blind-manager-authz-ko-l3-002`, `roomescape-prompt-blind-manager-authz-ko-l1-002` |
| 4 | `manager-authz` | `003` | `roomescape-prompt-blind-manager-authz-ko-l5-003`, `roomescape-prompt-blind-manager-authz-ko-l3-003`, `roomescape-prompt-blind-manager-authz-ko-l1-003` |
| 5 | `concurrent-login` | `002` | `roomescape-prompt-blind-concurrent-login-ko-l5-002`, `roomescape-prompt-blind-concurrent-login-ko-l3-002`, `roomescape-prompt-blind-concurrent-login-ko-l1-002` |
| 6 | `concurrent-login` | `003` | `roomescape-prompt-blind-concurrent-login-ko-l5-003`, `roomescape-prompt-blind-concurrent-login-ko-l3-003`, `roomescape-prompt-blind-concurrent-login-ko-l1-003` |
| 7 | `waiting-rank` | `002` | `roomescape-prompt-blind-waiting-rank-ko-l5-002`, `roomescape-prompt-blind-waiting-rank-ko-l3-002`, `roomescape-prompt-blind-waiting-rank-ko-l1-002` |
| 8 | `waiting-rank` | `003` | `roomescape-prompt-blind-waiting-rank-ko-l5-003`, `roomescape-prompt-blind-waiting-rank-ko-l3-003`, `roomescape-prompt-blind-waiting-rank-ko-l1-003` |

Run `python3 scripts/validate-benchmark-records.py` and `python3 scripts/generate-benchmark-metrics.py --check` before each feature block and after every recorded row. If metrics become stale because planned rows were appended or row statuses changed, run `python3 scripts/generate-benchmark-metrics.py --write`, then rerun both checks.

### Isolation Rules For Expansion

The implementation agent must not see:

- hidden oracle text or oracle path
- previous solution source
- `benchmarks/prompt-sufficiency-runs`
- other `benchmarks/prompt-sufficiency-blind-runs` targets
- run notes
- `convention-comparisons.csv`
- source hashes, prior diffs, or expected hidden failure lists

The coordinator/verifier may read hidden oracle and historical records only after implementation stops or asks for clarification. If the coordinator implements after reading oracle context, mark the run `invalid`.

### Stop Conditions

Stop before launching the next row if any of these occur:

- validator failure
- generated metrics check failure after `--write`
- target path already exists before target creation
- implementation prompt includes hidden oracle, prior target path, run note, convention comparison, or expected hidden failure detail
- source hash/diff shows unexplained identical same-feature source
- implementation needs a skill/reference/evaluator/oracle change
- a prompt or oracle needs to be edited
- a lower-level row is source-identical to an earlier higher-level row without an independently justified reason
- any row is marked `invalid`
- the experiment objective changes from Korean repeatability to language transfer or L0 stress testing

## Next Steps

1. Preserve the sanitized repo-outside workspace method for any expansion runs.
2. Use `benchmarks/reports/prompt-sufficiency-report.md` as the boundary-table and junior-checklist source before launching new rows.
3. Append only the 24 Korean repeat-expansion rows defined above when the next implementation session begins.
4. Keep the 84-run baseline and 12-run pilot separated in portfolio claims.
5. Defer English and L0/context-reduction rows until the Korean 3-repeat expansion is summarized.

The concrete pre-expansion gate is documented in `benchmarks/prompt-sufficiency-expansion-prep.md`. Do not start new implementation rows until the boundary table, prompt checklist, expansion decision memo, and next-experiment record design are drafted.

## L3 Repair And Clarification-Gate Follow-Up

The 36-row Korean prompt-only blind matrix is now the baseline for prompt-quality boundary analysis:

- L5: 12/12 pass.
- L3: 11 partial, 1 clarification_needed.
- L1: 9 partial, 3 fail.

The next question is no longer whether the original Korean boundary repeats. It is:

> Can compact feature-class checklists repair the L3 prompt gap, and can an explicit clarification gate reduce confident wrong implementations for ambiguous authorization prompts?

### Conditions

Use two new blind-only prompt levels:

| Level | Meaning | Rows |
| --- | --- | ---: |
| `L3R` | Repaired L3 prompt with only the missing checklist items added. | 4 features x 3 repeats = 12 |
| `L3Q` | Clarification-gated manager-authz prompt that keeps ownership ambiguous but explicitly forbids inventing a policy. | 1 feature x 3 repeats = 3 |

Do not reinterpret these rows as part of the original L3 distribution. They are intervention rows compared against the original L3 baseline.

### Run IDs

Use these run ID formats:

```text
roomescape-prompt-blind-{feature}-ko-l3r-{repeat}
roomescape-prompt-blind-manager-authz-ko-l3q-{repeat}
```

Allowed repeats remain `001`, `002`, and `003`.

### Case Paths

| Feature | `L3R` case path |
| --- | --- |
| `cancel-waiting` | `benchmarks/prompt-sufficiency-cases/roomescape/cancel-waiting/ko/l3r.md` |
| `manager-authz` | `benchmarks/prompt-sufficiency-cases/roomescape/manager-authz/ko/l3r.md` |
| `concurrent-login` | `benchmarks/prompt-sufficiency-cases/roomescape/concurrent-login/ko/l3r.md` |
| `waiting-rank` | `benchmarks/prompt-sufficiency-cases/roomescape/waiting-rank/ko/l3r.md` |

The clarification-gate case path is:

```text
benchmarks/prompt-sufficiency-cases/roomescape/manager-authz/ko/l3q.md
```

### Comparison Targets

Evaluate the follow-up against these original blind baselines:

- `L3R` pass-rate lift over original `L3`.
- `L3R` convention-failure reduction, especially `test_layering`, `oracle_*_behavior`, `real_concurrent_test`, `atomic_session_renewal`, and `persistence_guard`.
- `L3Q` clarification-needed rate over original manager-authz `L3`.
- No increase in invalid rows, source leakage, or skill/reference/evaluator/oracle changes.

### Stop Condition

Stop the follow-up if any row is `invalid`, if a repair prompt needs to be edited after runs begin, or if the verifier must change hidden oracle/evaluator rules to score the intervention.
