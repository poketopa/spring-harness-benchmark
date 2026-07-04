# Concurrent Login Prompt Sufficiency Case (KO L3RA No Server State)

## Case Metadata

- Feature: `concurrent-login`
- Prompt language: `ko`
- Prompt level: `L3RA`
- Ablation axis: `no-server-state`
- Removed L3R element: explicit server-side active-session storage instruction
- Oracle: `benchmarks/prompt-sufficiency-cases/roomescape/oracles/concurrent-login.md`
- Baseline path: `benchmarks/roomescape-jpa-auth-manager-authorization-regen-v2`
- Repeats: `001`
- Status: `planned`

## Purpose

This ablation removes the implementation-mechanism hint from L3R while keeping the externally visible latest-login behavior, independence, race handling, and test evidence instructions.

## Implementation Prompt Payload

Use only this section as the implementation prompt for prompt-only blind ablation runs.

한 계정에서 여러 번 로그인하면 가장 최근 로그인만 유효하게 해 주세요. 같은 회원의 예전 토큰은 의도한 인증 실패로 막고, 다른 회원의 토큰은 영향을 받지 않아야 합니다. 같은 회원이 동시에 로그인하는 race에서도 DB 제약 예외가 500으로 노출되지 않게 처리해 주세요. 새 토큰 성공, 이전 토큰 실패, 다른 회원 토큰 유지, 실제 동시 로그인 테스트(예: latch/barrier/executor 사용), invalid token 처리 테스트와 전체 Gradle 테스트를 실행해 주세요.

## Evaluation Notes

- Do not expose hidden oracle text or path to the implementation agent.
- Compare authentication design against L3R after implementation.
