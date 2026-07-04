# Next Session Handoff

## 현재 상태

현재 Roomescape benchmark scope의 핵심 연구는 완료됐다.

완료된 phase:

- repeatability
- requirement robustness
- failure recovery
- oracle-assisted prompt sufficiency baseline
- Korean prompt-only blind baseline
- L3R/L3Q follow-up
- L3R ablation screening
- partial-axis confirmatory
- robust-axis confirmatory
- race tie-breaker
- repository cleanup
- public README/report Korean localization

최종 검증 상태:

```bash
python3 scripts/validate-benchmark-records.py
python3 scripts/generate-benchmark-metrics.py --check
```

두 명령 모두 clean이어야 한다. 마지막 확인 결과는 validator `0 warning(s)`, metrics up to date였다.

## 반드시 보존할 계약

- Original L3, L3R, L3Q, ablation, confirmatory, tie-breaker 결과를 섞지 않는다.
- Hidden oracle은 implementation agent에게 노출하지 않는다.
- Prompt-only blind implementation은 sanitized `/tmp` workspace에서만 수행한다.
- Skill/reference/evaluator/oracle은 benchmark 도중 임의로 바꾸지 않는다.
- CSV 헤더, run_id, status enum, category name은 영어 그대로 유지한다.
- 사람이 읽는 README/report는 한국어로 유지한다.

## 먼저 읽을 문서

1. `README.md`
2. `benchmarks/README.md`
3. `benchmarks/benchmark-records/README.md`
4. `benchmarks/benchmark-records/summary.md`
5. `benchmarks/reports/benchmark-metrics.md`
6. `benchmarks/reports/prompt-sufficiency-l3r-ablation-closeout.md`
7. `benchmarks/reports/prompt-sufficiency-l3r-ablation-report.md`
8. `benchmarks/reports/prompt-sufficiency-report.md`
9. `scripts/validate-benchmark-records.py`
10. `scripts/generate-benchmark-metrics.py`

## 다음으로 할 수 있는 일

필수 다음 단계는 없다.

선택지는 다음과 같다.

1. L3R vNext를 실제 prompt template로 작성한다.
2. L3R/L3Q 영어 transfer 실험을 설계한다.
3. 새 feature family에서 L3R을 검증한다.
4. 이 저장소를 GitHub 공개용으로 commit 단위 정리한다.
5. portfolio/project README를 더 짧은 외부 공개용으로 다듬는다.

## 주의할 점

현재 저장소는 연구 기록 저장소다. Spring service portfolio project처럼 보이게 만들면 핵심 가치가 흐려진다.

공개 메시지는 다음 문장으로 잡는 것이 안전하다.

> 이 저장소는 AI coding skill의 반복성, 요구사항 견고성, 실패 복구, prompt sufficiency, L3R ablation을 기록한 Roomescape benchmark harness다.
