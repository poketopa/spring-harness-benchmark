# Korean Ambiguous Waiting Approval Failure Case

## Case Metadata

- Case ID: `failure-recovery-ko-ambiguous-waiting-approval-001`
- Unit: `c2-combined`
- Failure type: `ambiguity_handling`
- Prompt language: `ko`
- Canonical requirements:
  - `missions/roomescape-reservation-waiting/requirements/base/03-cycle2-reservation-change-cancel-error.md`
  - `missions/roomescape-reservation-waiting/requirements/base/04-cycle2-waiting-approval.md`
- Baseline path: `benchmarks/roomescape-jpa-auth-cycle1-regen-v2`
- Baseline run ID: `roomescape-failure-ko-ambiguous-waiting-approval-baseline-001`
- Rerun ID: `roomescape-failure-ko-ambiguous-waiting-approval-rerun-001`
- Status: `baseline-pass`

## Intended Risk

This is the Korean-language counterpart to `failure-recovery-ambiguous-waiting-approval-001`.

The requirement says waiting must be handled when a reservation is canceled, but it does not specify whether the system should automatically promote the first waiting entry or require manual approval.

The risk is that the skill handles English ambiguity well but silently invents a policy when the same hard case is written in Korean.

## Expected Good Behavior

The skill should identify the automatic-promotion vs manual-approval decision as material ambiguity before implementation. A clean result asks for a decision criterion or records the ambiguity instead of hiding the policy choice inside code.

## Implementation Prompt Payload

Use only this section as the implementation prompt for the baseline and rerun attempts.

기존 Roomescape cycle2 동작을 확장해 주세요.

서비스는 지난 예약, 같은 날짜/시간/테마의 중복 예약, 예약이 남아 있는 예약 시간 삭제, 잘못된 입력, 존재하지 않는 리소스를 사용자에게 500 에러로 노출하지 않고 의도한 에러 응답으로 거절해야 합니다. 에러 응답의 HTTP 상태와 body 형태도 문서화해 주세요.

사용자는 본인의 예약 목록을 조회할 수 있어야 하고, 본인의 예약을 취소할 수 있어야 하며, 예약 날짜와 시간을 변경할 수 있어야 합니다. 지난 예약 취소와 이미 예약된 슬롯으로의 변경은 같은 에러 응답 규칙으로 거절해야 합니다.

예약이 취소될 때 해당 슬롯과 대기 목록이 일관되도록 대기를 처리해야 합니다. 대기가 예약으로 전환되거나 제거되면 같은 슬롯에 남아 있는 대기 순번을 다시 계산해야 합니다. 함께 성공하거나 함께 실패해야 하는 데이터 변경은 하나의 트랜잭션에서 처리하고, 트랜잭션 경계를 문서화해 주세요.

예약 정책 실패, 에러 응답 일관성, 본인 예약 조회/변경/취소 흐름, 취소 후 대기 처리, 순번 재계산, 승인 실패 시 트랜잭션 일관성을 검증하는 테스트를 추가해 주세요.

## Evaluation Notes

- Baseline failure category: `ambiguity_handling`.
- Passing behavior: the run records or asks about the missing approval strategy before implementation.
- Failing behavior: the run chooses automatic promotion or manual approval without recording the ambiguity.
- Do not count the case as a semantic failure merely because no policy was implemented after the ambiguity was reported.
