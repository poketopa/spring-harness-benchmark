# Waiting Rank Prompt Sufficiency Case (KO L3RA No My List)

## Case Metadata

- Feature: `waiting-rank`
- Prompt language: `ko`
- Prompt level: `L3RA`
- Ablation axis: `no-my-list`
- Removed L3R element: combined my-list status/rank response requirement
- Oracle: `benchmarks/prompt-sufficiency-cases/roomescape/oracles/waiting-rank.md`
- Baseline path: `benchmarks/roomescape-jpa-auth-base-v2`
- Repeats: `001`
- Status: `planned`

## Purpose

This ablation keeps waiting creation policy, rank, duplicate rejection, and final guard but removes the explicit my-list read-model requirement.

## Implementation Prompt Payload

Use only this section as the implementation prompt for prompt-only blind ablation runs.

예약이 이미 차 있는 날짜/시간/테마 슬롯에만 대기를 신청할 수 있게 하고, 내가 몇 번째 대기자인지 보여 주세요. 예약 가능한 빈 슬롯에는 대기할 수 없고, 본인의 예약에는 대기할 수 없으며, 같은 회원은 같은 슬롯에 중복 대기할 수 없어야 합니다. 대기 순번은 신청 순서와 결정적인 tie-breaker로 계산해 주세요. 중복 대기는 서비스 검증뿐 아니라 DB 제약 또는 동등한 최종 가드로 보호해 주세요. occupied slot waiting, available slot rejection, own reservation rejection, duplicate waiting rejection, rank ordering, final duplicate guard 테스트와 전체 Gradle 테스트를 실행해 주세요.

## Evaluation Notes

- Do not expose hidden oracle text or path to the implementation agent.
- Compare read-model behavior against L3R after implementation.
