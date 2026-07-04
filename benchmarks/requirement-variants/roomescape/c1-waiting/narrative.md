# c1-waiting narrative Requirement Variant

## Variant Metadata

- Unit: c1-waiting
- Variant type: narrative
- Canonical requirement: `missions/roomescape-reservation-waiting/requirements/base/02-cycle1-waiting.md`
- Semantic checklist: `benchmarks/requirement-variants/checklists/c1-waiting.md`
- Status: self-reviewed candidate

## Requirement

방탈출 예약 서비스에서 사용자가 원하는 날짜, 시간, 테마 조합이 이미 다른 사람에게 예약되어 있어도 이제는 포기하지 않고 대기할 수 있어야 한다. 하나의 예약 가능 단위는 날짜, 시간, 테마로 결정되는 슬롯이며, 같은 슬롯에 여러 사용자가 대기하면 신청한 순서대로 순번이 정해진다.

같은 사용자가 같은 슬롯에 반복해서 대기를 넣는 것은 허용하지 않는다. 사용자는 자신이 넣은 대기 신청을 취소할 수 있어야 한다.

기존의 "내 예약 목록" 기능은 예약만 보여주는 수준에서 끝나면 안 된다. 사용자가 가지고 있는 예약과 대기를 한 화면/API에서 함께 확인할 수 있어야 하며, 각 항목은 예약인지 대기인지 상태로 구분되어야 한다. 대기 항목에는 현재 본인의 대기 순번도 포함되어야 한다.

구현하면서 예약과 대기가 도메인에서 어떤 관계인지 분명하게 드러내고, 대기 순번을 어떻게 계산하거나 저장할지 결정한다. 새로 추가하는 API와 결정한 명세는 대상 프로젝트의 README에 정리한다.

이번 기능에 대해 요구사항 테스트를 작성한다. 대기 신청, 중복 대기 거부, 대기 취소, 내 예약/대기 통합 조회와 순번 표시가 검증되어야 한다.

## Semantic Equivalence Review

- [x] No required behavior was removed.
- [x] No required behavior was weakened.
- [x] No new required behavior was added.
- [x] Ambiguous wording was avoided or intentionally recorded.
- [x] API, test, error, and transaction expectations remain equivalent to the canonical requirement.

## Notes

- Intended wording difference: product-owner narrative.
- Risk of semantic drift: low.

