# Spring Usecase 반복성 보고서

## 요약

이 벤치마크는 `spring-usecase-implementation` skill이 같은 Roomescape 요구사항 family에서 반복 실행될 때 owner convention에 맞는 Spring 코드를 안정적으로 생성하는지 측정했다.

최종 반복 benchmark set은 4개 requirement unit의 26개 repeat run으로 구성된다. 24개는 clean pass였고, 2개는 full verification 전에 실패했다. 두 실패는 기록 후 run-local correction으로 수정됐으며 skill/reference 변경은 필요하지 않았다.

## 범위

| Unit | Requirement | Baseline |
| --- | --- | --- |
| `c1-waiting-repeat` | waiting creation/cancel and combined my-reservation read model | `benchmarks/roomescape-jpa-auth-base-v2` |
| `c2-repeat` | reservation change/cancel/error and waiting approval | `benchmarks/roomescape-jpa-auth-cycle1-regen-v2` |
| `manager-authz-repeat` | store manager authorization | `benchmarks/roomescape-jpa-auth-cycle2-regen-v2` |
| `concurrent-login-repeat` | prevent concurrent login | `benchmarks/roomescape-jpa-auth-manager-authorization-regen-v2` |

## 결과

| Unit | Repeat runs | Clean pass | Recorded failure | 판단 |
| --- | ---: | ---: | ---: | --- |
| `c1-waiting-repeat` | 6 | 6 | 0 | stable |
| `c2-repeat` | 6 | 6 | 0 | stable |
| `manager-authz-repeat` | 7 | 6 | 1 | run-local correction 후 stable |
| `concurrent-login-repeat` | 7 | 6 | 1 | run-local correction 후 stable |

전체 지표:

| 항목 | 값 |
| --- | ---: |
| recorded runs | 45 |
| passing runs | 43 |
| repeat runs | 26 |
| clean repeat passes | 24 |
| run-local repeat failures | 2 |
| skill updates recorded | 23 |
| pending skill updates | 0 |

## 실패 분석

`roomescape-repeat-manager-authz-001`은 `ManagerAuthorizationService`가 추가된 뒤 기존 service unit test가 오래된 `ReservationService` constructor를 사용해 full verification 전에 실패했다. `roomescape-convention-correction-007`에서 run-local로 수정했고 이후 6회 manager authorization repeat가 통과했다.

`roomescape-repeat-concurrent-login-004`는 stale-token acceptance assertion helper가 unauthorized error body를 `List.class`로 요청해 narrow verification에서 실패했다. `roomescape-convention-correction-008`에서 run-local로 수정했고 이후 3회 concurrent-login repeat가 통과했다.

두 실패 모두 skill/reference update를 요구하지 않았다.

## 안정된 convention

- use case 이름으로 찾기 쉬운 feature-specific service 분리
- `WaitingPromotionService`가 reservation-waiting promotion 소유
- `MyReservationService`가 combined reservation/waiting read model 소유
- `AuthenticatedMemberService`가 `LoginMember -> Member` lookup 중앙화
- DTO factory는 단순 mapping에 `from`, 계산 state가 있으면 `of`
- domain invariant는 generic validator보다 domain class에 둠
- manager authorization은 전용 authorization boundary 사용
- stateful auth는 server-side active session state와 real concurrent test 사용

## 결론

현재 skill은 테스트한 Roomescape requirement family에서 반복 안정성이 충분하다. 추가 증거가 필요하다면 가장 복잡하고 post-correction streak이 짧은 concurrent-login unit에 집중하는 것이 효율적이다.
