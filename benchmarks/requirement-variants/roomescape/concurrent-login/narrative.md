# concurrent-login narrative Requirement Variant

## Variant Metadata

- Unit: concurrent-login
- Variant type: narrative
- Canonical requirement: `missions/roomescape-reservation-waiting/requirements/add-ons/auth-authorization/04-prevent-concurrent-login.md`
- Semantic checklist: `benchmarks/requirement-variants/checklists/concurrent-login.md`
- Status: self-reviewed candidate

## Requirement

운영팀은 같은 계정이 여러 기기에서 동시에 로그인되어 있는 상황을 제한하고 싶어 한다. 이 요구사항은 인증 방식과 직접 연결된다. 특히 토큰 기반 인증을 사용한다면 이미 발급된 토큰을 어떻게 무효화할지, 서버가 어떤 로그인 상태를 기억해야 하는지 고민해야 한다.

구현 범위 안에서 같은 계정의 동시 로그인을 제한한다. 새 로그인이 발생했을 때 기존 로그인 상태를 유지할지, 새 로그인을 우선할지 정책을 선택하고 문서화한다. 선택한 정책에 따라 기존 토큰이나 세션이 어떻게 처리되는지 테스트 가능해야 한다.

현재 인증 방식에서 이 요구사항을 구현할 때 어려운 지점도 기록한다. 서버가 로그인 상태를 알고 있는지, 새 로그인 시 기존 로그인 정보를 찾을 수 있는지, 토큰 검증마다 저장소를 확인해야 하는지, 운영 요구사항 때문에 어떤 서버 상태가 추가되는지 설명한다.

## Semantic Equivalence Review

- [x] No required behavior was removed.
- [x] No required behavior was weakened.
- [x] No new required behavior was added.
- [x] Ambiguous wording was avoided or intentionally recorded.
- [x] API, test, error, and transaction expectations remain equivalent to the canonical requirement.

## Notes

- Intended wording difference: operations narrative.
- Risk of semantic drift: low.

