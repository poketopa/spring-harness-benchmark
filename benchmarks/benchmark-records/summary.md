# Roomescape Benchmark Research Summary

## Snapshot

- Last updated: 2026-07-05
- Mission: `roomescape-reservation-waiting`
- Skill under test: `spring-usecase-implementation`
- Stable application target: `benchmarks/roomescape-jpa-auth-concurrent-login-regen-v3`
- Latest completed unit: `prompt-sufficiency-l3r-ablation-race-tiebreaker`
- Validator state: `Benchmark record validation passed: 0 warning(s)`
- Metrics state: `benchmarks/reports/benchmark-metrics.md is up to date`

## Current Metrics

| Metric | Value |
| --- | ---: |
| Recorded runs | 245 |
| Passing runs | 182 |
| Partial runs | 54 |
| Failing runs | 9 |
| Requirement robustness runs | 12 |
| Requirement robustness pass | 12 |
| Failure-recovery cases | 10 |
| Failure-recovery runs | 14 |
| Prompt-sufficiency oracle-assisted baseline runs | 84 |
| Prompt-sufficiency oracle-assisted baseline pass | 84 |
| Prompt-only blind matrix rows | 51 |
| Prompt-only blind pass rows | 24 |
| Prompt-only blind clarification-needed rows | 4 |
| Prompt-only blind partial rows | 20 |
| Prompt-only blind fail rows | 3 |
| L3R ablation rows | 39 |

## Research State

반복성, 요구사항 견고성, 실패 복구, 프롬프트 충분성, L3R ablation 실험은 현재 scope에서 모두 닫혔다.

주요 결론:

- Repeatability: 26 repeat runs 중 24 clean pass, 2 run-local failure는 기록 후 수정됐다.
- Requirement robustness: 12/12 pass. 요구사항 문장 스타일이 달라도 convention drift는 관찰되지 않았다.
- Failure recovery: 10 hard cases가 validator-clean 상태로 기록됐다. Case-design gap과 validator blind spot도 구분해 수정했다.
- Prompt sufficiency baseline: oracle-assisted 84 rows는 모두 pass다. 하지만 이는 약한 prompt만으로 충분하다는 증거가 아니라 harness/oracle-assisted evidence다.
- Prompt-only blind baseline: Korean 36 rows에서 L5는 12/12 pass, 원본 L3/L1은 partial/fail이 뚜렷했다.
- L3R follow-up: 12/12 pass. 원본 L3의 약점을 compact repaired checklist가 복구했다.
- Manager-authz L3Q: 3/3 clarification_needed. Store/manager ownership ambiguity에서는 구현을 멈추는 것이 올바른 결과였다.
- L3R ablation: fragile cluster와 robust/robust-leaning cluster를 분리했다.

## L3R Ablation Conclusion

Confirmed fragile clusters:

- `cancel-waiting`: rejection policy, rollback requirement, explicit test checklist
- `manager-authz`: authn/authz split and unauthenticated rejection evidence
- `concurrent-login`: different-member token-retention evidence
- `waiting-rank`: own-reservation/slot policy and my-list status/rank read model

Confirmed robust or robust-leaning clusters:

- `manager-authz`: store model wording, service/test boundary wording
- `concurrent-login`: server-side active-session storage wording
- `concurrent-login`: race/atomicity wording은 5/6 pass로 robust-leaning이나 real concurrent test 문구는 유지해야 안전하다.
- `waiting-rank`: DB or equivalent final duplicate guard wording

상세 보고서:

- `benchmarks/reports/prompt-sufficiency-l3r-ablation-closeout.md`
- `benchmarks/reports/prompt-sufficiency-l3r-ablation-report.md`

## Cleanup And Localization

Repo 공개 전 정리를 수행했다.

- 전체 백업: `/Users/lhs/Desktop/harness-backup-20260705-014446`
- 제거: `tmp/`, `chapter-notes/`, `exam.pdf`, nested checkout, local app state, all `build/` and `.gradle/`
- 정리 후 repo 크기: 226M
- README와 reports는 한국어로 전환했다.
- CSV 헤더, run_id, status enum, category name, prompt/oracle 원문은 스크립트 계약 때문에 영어를 유지한다.

## Stop Condition

현재 연구 scope는 종료해도 된다.

후속 작업은 선택 사항이다.

- L3R vNext를 새 feature family에 적용
- 영어 transfer of L3R/L3Q 수행
- 다른 repository/domain에서 ablation 재현
- 현재 변경사항을 reviewable commit 단위로 분리
