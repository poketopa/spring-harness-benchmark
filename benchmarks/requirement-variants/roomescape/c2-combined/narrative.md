# c2-combined narrative Requirement Variant

## Variant Metadata

- Unit: c2-combined
- Variant type: narrative
- Canonical requirement: `missions/roomescape-reservation-waiting/requirements/base/03-cycle2-reservation-change-cancel-error.md` + `missions/roomescape-reservation-waiting/requirements/base/04-cycle2-waiting-approval.md`
- Semantic checklist: `benchmarks/requirement-variants/checklists/c2-combined.md`
- Status: self-reviewed candidate

## Requirement

예약 서비스는 이제 정상적인 예약 생성만 처리해서는 안 된다. 사용자가 이미 지난 시간에 예약하려 하거나, 같은 날짜/시간/테마에 중복 예약하려 하거나, 예약이 걸려 있는 시간을 삭제하려 하거나, 잘못된 입력값을 보내는 경우를 서비스 정책으로 거부해야 한다. 빈 이름, 잘못된 날짜 형식 같은 입력 오류도 의도된 에러 응답으로 처리한다.

예상 가능한 실패는 사용자에게 500 서버 에러로 노출하지 않는다. 서비스 정책 위반, 입력 오류, 존재하지 않는 리소스에 대해 상태 코드와 응답 본문 형식을 결정하고 README에 문서화한다.

사용자는 자신의 예약 목록을 조회할 수 있고, 자신의 예약을 취소하거나 날짜와 시간을 변경할 수 있어야 한다. 지난 예약을 취소하거나 이미 차 있는 시간으로 변경하는 경우도 같은 에러 응답 규칙으로 처리한다.

대기 기능은 예약 취소/전환 흐름과 연결되어야 한다. 대기를 예약으로 전환하는 방식을 자동 전환 또는 수동 승인 중 하나로 선택하고, 선택 이유와 트랜잭션 경계를 README에 남긴다. 대기가 예약으로 바뀌거나 예약/대기가 취소되면 같은 슬롯의 대기 순번이 다시 계산되어야 한다. 함께 성공하거나 함께 실패해야 하는 데이터 변경은 하나의 트랜잭션 안에서 다룬다.

## Semantic Equivalence Review

- [x] No required behavior was removed.
- [x] No required behavior was weakened.
- [x] No new required behavior was added.
- [x] Ambiguous wording was avoided or intentionally recorded.
- [x] API, test, error, and transaction expectations remain equivalent to the canonical requirement.

## Notes

- Intended wording difference: combined product narrative.
- Risk of semantic drift: medium because two canonical documents are merged.

