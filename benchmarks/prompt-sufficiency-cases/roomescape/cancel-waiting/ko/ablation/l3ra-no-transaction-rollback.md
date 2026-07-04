# Cancel Waiting Prompt Sufficiency Case (KO L3RA No Transaction Rollback)

## Case Metadata

- Feature: `cancel-waiting`
- Prompt language: `ko`
- Prompt level: `L3RA`
- Ablation axis: `no-transaction-rollback`
- Removed L3R element: single transaction and rollback requirement
- Oracle: `benchmarks/prompt-sufficiency-cases/roomescape/oracles/cancel-waiting.md`
- Baseline path: `benchmarks/roomescape-jpa-auth-cycle1-regen-v2`
- Repeats: `001`
- Status: `planned`

## Purpose

This ablation removes explicit transaction and rollback wording from L3R while keeping cancellation policy, promotion, rank, service-boundary, and test evidence instructions.

## Implementation Prompt Payload

Use only this section as the implementation prompt for prompt-only blind ablation runs.

예약 취소와 예약 대기 처리를 연결해 주세요. 사용자가 본인의 예약을 취소하면 같은 날짜/시간/테마의 가장 빠른 대기 1명이 예약으로 승격되어야 합니다. 지난 예약 취소와 다른 사용자 예약 취소는 기존 정책에 맞게 거절해 주세요. 남은 대기 순번은 신청 순서 기준으로 다시 계산되어야 합니다. 정책은 Controller가 아니라 Service/usecase 계층에 두고, own cancel, past cancel rejection, other-member rejection, promotion, rank recalculation 테스트와 전체 Gradle 테스트를 실행해 주세요.

## Evaluation Notes

- Do not expose hidden oracle text or path to the implementation agent.
- Compare against original L3 and L3R after implementation.
