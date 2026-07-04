# 요구사항 견고성 보고서

## 요약

`spring-usecase-implementation`이 같은 의미의 Roomescape 요구사항을 서로 다른 문장 스타일로 받아도 같은 backend convention을 유지하는지 측정했다.

결과는 안정적이다. 12개 requirement-robustness run이 모두 narrow verification, `./gradlew clean test`, convention comparison, semantic gate review, benchmark record validation을 통과했다.

## 범위

| Unit | Variant | Baseline | Target root |
| --- | --- | --- | --- |
| `c1-waiting` | narrative, acceptance-criteria, api-contract | `benchmarks/roomescape-jpa-auth-base-v2` | `benchmarks/robustness-runs/` |
| `c2-combined` | narrative, domain-rule, api-contract | `benchmarks/roomescape-jpa-auth-cycle1-regen-v2` | `benchmarks/robustness-runs/` |
| `manager-authz` | narrative, security-case, api-contract | `benchmarks/roomescape-jpa-auth-cycle2-regen-v2` | `benchmarks/robustness-runs/` |
| `concurrent-login` | narrative, domain-rule, operational-policy | `benchmarks/roomescape-jpa-auth-manager-authorization-regen-v2` | `benchmarks/robustness-runs/` |

## 결과

| 항목 | 값 |
| --- | ---: |
| robustness run | 12 |
| pass | 12 |
| full verification failure | 0 |
| P0/P1 convention failure | 0 |
| semantic drift finding | 0 |
| validator failure | 0 |
| skill/reference update | 0 |

## 관찰된 안정 결정

- Waiting은 `WaitingService`, 직접 rank query, combined my-reservation read mapping을 유지했다.
- Cycle2 behavior는 reservation lifecycle rule을 `ReservationService`, promotion orchestration을 `WaitingPromotionService`에 유지했다.
- Manager authorization은 authentication과 authorization을 분리하고 manager/store check를 service boundary에 둔다.
- Concurrent login은 newest-login-wins policy, server-side auth session, stale-token rejection, real concurrent verification을 유지한다.

## 결론

현재 skill/reference set은 테스트한 네 요구사항 family와 세 문장 스타일에서 안정적이다. requirement robustness 12-run inclusion bar를 충족했고, paraphrase 요구사항에서도 convention drift가 관찰되지 않았다.
