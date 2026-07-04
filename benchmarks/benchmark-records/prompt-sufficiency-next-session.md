# Prompt Sufficiency Next Session Prompt

Use this exact message to start the next Codex session:

```text
/Users/lhs/Desktop/harness 프로젝트에서 계속 진행한다.

이번 세션의 목표는 완료된 prompt-only blind 결과를 기준으로 다음 실험을 설계하는 것이다. 현재 상태는 다음과 같다.

- 84-run prompt-sufficiency baseline은 oracle-assisted harness evidence로 유지한다.
- 원본 Korean prompt-only blind baseline은 36/36 rows complete, validator-clean이다.
- 원본 baseline 결과: L5 12/12 pass, L3 11 partial + 1 clarification_needed, L1 9 partial + 3 fail.
- L3 repair follow-up은 12/12 pass다.
- manager-authz clarification-gate follow-up은 3/3 clarification_needed다.
- 전체 blind matrix는 51 rows, 24 pass, 4 clarification_needed, 20 partial, 3 fail, 0 invalid, 0 planned다.
- L3R/L3Q는 intervention rows이므로 원본 L3 성공률에 합치지 않는다.

먼저 반드시 다음 문서를 읽는다:
1. README.md
2. benchmarks/README.md
3. benchmarks/benchmark-records/README.md
4. benchmarks/benchmark-records/summary.md
5. benchmarks/prompt-sufficiency-plan.md
6. benchmarks/prompt-sufficiency-blind-plan.md
7. benchmarks/prompt-sufficiency-blind-execution-protocol.md
8. benchmarks/reports/prompt-sufficiency-report.md
9. benchmarks/reports/benchmark-metrics.md
10. benchmarks/benchmark-records/prompt-sufficiency-matrix.csv
11. benchmarks/benchmark-records/prompt-sufficiency-blind-matrix.csv
12. benchmarks/benchmark-records/runs.csv
13. benchmarks/benchmark-records/convention-comparisons.csv
14. benchmarks/benchmark-records/runs/roomescape-prompt-blind-cancel-waiting-ko-l3r-001.md
15. benchmarks/benchmark-records/runs/roomescape-prompt-blind-manager-authz-ko-l3r-001.md
16. benchmarks/benchmark-records/runs/roomescape-prompt-blind-concurrent-login-ko-l3r-001.md
17. benchmarks/benchmark-records/runs/roomescape-prompt-blind-waiting-rank-ko-l3r-001.md
18. benchmarks/benchmark-records/runs/roomescape-prompt-blind-manager-authz-ko-l3q-001.md

시작 확인:
1. README.md의 검증 상태를 확인한다.
2. 공개 저장소에는 정리 과정에서 기록 검증 스크립트가 빠져 있다.
3. 새 실험 기록을 추가하거나 지표를 갱신하려면 동등한 검증 수단을 먼저 복구/확정한다.

핵심 해석:
- L5 prompt는 Korean blind setting에서 반복 안정적으로 pass했다.
- 원본 L3 prompt는 core behavior를 자주 만들지만 oracle-complete 구현에는 부족했다.
- L3R compact checklist는 네 feature class에서 원본 L3 gap을 닫았다.
- L3Q clarification gate는 manager-authz처럼 소유권 모델이 없는 요구사항에서 잘못된 자신감 있는 구현을 막았다.
- 좋은 포트폴리오 주장은 "빈약한 prompt도 충분하다"가 아니라 "어떤 prompt 정보가 부족하면 실패/partial/clarification으로 갈리는지 측정했고, 최소 보강 checklist와 clarification gate가 효과 있음을 검증했다"이다.

이번 세션에서 할 일:
1. 다음 실험 질문을 하나로 고른다.
   - 권장 A: English transfer of L3R/L3Q. 한국어에서 성공한 repaired checklist가 영어에서도 유지되는지 본다.
   - 권장 B: new feature-family validation. 현재 네 feature class 밖에서도 checklist가 작동하는지 본다.
   - 보류: 같은 Korean L3R/L3Q 반복 추가. 이미 3-repeat 안정성이 충분해 정보 이득이 낮다.
2. 선택한 실험에 대해 matrix/record design을 먼저 작성한다.
3. run_id, case_path, baseline_path, target_path, isolation rule, stop condition, scoring rule을 문서화한다.
4. 새 implementation rows를 시작하기 전 기록 계약과 검증 수단을 먼저 확정한다.
5. 구현 run을 시작한다면 implementation agent는 hidden oracle, previous solution source, run note, convention comparison을 보지 않아야 한다.

금지:
- 84-run oracle-assisted baseline과 prompt-only blind/intervention 결과를 합쳐 성공률로 주장하지 않는다.
- 원본 L3와 L3R을 같은 prompt level로 합치지 않는다.
- 새 실험 matrix가 정의되기 전 implementation agent를 만들지 않는다.
- skill/reference/evaluator/oracle을 바꾸지 않는다. 새 실험을 위해 기록 검증 수단을 복구하거나 바꿔야 하는 경우만 최소 변경하고 반드시 문서화한다.
```
