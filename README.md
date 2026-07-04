# Spring Usecase Harness

## 요약

Spring/Java 요구사항 구현을 대상으로, 코딩 에이전트(Codex)의 구현 안정성을 측정.

### 핵심 결론

```
약한 프롬프트도 숨김 정답 기준이나 기존 맥락을 암묵적으로 받으면 좋아 보일 수 있다.
그러나 프롬프트 단독 블라인드 조건에서는 요구사항 증거가 쉽게 빠진다.
L3R처럼 검증자가 관찰할 수 있는 동작과 필수 증거를 압축해 명시한 프롬프트는 그 경계를 크게 개선했다.
```


## 개요

각 실험은 다음 단위를 가진다.

- 질문
- 실험 행렬
- 대상 프로젝트
- 실행 기록
- CSV 기록
- 보고서

이 README는 전체 실험 지도다. 
세부 판단 근거는 `benchmarks/reports/`와 `benchmarks/benchmark-records/`에 보관한다.

## 전체 결과

| 실험군 | 행/실행 | 상태 | 핵심 결론 |
| --- | ---: | --- | --- |
| 반복 안정성 | 26회 반복 실행 | 완료 | 24회 통과, 2회 실행별 실패 |
| 요구사항 견고성 | 12회 실행 | 완료 | 12/12 통과 |
| 실패 복구 | 10개 사례, 14회 실행 | 완료 | 난제 사례와 검증기 공백 기록/수정 |
| 프롬프트 충분성 | 84회 실행 | 완료 | 정답 기준 보조 기준선 84/84 통과 |
| 프롬프트 단독 블라인드 | 실험 행렬 51행 | 완료 | L5 안정, 원본 L3/L1 불안정, L3R 수리 |
| L3R 제거 실험 | 39행 | 완료 | 취약/견고 묶음 분리 |


## 실험군 1. 반복 안정성

질문:

> 같은 요구사항을 반복 실행해도 같은 관례의 Spring 코드가 나오는가?

결과:

- 26회 반복 실행
- 24회 깔끔한 통과
- 2회 실행별 실패
- 스킬/참고 자료 수정 없이 보정 가능

보고서:

- [spring-usecase-repeatability-report.md](benchmarks/reports/spring-usecase-repeatability-report.md)

## 실험군 2. 요구사항 견고성

질문:

> 같은 의미의 요구사항을 다른 문장 스타일로 바꿔도 의미와 구조가 유지되는가?

대상:

- `c1-waiting`
- `c2-combined`
- `manager-authz`
- `concurrent-login`

결과:

- 12/12 통과
- P0/P1 관례 실패 없음
- 의미 이탈 없음

보고서:

- [requirement-robustness-report.md](benchmarks/reports/requirement-robustness-report.md)

## 실험군 3. 실패 복구

질문:

> 인공지능 코딩 난제 사례를 기록하고, 구현 실패와 벤치마크 설계 공백을 구분하며, 평가기 공백을 줄일 수 있는가?

결과:

- 난제 사례 10개
- 실행 14회
- 사례 설계 공백 2건 수정
- 평가기/검증기 공백 2건 수정

보고서:

- [failure-recovery-report.md](benchmarks/reports/failure-recovery-report.md)

## 실험군 4. 프롬프트 충분성

질문:

> 프롬프트 품질이 낮아지면 어떤 요구사항 증거가 사라지는가?

정답 기준 보조 기준선:

- 84/84 통과
- 한국어/영어 프롬프트 약화
- 숨김 정답 기준으로 결과 판정

프롬프트 단독 블라인드 기준선:

- L5: 12/12 통과
- 원본 L3: 11건 부분 통과, 1건 추가 질문 필요
- 원본 L1: 9건 부분 통과, 3건 실패

L3R 후속 실험:

- L3R: 12/12 통과
- L3Q: 3/3 추가 질문 필요

핵심 해석:

- 원본 L3/L1은 블라인드 조건에서 충분하지 않았다.
- L3R은 원본 L3의 실패 경계를 실질적으로 복구했다.
- L3Q의 `clarification_needed`는 "정보가 없으면 질문해야 한다"는 기대 동작을 확인한 결과다.

보고서:

- [prompt-sufficiency-report.md](benchmarks/reports/prompt-sufficiency-report.md)

## 실험군 5. L3R 제거 실험

질문:

> L3R에서 어떤 정보 묶음을 제거하면 다시 부분 통과/실패/추가 질문 필요로 떨어지는가?

단계:

| 단계 | 행 | 결과 |
| --- | ---: | --- |
| 선별 | 12 | 5건 통과, 7건 부분 통과 |
| 부분 통과 축 확인 | 14 | 14건 부분 통과 |
| 견고 축 확인 | 10 | 9건 통과, 1건 부분 통과 |
| 경쟁 조건 판별 | 3 | 3건 통과 |

확정된 취약 묶음:

- 취소 거절 정책
- 되돌림 증거
- 인증/인가 분리
- 다른 회원 토큰 보존
- 대기 슬롯/본인 예약 정책
- 내 예약 목록의 상태/순번 응답

확정된 견고 또는 견고 경향 묶음:

- 매장 소유 모델
- 서비스/테스트 경계 문구
- 서버 측 활성 세션 문구
- 최종 중복 방지 문구
- 경쟁/원자성 문구. 단, 실제 동시성 테스트 문구는 유지 권장

핵심 해석:

- 취약 묶음은 검증자가 관찰할 수 있는 동작과 테스트 증거를 직접 지정하는 문장에 집중되어 있다.
- 견고 묶음은 이미 스킬/참고 자료나 일반 Spring 관례로 보강되는 축에 가깝다.
- 경쟁/원자성 문구는 대체로 견고하지만, 실제 동시성 테스트 문구는 유지하는 편이 안전하다.

보고서:

- [prompt-sufficiency-l3r-ablation-closeout.md](benchmarks/reports/prompt-sufficiency-l3r-ablation-closeout.md)
- [prompt-sufficiency-l3r-ablation-report.md](benchmarks/reports/prompt-sufficiency-l3r-ablation-report.md)

## 기록 원칙

이 저장소의 벤치마크는 다음 원칙을 지킨다.

- 원본 실험, 후속 실험, 제거 실험 결과를 서로 섞지 않는다.
- 새 실험은 별도 실험 행렬과 CSV 기록을 가진다.
- 숨김 정답 기준은 구현 에이전트에게 노출하지 않는다.
- 구현은 정제된 작업 공간에서 수행한다.
- 스킬, 참고 자료, 평가기, 정답 기준은 실험 중 임의로 바꾸지 않는다.
- 결과 판정은 실행 기록, CSV 기록, 보고서가 서로 맞아야 한다.

## 검증 상태

정리 전 마지막 검증 결과는 다음과 같다.

- CSV 원장, 필수 필드, 보고서 참조, 실행 기록 계약: 통과
- `benchmarks/reports/benchmark-metrics.md`와 현재 CSV 기록: 일치

현재 공개 저장소에는 검증 스크립트를 포함하지 않는다.
새 실험을 이어가려면 같은 기록 계약을 보존하는 검증기를 별도 복구한 뒤 CSV와 보고서를 함께 갱신한다.

## 저장소 구조

```text
.
├── README.md
├── .agents/
│   └── skills/
│       └── spring-usecase-implementation/
│           ├── SKILL.md
│           └── references/
│               ├── architecture-principles.md
│               ├── controller-service-repository.md
│               ├── diagnostics-checklist.md
│               ├── domain-modeling.md
│               ├── java-class-ordering.md
│               ├── review-rubric.md
│               └── testing-style.md
├── benchmarks/
│   ├── README.md
│   ├── benchmark-records/
│   │   ├── README.md
│   │   ├── runs.csv
│   │   ├── convention-comparisons.csv
│   │   ├── *-matrix.csv
│   │   ├── runs/
│   │   └── templates/
│   ├── reports/
│   │   ├── benchmark-metrics.md
│   │   ├── failure-recovery-report.md
│   │   ├── prompt-sufficiency-report.md
│   │   ├── prompt-sufficiency-l3r-ablation-report.md
│   │   ├── prompt-sufficiency-l3r-ablation-closeout.md
│   │   ├── requirement-robustness-report.md
│   │   └── spring-usecase-repeatability-report.md
│   ├── failure-cases/
│   ├── failure-runs/
│   ├── requirement-variants/
│   ├── robustness-runs/
│   ├── repeat-runs/
│   ├── prompt-sufficiency-cases/
│   ├── prompt-sufficiency-runs/
│   ├── prompt-sufficiency-blind-runs/
│   ├── prompt-sufficiency-l3r-ablation-runs/
│   ├── prompt-sufficiency-l3r-ablation-confirmatory-runs/
│   ├── prompt-sufficiency-l3r-ablation-robust-confirmatory-runs/
│   ├── prompt-sufficiency-l3r-ablation-race-tiebreaker-runs/
│   └── roomescape-*/
└── missions/
    └── roomescape-reservation-waiting/
        └── requirements/
            ├── base/
            └── add-ons/
                ├── auth-authorization/
                └── jpa/
```

- `.agents/skills/spring-usecase-implementation/`: 평가 대상 Codex 스킬과 참고 자료
- `benchmarks/benchmark-records/`: CSV 원장, 실행 기록, 실험 행렬, 기록 템플릿
- `benchmarks/reports/`: 사람이 읽는 최종 보고서와 지표 보고서
- `benchmarks/*-runs/`: 각 실험에서 생성된 Spring 대상 프로젝트
- `benchmarks/prompt-sufficiency-cases/`: 프롬프트 수준별 사례와 숨김 정답 기준
- `benchmarks/requirement-variants/`: 요구사항 변형과 의미 점검표
- `missions/`: 원 요구사항 문서
