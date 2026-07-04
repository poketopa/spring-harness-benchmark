# manager-authz narrative Requirement Variant

## Variant Metadata

- Unit: manager-authz
- Variant type: narrative
- Canonical requirement: `missions/roomescape-reservation-waiting/requirements/add-ons/auth-authorization/03-store-manager-authorization.md`
- Semantic checklist: `benchmarks/requirement-variants/checklists/manager-authz.md`
- Status: self-reviewed candidate

## Requirement

서비스가 여러 매장을 지원하면서, 로그인 여부만으로 예약 관리 권한을 판단할 수 없게 되었다. 매장 매니저는 자신이 담당하는 매장의 예약만 다룰 수 있어야 하며, 다른 매장의 예약을 조회하거나 변경하거나 삭제해서는 안 된다.

구현은 인증과 인가를 구분해야 한다. 로그인하지 않은 사용자는 인증 실패로 처리하고, 로그인은 했지만 매니저가 아니거나 다른 매장 예약에 접근하는 사용자는 인가 실패로 처리한다. 두 실패를 같은 문제처럼 뭉개지 않는다.

로그인한 사용자가 매니저인지 확인할 수 있어야 하고, 그 매니저가 어떤 매장을 담당하는지도 확인할 수 있어야 한다. 접근하려는 예약이 어떤 매장에 속하는지도 비교 가능해야 한다.

인가 판단 로직이 컨트롤러 곳곳에 흩어지지 않게 경계를 정한다. 매니저와 매장의 관계, 예약 조회 후 비교할지 권한 조건을 포함해 조회할지, 실패 응답을 어떻게 줄지, 로그인 정보와 도메인 정보를 어떻게 연결할지 결정하고 설명한다.

## Semantic Equivalence Review

- [x] No required behavior was removed.
- [x] No required behavior was weakened.
- [x] No new required behavior was added.
- [x] Ambiguous wording was avoided or intentionally recorded.
- [x] API, test, error, and transaction expectations remain equivalent to the canonical requirement.

## Notes

- Intended wording difference: product/security narrative.
- Risk of semantic drift: low.

