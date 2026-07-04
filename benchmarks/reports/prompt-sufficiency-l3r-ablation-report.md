# L3R Ablation 상세 보고서

## Snapshot

- 상태: screening, partial-axis confirmatory, robust-axis confirmatory, race tie-breaker 완료
- 전체 L3R ablation rows: 39
- Screening: 12 rows, 5 pass / 7 partial
- Partial-axis confirmatory: 14 rows, 14 partial
- Robust-axis confirmatory: 10 rows, 9 pass / 1 partial
- Race tie-breaker: 3 rows, 3 pass
- Implementation isolation: sanitized `/tmp` project plus prompt payload only
- Verifier isolation: hidden oracle read only after implementation stops
- Intervention: skill, reference, evaluator, oracle 변경 없음

## 연구 질문

L3R prompt의 repair effect는 어떤 compact prompt element가 만들었는가?

각 row는 L3R prompt에서 하나의 정보 cluster를 제거하고, prompt-only blind isolation에서 L3R pass가 다시 `partial`, `fail`, `clarification_needed`로 떨어지는지 확인했다.

## Screening 결과

| Feature | Removed cluster | Result | Signal |
| --- | --- | --- | --- |
| `cancel-waiting` | past-cancel and other-member rejection policy | partial | past-cancel rejection과 direct rejection-policy test evidence가 빠졌다. |
| `cancel-waiting` | single transaction and rollback requirement | partial | core behavior는 살아남았지만 rollback-failure evidence가 빠졌다. |
| `cancel-waiting` | explicit required test checklist | partial | behavior는 대체로 살아남았지만 rollback-test evidence가 빠졌다. |
| `manager-authz` | explicit Store/Theme/Manager ownership model | pass | compatible store ownership model을 추론했다. |
| `manager-authz` | unauthenticated vs unauthorized error split | partial | non-manager가 wrong auth boundary로 mapping됐다. |
| `manager-authz` | explicit service boundary and test checklist | pass | service boundary와 acceptance coverage가 살아남았다. |
| `concurrent-login` | other-member token preservation | partial | newest-login behavior는 살아남았지만 other-member retention evidence가 빠졌다. |
| `concurrent-login` | real concurrent login/race handling and 500 avoidance | pass | locking과 real concurrent coverage를 추론했다. |
| `concurrent-login` | explicit server-side active-session storage | pass | server-side state와 concurrency protection을 추론했다. |
| `waiting-rank` | available-slot and own-reservation rejection policy | partial | waiting이 reservation creation으로 접혔고 key rejection policy가 빠졌다. |
| `waiting-rank` | combined my-list status/rank response requirement | partial | waiting creation/rank는 살아남았지만 my-list read model이 빠졌다. |
| `waiting-rank` | DB or equivalent final duplicate guard | pass | final duplicate guard를 추론했다. |

## Partial-Axis Confirmatory

Screening에서 partial이 된 7개 axis만 repeat `002`, `003`으로 반복했다.

| Feature | Axis | 001 | 002 | 003 | 결론 |
| --- | --- | --- | --- | --- | --- |
| `cancel-waiting` | `no-rejection-policy` | partial | partial | partial | past-cancel policy는 fragile하다. |
| `cancel-waiting` | `no-transaction-rollback` | partial | partial | partial | rollback-failure evidence는 fragile하다. |
| `cancel-waiting` | `no-test-checklist` | partial | partial | partial | explicit checklist는 fragile하다. |
| `manager-authz` | `no-authn-authz-split` | partial | partial | partial | authn/authz split은 fragile하다. |
| `concurrent-login` | `no-other-member` | partial | partial | partial | other-member retention evidence는 fragile하다. |
| `waiting-rank` | `no-slot-policy` | partial | partial | partial | own-reservation/slot policy는 fragile하다. |
| `waiting-rank` | `no-my-list` | partial | partial | partial | my-list status/rank read model은 fragile하다. |

## Robust-Axis Confirmatory

Screening에서 pass였던 5개 axis를 repeat `002`, `003`으로 반복했다.

| Feature | Axis | 001 | 002 | 003 | 결론 |
| --- | --- | --- | --- | --- | --- |
| `manager-authz` | `no-store-model` | pass | pass | pass | store ownership wording은 robust하다. |
| `manager-authz` | `no-service-test-boundary` | pass | pass | pass | service/test boundary wording은 robust하다. |
| `concurrent-login` | `no-race-atomicity` | pass | pass | partial | tie-breaker가 필요했다. |
| `concurrent-login` | `no-server-state` | pass | pass | pass | server-side-state wording은 robust하다. |
| `waiting-rank` | `no-final-guard` | pass | pass | pass | final duplicate guard wording은 robust하다. |

## Race Tie-Breaker

`concurrent-login` / `no-race-atomicity`가 `pass, pass, partial`이었기 때문에 추가 repeat `004-006`을 실행했다.

| Axis | 004 | 005 | 006 | 최종 신호 |
| --- | --- | --- | --- | --- |
| `no-race-atomicity` | pass | pass | pass | 001-006 중 5/6 pass. robust-leaning이지만 test-evidence-sensitive하다. |

## 최종 신호

Confirmed fragile:

- `cancel-waiting`: rejection policy, rollback requirement, explicit test checklist
- `manager-authz`: authn/authz split and unauthenticated rejection evidence
- `concurrent-login`: different-member token-retention evidence
- `waiting-rank`: own-reservation/slot policy and my-list status/rank read model

Confirmed robust or robust-leaning:

- `manager-authz`: explicit store model wording, service/test boundary wording
- `concurrent-login`: explicit server-side active-session storage wording
- `concurrent-login`: race/atomicity wording은 5/6 pass로 robust-leaning이나 real-concurrent-test 문구는 유지하는 편이 안전하다.
- `waiting-rank`: DB or equivalent final duplicate guard wording

## 결론

L3R에서 삭제하면 위험한 것은 "무엇을 만들라"는 넓은 domain noun보다, oracle이 직접 확인하는 negative path, cross-principal preservation, response shape, rollback/concurrency evidence다.

따라서 L3R vNext는 robust structure를 압축하되 fragile evidence는 명시적으로 유지해야 한다.
