# Benchmark Records

벤치마크 실행 기록을 보관하는 원장 디렉터리입니다.

기록의 기본 흐름은 다음과 같습니다.

```text
prompt -> generated result -> oracle/convention comparison -> run note -> CSV ledger -> report
```

## 필수 파일

- `runs.csv`: benchmark run 단위 원장
- `convention-comparisons.csv`: run별 판정 category 원장
- `skill-updates.csv`: skill/reference 변경 이력
- `requirement-robustness-matrix.csv`: requirement robustness matrix
- `failure-recovery-matrix.csv`: failure-recovery matrix
- `prompt-sufficiency-matrix.csv`: oracle-assisted prompt sufficiency matrix
- `prompt-sufficiency-blind-matrix.csv`: prompt-only blind baseline 및 L3R/L3Q follow-up matrix
- `prompt-sufficiency-l3r-ablation-matrix.csv`: L3R ablation screening matrix
- `prompt-sufficiency-l3r-ablation-confirmatory-matrix.csv`: fragile-axis confirmatory matrix
- `prompt-sufficiency-l3r-ablation-robust-confirmatory-matrix.csv`: robust-axis confirmatory matrix
- `prompt-sufficiency-l3r-ablation-race-tiebreaker-matrix.csv`: race-axis tie-breaker matrix
- `runs/{run_id}.md`: 사람이 읽는 run note
- `templates/`: run note 및 blind implementation prompt 템플릿
- `summary.md`: 현재 연구 요약
- `next-session-handoff.md`: 다음 세션 인수인계

## 실행 전 계약

1. 안정적인 `run_id`를 먼저 정한다.
2. matrix row를 먼저 만든다.
3. implementation이 볼 수 있는 context와 verifier-only context를 분리한다.
4. hidden oracle은 implementation agent에게 노출하지 않는다.
5. prompt-only blind 계열은 sanitized `/tmp` workspace에서 구현한다.

## 실행 후 계약

1. 가능한 경우 narrow test를 먼저 실행한다.
2. 최종 검증은 보통 다음 명령이다.

   ```bash
   ./gradlew clean test
   ```

3. `runs.csv`를 갱신한다.
4. `convention-comparisons.csv`를 갱신한다.
5. run note를 작성한다.
6. 필요할 때만 `skill-updates.csv`를 갱신한다.
7. CSV 원장, run note, 보고서가 서로 일치하는지 확인한다.

정리 전 마지막 공식 검증 결과는 validator `0 warning(s)`, metrics up to date였다.

## 상태 값

CSV 헤더와 enum 값은 스크립트 계약이므로 영어로 유지한다.

- run status: `pass`, `partial`, `fail`
- verification status: `pass`, `fail`, `not_run`
- matrix status: `planned`, `in_progress`, `pass`, `partial`, `fail`, `clarification_needed`
- boolean: `true`, `false`

## 주의

원시 run note는 재현 근거다. 사람이 읽는 결론은 `benchmarks/reports/`에 한국어로 정리하되, CSV 필드명, run_id, status 값, 파일 경로, 명령어는 번역하지 않는다.
