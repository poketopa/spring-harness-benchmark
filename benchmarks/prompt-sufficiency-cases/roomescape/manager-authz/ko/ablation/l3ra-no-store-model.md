# Manager Authorization Prompt Sufficiency Case (KO L3RA No Store Model)

## Case Metadata

- Feature: `manager-authz`
- Prompt language: `ko`
- Prompt level: `L3RA`
- Ablation axis: `no-store-model`
- Removed L3R element: explicit Store/Theme/Manager ownership model
- Oracle: `benchmarks/prompt-sufficiency-cases/roomescape/oracles/manager-authz.md`
- Baseline path: `benchmarks/roomescape-jpa-auth-cycle2-regen-v2`
- Repeats: `001`
- Status: `planned`

## Purpose

This ablation removes the explicit Store domain model and ownership relation from L3R while keeping auth failure categories, service-boundary, and test evidence instructions.

## Implementation Prompt Payload

Use only this section as the implementation prompt for prompt-only blind ablation runs.

매장 매니저 인가 기능을 추가해 주세요. 매니저는 자신이 관리하는 매장의 예약만 조회/변경/삭제할 수 있어야 합니다. 비로그인은 인증 실패, 로그인했지만 매니저가 아닌 사용자는 인가 실패, 다른 매장 예약 접근도 인가 실패로 처리하고 기존 에러 응답 스타일을 유지해 주세요. 매니저-매장 관계는 테스트와 Service 코드에서 명확히 드러나게 모델링하고, 권한 정책은 Controller가 아니라 일관된 Service 경계에 두세요. own-store success, other-store rejection, non-manager rejection, unauthenticated rejection 테스트와 전체 Gradle 테스트를 실행해 주세요.

## Evaluation Notes

- Do not expose hidden oracle text or path to the implementation agent.
- A clarification question is an acceptable result if the implementation agent refuses to invent the missing store model.
