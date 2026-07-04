# 프롬프트 충분성 보고서

## 요약

이 보고서는 junior backend developer 수준의 요구사항 prompt가 어느 정도 구체적이어야 AI-assisted Spring 구현이 안정적인지 측정한다.

두 종류의 증거를 분리한다.

1. oracle-assisted baseline: hidden oracle과 기존 benchmark context가 verifier 쪽에 존재하는 상태의 84-run baseline
2. prompt-only blind benchmark: implementation agent가 prompt와 sanitized baseline만 보고 구현하고, hidden oracle은 구현 후 verifier만 보는 상태

## Oracle-Assisted Baseline

- 계획: 84 rows
- 완료: 84 rows
- 결과: 84/84 pass
- 언어: 한국어 42, 영어 42
- feature: `cancel-waiting`, `manager-authz`, `concurrent-login`, `waiting-rank`
- prompt level: L5-L1 또는 L5/L3/L1 probe
- skill/reference/evaluator/oracle intervention: 없음

이 결과는 harness, hidden oracle, stable target, verification command가 함께 있을 때 degraded prompt도 통과 가능한 구현으로 수렴할 수 있음을 보여준다.

하지만 약한 prompt만으로 충분하다는 증거는 아니다. hidden oracle과 prior target/source가 같은 repository에 있었기 때문이다.

## Prompt-Only Blind Baseline

Korean prompt-only blind baseline 36 rows를 완료했다.

| Prompt level | 결과 |
| --- | --- |
| L5 | 12/12 pass |
| L3 | 11 partial, 1 clarification_needed |
| L1 | 9 partial, 3 fail |

해석:

- L5는 prompt-only blind에서도 안정적이었다.
- 원본 L3는 대부분 구현은 되지만 hidden oracle의 negative path, cross-principal rule, read model evidence를 자주 놓쳤다.
- L1은 partial과 fail이 섞였고, 일부 feature에서는 잘못된 문제 해석까지 발생했다.

## L3 Repair Follow-Up

원본 L3의 실패 경계를 보완하기 위해 compact repaired checklist인 L3R을 설계했다.

결과:

- L3R: 12/12 pass
- manager-authz L3Q clarification gate: 3/3 clarification_needed

해석:

- L3R은 원본 L3의 partial 경계를 실질적으로 복구했다.
- L3Q는 store/manager ownership처럼 prompt만으로 결정하면 위험한 경우 구현을 멈추고 질문하게 만들었다.
- 원본 L3와 L3R은 같은 success rate로 합치면 안 된다. L3R은 intervention이다.

## L3R Ablation

L3R에서 어떤 문장 cluster가 repair effect를 만드는지 별도 ablation으로 분석했다.

상세 결론은 다음 문서를 본다.

- `benchmarks/reports/prompt-sufficiency-l3r-ablation-closeout.md`
- `benchmarks/reports/prompt-sufficiency-l3r-ablation-report.md`

핵심만 요약하면, fragile cluster는 verifier-visible behavior와 required evidence를 직접 말하는 문장이다.

- cancellation rejection policy
- transaction/rollback evidence
- authn/authz split
- other-member token preservation
- available-slot and own-reservation waiting rejection
- my-list status/rank read model
- explicit test checklist

robust 또는 robust-leaning cluster는 domain/architecture 구조를 넓게 암시하는 문장이다.

- store ownership model
- service boundary
- server-side active-session state
- final duplicate guard
- race/atomicity wording은 5/6 pass였지만 real concurrent test 문구는 유지하는 편이 안전하다.

## 결론

현재 가장 방어 가능한 주장은 다음과 같다.

> 약한 prompt가 자동으로 충분한 것은 아니다. 하지만 hidden oracle에서 자주 누락되는 behavior/evidence를 compact checklist로 명시하면, prompt-only blind 환경에서도 L3 수준 prompt를 L5에 가까운 안정성으로 끌어올릴 수 있다.

다음 연구를 한다면 English transfer of L3R, 새 feature family validation, 또는 더 낮은 context L0 실험이 자연스럽다.
