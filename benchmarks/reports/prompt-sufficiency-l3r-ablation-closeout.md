# L3R Ablation 최종 정리

## 상태

현재 Roomescape benchmark scope에서 L3R ablation 프로젝트는 완료됐다.

질문은 다음이었다.

> L3R prompt에서 어떤 정보 cluster를 제거하면 다시 `partial`, `fail`, `clarification_needed`로 떨어지는가?

증거 세트:

| Phase | Matrix | Rows | Result |
| --- | --- | ---: | --- |
| Screening | `benchmarks/benchmark-records/prompt-sufficiency-l3r-ablation-matrix.csv` | 12 | 5 pass, 7 partial |
| Partial-axis confirmatory | `benchmarks/benchmark-records/prompt-sufficiency-l3r-ablation-confirmatory-matrix.csv` | 14 | 14 partial |
| Robust-axis confirmatory | `benchmarks/benchmark-records/prompt-sufficiency-l3r-ablation-robust-confirmatory-matrix.csv` | 10 | 9 pass, 1 partial |
| Race tie-breaker | `benchmarks/benchmark-records/prompt-sufficiency-l3r-ablation-race-tiebreaker-matrix.csv` | 3 | 3 pass |

모든 row는 원본 L3, L3R, L3Q 기록과 분리했다. Implementation agent는 sanitized `/tmp` workspace와 prompt payload만 받았다. Hidden oracle review는 구현이 끝난 뒤 verifier 쪽에서만 수행했다. Skill, reference, evaluator, oracle은 변경하지 않았다.

최종 검증:

- `python3 scripts/validate-benchmark-records.py`: 0 warnings
- `python3 scripts/generate-benchmark-metrics.py --check`: up to date

## 답

제거했을 때 일관되게 regression을 만든 요소는 넓은 domain noun보다 verifier가 직접 확인하는 behavior와 required evidence였다.

Confirmed fragile cluster:

| Feature | Axis | Evidence | 해석 |
| --- | --- | --- | --- |
| `cancel-waiting` | `no-rejection-policy` | 3/3 partial | past-cancel과 direct rejection policy는 명시해야 한다. |
| `cancel-waiting` | `no-transaction-rollback` | 3/3 partial | rollback-failure evidence는 명시해야 한다. |
| `cancel-waiting` | `no-test-checklist` | 3/3 partial | explicit test checklist가 repair effect를 가진다. |
| `manager-authz` | `no-authn-authz-split` | 3/3 partial | authentication과 authorization status/evidence는 명시해야 한다. |
| `concurrent-login` | `no-other-member` | 3/3 partial | different-member token preservation은 명시해야 한다. |
| `waiting-rank` | `no-slot-policy` | 3/3 partial | available-slot과 own-reservation rejection은 명시해야 한다. |
| `waiting-rank` | `no-my-list` | 3/3 partial | my-list status/rank read model은 명시해야 한다. |

Confirmed robust 또는 robust-leaning cluster:

| Feature | Axis | Evidence | 해석 |
| --- | --- | --- | --- |
| `manager-authz` | `no-store-model` | 3/3 pass | agent가 explicit enough store ownership model을 추론했다. |
| `manager-authz` | `no-service-test-boundary` | 3/3 pass | service-boundary/test coverage는 해당 cluster 없이도 자주 살아남았다. |
| `concurrent-login` | `no-server-state` | 3/3 pass | latest-login behavior에서 server-side active-session state를 추론했다. |
| `waiting-rank` | `no-final-guard` | 3/3 pass | DB 또는 equivalent duplicate guard를 추론했다. |
| `concurrent-login` | `no-race-atomicity` | 5/6 pass | robust-leaning이지만 test-evidence-sensitive하다. |

## L3R vNext

다음 L3R prompt는 단순히 더 짧아지면 안 된다. Fragile evidence에는 단어를 쓰고, robust structure는 압축해야 한다.

명시적으로 유지할 요소:

- past-cancel, other-member access, available-slot waiting, own-reservation waiting처럼 오분류하기 쉬운 rejection policy
- unauthenticated와 authenticated-but-forbidden의 status 차이
- same member old token fail, other member token remains valid 같은 cross-principal preservation
- my-list status/rank처럼 oracle이 보는 response shape
- transaction/rollback처럼 failure-mode evidence가 중요한 항목
- concurrent-login의 real latch/barrier/executor test

압축 가능한 요소:

- manager-store ownership model
- service boundary
- server-side active-session storage
- DB 또는 equivalent final duplicate guard

권장 skeleton:

```text
기존 API와 error-response style을 유지하며 feature를 구현한다.

Behavior:
- positive path
- actor/resource boundary가 있는 rejection policy
- cross-principal preservation rule
- 사용자가 관찰하는 response shape

Architecture:
- domain policy는 controller에 두지 않는다.
- authorization/session/waiting policy는 service boundary 뒤에 둔다.
- service 아래 계층에는 request DTO를 넘기지 않는다.
- duplicate/race-sensitive invariant에는 DB 또는 equivalent final guard를 둔다.

Tests:
- positive path
- 각 rejection policy
- cross-principal preservation
- read model response shape
- rollback/failure behavior
- concurrency-sensitive behavior는 latch/barrier/executor 기반 real concurrent test

Verification:
- targeted test
- ./gradlew clean test
```

## Feature별 유지 가이드

`cancel-waiting`:

- past-cancel rejection 유지
- other-member cancellation rejection 유지
- transaction/rollback failure evidence 유지
- test checklist 유지

`manager-authz`:

- authn/authz split 유지
- unauthenticated rejection evidence 유지
- store ownership과 service-boundary wording은 압축 가능하지만 완전 삭제하지 않는다.

`concurrent-login`:

- other-member token preservation 유지
- real concurrent login test wording 유지
- server-side active-session storage는 압축 가능하지만 이름은 남긴다.
- race/atomicity는 robust-leaning이지만 안전하게 제거할 요소는 아니다.

`waiting-rank`:

- available-slot rejection 유지
- own-reservation waiting rejection 유지
- my-list status/rank response 유지
- final duplicate guard wording은 압축 가능하지만 이름은 남긴다.

## Stop Condition

현재 프로젝트는 여기서 멈춰도 된다.

선택적 후속 작업:

- L3R vNext를 새 prompt-family run에 적용
- 다른 domain/repository에서 ablation 재현
- benchmark-record diff를 reviewable commit 단위로 분리
