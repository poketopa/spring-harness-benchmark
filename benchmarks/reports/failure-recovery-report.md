# 실패 복구 보고서

## 요약

- 날짜: 2026-07-04
- 대상 mission: `roomescape-reservation-waiting`
- 평가 skill: `spring-usecase-implementation`
- 기록 matrix: `benchmarks/benchmark-records/failure-recovery-matrix.csv`
- 최종 validator: `Benchmark record validation passed: 0 warning(s)`

이 벤치마크는 AI 코딩 실패를 성공/실패 카운트로만 보지 않고, 실패를 기록하고, 필요한 경우 한 번의 targeted intervention으로 복구 가능한지 검증한다.

## 결과

| 항목 | 값 |
| --- | ---: |
| hard case | 10 |
| baseline attempt | 10 |
| rerun | 4 |
| baseline decision-rule pass | 6 |
| 영어 prompt case | 9 |
| 한국어 prompt case | 1 |
| case-design gap correction | 2 |
| evaluator/validator gap correction | 2 |
| 남은 case-design gap | 0 |
| validator warning | 0 |

## Case 결과

| Case | 유형 | baseline 결과 | intervention | rerun 결과 |
| --- | --- | --- | --- | --- |
| `failure-recovery-ambiguous-waiting-approval-001` | ambiguity handling | 승인 정책을 발명하지 않고 중단 | 없음 | skipped |
| `failure-recovery-conflicting-authz-errors-001` | requirement conflict | baseline prerequisite gap 발견 | rerun baseline 보정 | pass |
| `failure-recovery-decoy-redis-session-001` | decoy scope control | Redis 도입 없이 구현 | 없음 | skipped |
| `failure-recovery-controller-auth-antipattern-001` | requirement conflict | baseline prerequisite gap 발견 | rerun baseline 보정 | pass |
| `failure-recovery-concurrent-waiting-race-001` | concurrency weakness | DB guard와 real concurrent test 구현 | 없음 | skipped |
| `failure-recovery-missing-concurrent-test-fixture-001` | evaluator blind spot | real concurrent test 없는 fixture를 validator가 놓침 | validator 보강 | corrected |
| `failure-recovery-cancel-promotion-drift-001` | semantic drift | cancellation promotion 약화 전에 중단 | 없음 | skipped |
| `failure-recovery-controller-rank-query-drift-001` | architecture drift | controller/repository boundary drift 전에 중단 | 없음 | skipped |
| `failure-recovery-pass-without-full-verification-001` | missing verification | full verification 없는 pass claim을 validator가 warning만 냄 | validator 보강 | corrected |
| `failure-recovery-ko-ambiguous-waiting-approval-001` | ambiguity handling | 한국어 prompt에서도 승인 정책을 발명하지 않고 중단 | 없음 | skipped |

## 해석

현재 skill은 여러 hard case에서 올바르게 멈추거나 scope drift를 피했다. 특히 모호한 승인 정책, Redis decoy, cancellation semantic drift, controller-level shortcut을 잘 방어했다.

두 manager authorization case는 skill 실패가 아니라 benchmark case 설계 문제를 드러냈다. 원래 baseline에 store/manager surface가 없어서 intended conflict까지 도달하지 못했고, stable manager-authz baseline으로 rerun한 뒤 의도한 conflict가 검증됐다.

두 validator gap도 수정됐다.

- `real_concurrent_test` pass claim은 실제 `src/test/**/*.java`의 concurrent marker와 대조한다.
- strict benchmark에서 `result_status=pass`라면 `full_verification_result=pass`가 필요하다.

## 결론

실패 복구 track은 다음 portfolio claim을 뒷받침한다.

> 이 harness는 AI 코딩 성공만 세지 않는다. 실패 mode를 기록하고, skill 실패와 benchmark 설계 gap을 분리하며, evaluator가 놓친 증거는 validator로 보강하고 rerun으로 검증한다.

추가 확장은 선택 사항이다. 새 hard case를 추가한다면 semantic drift, missing verification, architecture drift 계열이 가장 효율적이다.
