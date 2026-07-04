# 벤치마크 지표

이 보고서는 benchmark CSV 기록에서 생성된다.

- 최신 run 날짜: `2026-07-05`
- Source: `benchmarks/benchmark-records/runs.csv`
- Failure matrix: `benchmarks/benchmark-records/failure-recovery-matrix.csv`
- Robustness matrix: `benchmarks/benchmark-records/requirement-robustness-matrix.csv`
- Prompt sufficiency matrix: `benchmarks/benchmark-records/prompt-sufficiency-matrix.csv`
- Prompt-only blind matrix: `benchmarks/benchmark-records/prompt-sufficiency-blind-matrix.csv`

## 전체 Run

| 지표 | 값 |
| --- | ---: |
| 기록된 run | 245 |
| Pass run | 182 |
| Partial run | 54 |
| Fail run | 9 |
| 기록된 convention failure | 124 |

## 실패 복구

| 지표 | 값 |
| --- | ---: |
| Failure-recovery case | 10 |
| Failure-recovery run | 14 |
| Baseline run | 10 |
| Rerun | 4 |
| Baseline status pass | 6 |
| Baseline status fail | 4 |
| Rerun status skipped | 6 |
| Rerun status pass | 2 |
| Rerun status corrected | 2 |
| Validated case | 10 |
| 영어 prompt case | 9 |
| 한국어 prompt case | 1 |

## 실패 복구 Run 상태

| 지표 | 값 |
| --- | ---: |
| Baseline result pass | 2 |
| Baseline result partial | 6 |
| Baseline result fail | 2 |
| Rerun result pass | 0 |
| Rerun result partial | 2 |
| Rerun result fail | 2 |

## 요구사항 견고성

| 지표 | 값 |
| --- | ---: |
| Robustness matrix row | 12 |
| Robustness pass row | 12 |
| Robustness planned row | 0 |
| Robustness fail row | 0 |

## 프롬프트 충분성

| 지표 | 값 |
| --- | ---: |
| Prompt-sufficiency planned run | 0 |
| Prompt-sufficiency completed run | 84 |
| 한국어 prompt-sufficiency row | 42 |
| 영어 prompt-sufficiency row | 42 |
| Cancel-waiting row | 30 |
| Manager-authz row | 18 |
| Concurrent-login row | 18 |
| Waiting-rank row | 18 |
| L5 row | 24 |
| L4 row | 6 |
| L3 row | 24 |
| L2 row | 6 |
| L1 row | 24 |

## Prompt-Only Blind

| 지표 | 값 |
| --- | ---: |
| Blind matrix row | 51 |
| Blind planned row | 0 |
| Blind in-progress row | 0 |
| Blind pass row | 24 |
| Blind clarification-needed row | 4 |
| Blind partial row | 20 |
| Blind fail row | 3 |
| Blind invalid row | 0 |
| Blind 한국어 row | 51 |
| Blind 영어 row | 0 |
| Blind cancel-waiting row | 12 |
| Blind manager-authz row | 15 |
| Blind concurrent-login row | 12 |
| Blind waiting-rank row | 12 |
| Blind L5 row | 12 |
| Blind L3 row | 12 |
| Blind L3R row | 12 |
| Blind L3Q row | 3 |
| Blind L1 row | 12 |

## 실패 복구 Case

| Case | 언어 | 유형 | Baseline | Rerun | 검증 |
| --- | --- | --- | --- | --- | --- |
| `failure-recovery-ambiguous-waiting-approval-001` | `en` | `ambiguity_handling` | `pass` | `skipped` | `true` |
| `failure-recovery-conflicting-authz-errors-001` | `en` | `requirement_conflict` | `fail` | `pass` | `true` |
| `failure-recovery-decoy-redis-session-001` | `en` | `decoy_scope_control` | `pass` | `skipped` | `true` |
| `failure-recovery-controller-auth-antipattern-001` | `en` | `requirement_conflict` | `fail` | `pass` | `true` |
| `failure-recovery-concurrent-waiting-race-001` | `en` | `concurrency_weakness` | `pass` | `skipped` | `true` |
| `failure-recovery-missing-concurrent-test-fixture-001` | `en` | `evaluator_blind_spot` | `fail` | `corrected` | `true` |
| `failure-recovery-cancel-promotion-drift-001` | `en` | `semantic_drift` | `pass` | `skipped` | `true` |
| `failure-recovery-controller-rank-query-drift-001` | `en` | `architecture_drift` | `pass` | `skipped` | `true` |
| `failure-recovery-pass-without-full-verification-001` | `en` | `missing_verification` | `fail` | `corrected` | `true` |
| `failure-recovery-ko-ambiguous-waiting-approval-001` | `ko` | `ambiguity_handling` | `pass` | `skipped` | `true` |
