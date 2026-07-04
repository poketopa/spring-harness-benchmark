# Waiting Rank Prompt Sufficiency Case (KO L3R)

## Case Metadata

- Feature: `waiting-rank`
- Prompt language: `ko`
- Prompt level: `L3R`
- Oracle: `benchmarks/prompt-sufficiency-cases/roomescape/oracles/waiting-rank.md`
- Baseline path: `benchmarks/roomescape-jpa-auth-base-v2`
- Repeats: `001`, `002`, `003`
- Status: `planned`

## Purpose

This repaired L3 case tests whether adding the missing slot-policy and persistence-guard checklist closes the observed L3 blind gaps.

## Implementation Prompt Payload

Use only this section as the implementation prompt for prompt-sufficiency blind repair runs.

예약이 이미 차 있는 날짜/시간/테마 슬롯에만 대기를 신청할 수 있게 하고, 내가 몇 번째 대기자인지 보여 주세요. 예약 가능한 빈 슬롯에는 대기할 수 없고, 본인의 예약에는 대기할 수 없으며, 같은 회원은 같은 슬롯에 중복 대기할 수 없어야 합니다. 대기 순번은 신청 순서와 결정적인 tie-breaker로 계산하고, 내 예약 목록에서는 예약과 대기를 함께 보여 주며 대기 항목에는 현재 순번을 표시해 주세요. 중복 대기는 서비스 검증뿐 아니라 DB 제약 또는 동등한 최종 가드로 보호해 주세요. occupied slot waiting, available slot rejection, own reservation rejection, duplicate waiting rejection, rank ordering, my list response, final duplicate guard 테스트와 전체 Gradle 테스트를 실행해 주세요.

## Evaluation Notes

- Do not inspect sibling prompt levels before completing a run.
- Do not update the skill/reference during the blind repair set.
- Record whether the compact checklist removes the historical available-slot, own-reservation, and final-guard gaps.
