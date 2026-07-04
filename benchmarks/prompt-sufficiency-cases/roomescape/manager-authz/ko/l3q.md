# Manager Authorization Prompt Sufficiency Case (KO L3Q)

## Case Metadata

- Feature: `manager-authz`
- Prompt language: `ko`
- Prompt level: `L3Q`
- Oracle: `benchmarks/prompt-sufficiency-cases/roomescape/oracles/manager-authz.md`
- Baseline path: `benchmarks/roomescape-jpa-auth-cycle2-regen-v2`
- Repeats: `001`, `002`, `003`
- Status: `planned`

## Purpose

This clarification-gated L3 case tests whether an implementation agent stops instead of inventing store ownership when the product boundary is missing.

## Implementation Prompt Payload

Use only this section as the implementation prompt for prompt-sufficiency blind clarification-gate runs.

매장 매니저는 자기 매장 예약만 관리할 수 있게 해 주세요. 다른 매장 예약이나 일반 회원 접근은 막아야 합니다. 기존 구조에 맞게 테스트도 추가해 주세요.

중요: 현재 프로젝트 구조만으로 "매장"이 어떤 도메인인지, 예약이 매장에 어떻게 속하는지, 매니저와 매장의 소유 관계를 어떻게 판단해야 하는지 명확하지 않다면 임의로 Theme/Admin/본인 예약 정책으로 해석하지 말고 구현을 멈춘 뒤 필요한 제품 정책 질문을 남겨 주세요.

## Evaluation Notes

- Do not inspect sibling prompt levels before completing a run.
- Do not update the skill/reference during the blind repair set.
- The desired outcome is `clarification_needed` when the target project lacks a clear Store/Manager ownership model.
