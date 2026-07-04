# Cancel Waiting Prompt Sufficiency Case (KO L3RA No Rejection Policy)

## Case Metadata

- Feature: `cancel-waiting`
- Prompt language: `ko`
- Prompt level: `L3RA`
- Ablation axis: `no-rejection-policy`
- Removed L3R element: past-cancel and other-member rejection policy
- Oracle: `benchmarks/prompt-sufficiency-cases/roomescape/oracles/cancel-waiting.md`
- Baseline path: `benchmarks/roomescape-jpa-auth-cycle1-regen-v2`
- Repeats: `001`
- Status: `planned`

## Purpose

This ablation removes the explicit negative-path cancellation policy from L3R while keeping promotion, transaction, service-boundary, rank, and test evidence instructions.

## Implementation Prompt Payload

Use only this section as the implementation prompt for prompt-only blind ablation runs.

예약 취소와 예약 대기 처리를 연결해 주세요. 예약이 취소되면 같은 날짜/시간/테마의 가장 빠른 대기 1명이 예약으로 승격되어야 합니다. 예약 삭제/대기 삭제/예약 승격은 하나의 트랜잭션으로 처리해 승격 실패 시 전체 변경이 롤백되게 해 주세요. 남은 대기 순번은 신청 순서 기준으로 다시 계산되어야 합니다. 정책은 Controller가 아니라 Service/usecase 계층에 두고, own cancel, promotion, rank recalculation, rollback 테스트와 전체 Gradle 테스트를 실행해 주세요.

## Evaluation Notes

- Do not expose hidden oracle text or path to the implementation agent.
- Compare against original L3 and L3R after implementation.
