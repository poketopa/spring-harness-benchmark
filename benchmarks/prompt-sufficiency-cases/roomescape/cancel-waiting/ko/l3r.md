# Cancel Waiting Prompt Sufficiency Case (KO L3R)

## Case Metadata

- Feature: `cancel-waiting`
- Prompt language: `ko`
- Prompt level: `L3R`
- Oracle: `benchmarks/prompt-sufficiency-cases/roomescape/oracles/cancel-waiting.md`
- Baseline path: `benchmarks/roomescape-jpa-auth-cycle1-regen-v2`
- Repeats: `001`, `002`, `003`
- Status: `planned`

## Purpose

This repaired L3 case tests whether a compact checklist can close the observed L3 blind gap without using the full L5 prose.

## Implementation Prompt Payload

Use only this section as the implementation prompt for prompt-sufficiency blind repair runs.

예약 취소와 예약 대기 처리를 연결해 주세요. 사용자가 본인의 예약을 취소하면 같은 날짜/시간/테마의 가장 빠른 대기 1명이 예약으로 승격되어야 합니다. 지난 예약 취소와 다른 사용자 예약 취소는 기존 정책에 맞게 거절하고, 예약 삭제/대기 삭제/예약 승격은 하나의 트랜잭션으로 처리해 승격 실패 시 전체 변경이 롤백되게 해 주세요. 남은 대기 순번은 신청 순서 기준으로 다시 계산되어야 합니다. 정책은 Controller가 아니라 Service/usecase 계층에 두고, own cancel, past cancel rejection, other-member rejection, promotion, rank recalculation, rollback 테스트와 전체 Gradle 테스트를 실행해 주세요.

## Evaluation Notes

- Do not inspect sibling prompt levels before completing a run.
- Do not update the skill/reference during the blind repair set.
- Record whether the compact checklist removes the historical L3 past-cancel and rollback-test gaps.
