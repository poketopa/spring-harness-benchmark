# Concurrent Login Prompt Sufficiency Case (KO L3RA No Race Atomicity)

## Case Metadata

- Feature: `concurrent-login`
- Prompt language: `ko`
- Prompt level: `L3RA`
- Ablation axis: `no-race-atomicity`
- Removed L3R element: real concurrent login race handling and 500-avoidance
- Oracle: `benchmarks/prompt-sufficiency-cases/roomescape/oracles/concurrent-login.md`
- Baseline path: `benchmarks/roomescape-jpa-auth-manager-authorization-regen-v2`
- Repeats: `001`
- Status: `planned`

## Purpose

This ablation removes explicit race/atomicity wording from L3R while keeping latest-login, stale-token, other-member, server-state, and test evidence instructions.

## Implementation Prompt Payload

Use only this section as the implementation prompt for prompt-only blind ablation runs.

한 계정에서 여러 번 로그인하면 가장 최근 로그인만 유효하게 해 주세요. 같은 회원의 예전 토큰은 의도한 인증 실패로 막고, 다른 회원의 토큰은 영향을 받지 않아야 합니다. active session을 서버 쪽 상태나 동등한 저장 구조로 검증해 주세요. 새 토큰 성공, 이전 토큰 실패, 다른 회원 토큰 유지, invalid token 처리 테스트와 전체 Gradle 테스트를 실행해 주세요.

## Evaluation Notes

- Do not expose hidden oracle text or path to the implementation agent.
- Compare concurrent test evidence against L3R after implementation.
