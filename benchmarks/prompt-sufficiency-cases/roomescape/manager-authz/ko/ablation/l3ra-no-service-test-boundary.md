# Manager Authorization Prompt Sufficiency Case (KO L3RA No Service/Test Boundary)

## Case Metadata

- Feature: `manager-authz`
- Prompt language: `ko`
- Prompt level: `L3RA`
- Ablation axis: `no-service-test-boundary`
- Removed L3R element: explicit service boundary and test checklist
- Oracle: `benchmarks/prompt-sufficiency-cases/roomescape/oracles/manager-authz.md`
- Baseline path: `benchmarks/roomescape-jpa-auth-cycle2-regen-v2`
- Repeats: `001`
- Status: `planned`

## Purpose

This ablation keeps the Store ownership and auth failure policy but removes explicit architecture and named test checklist instructions.

## Implementation Prompt Payload

Use only this section as the implementation prompt for prompt-only blind ablation runs.

매장 매니저 인가 기능을 추가해 주세요. 이 요구사항에서 매장은 예약 가능한 테마/시간을 소유하는 별도 Store 개념이며, Theme는 Store에 속하고 Manager는 자신이 관리하는 Store의 예약만 조회/변경/삭제할 수 있어야 합니다. 비로그인은 인증 실패, 로그인했지만 매니저가 아닌 사용자는 인가 실패, 다른 Store 예약 접근도 인가 실패로 처리하고 기존 에러 응답 스타일을 유지해 주세요. 필요한 테스트와 전체 Gradle 테스트를 실행해 주세요.

## Evaluation Notes

- Do not expose hidden oracle text or path to the implementation agent.
- Compare service-boundary and test evidence against L3R after implementation.
