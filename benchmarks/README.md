# Benchmarks

Roomescape Spring 프로젝트를 대상으로 한 AI 코딩 skill 평가 자료입니다.

이 디렉터리는 단순한 예제 프로젝트 모음이 아니라, 요구사항 문장, target project, 실행 기록, hidden oracle review, 최종 보고서를 함께 보관하는 벤치마크 데이터셋입니다.

## 지도

- `benchmark-records/`: CSV 원장, run note, 템플릿, 연구 요약
- `failure-cases/`: 실패 복구 hard case
- `failure-runs/`: 실패 복구 baseline/rerun target
- `prompt-sufficiency-cases/`: 프롬프트 충분성 case와 hidden oracle
- `prompt-sufficiency-runs/`: oracle-assisted baseline target
- `prompt-sufficiency-blind-runs/`: prompt-only blind baseline target
- `prompt-sufficiency-l3r-ablation-runs/`: L3R ablation screening target
- `prompt-sufficiency-l3r-ablation-confirmatory-runs/`: fragile-axis confirmatory target
- `prompt-sufficiency-l3r-ablation-robust-confirmatory-runs/`: robust-axis confirmatory target
- `prompt-sufficiency-l3r-ablation-race-tiebreaker-runs/`: race-axis tie-breaker target
- `reports/`: 최종 사람이 읽는 보고서
- `requirement-variants/`: 같은 의미를 다른 문장 스타일로 바꾼 요구사항
- `robustness-runs/`: requirement robustness target
- `roomescape-jpa-auth-*`: baseline, regenerated, stable target

## 먼저 읽을 보고서

- `reports/benchmark-metrics.md`
- `reports/prompt-sufficiency-l3r-ablation-closeout.md`
- `reports/prompt-sufficiency-l3r-ablation-report.md`
- `reports/prompt-sufficiency-report.md`
- `reports/failure-recovery-report.md`
- `reports/requirement-robustness-report.md`
- `reports/spring-usecase-repeatability-report.md`

## 기록 상태

정리 전 마지막 검증 결과는 다음과 같습니다.

- CSV 원장, 필수 필드, 보고서 참조, 실행 기록 계약: 통과
- `reports/benchmark-metrics.md`와 현재 CSV 기록: 일치

현재 공개 저장소에는 검증 스크립트를 포함하지 않습니다.
