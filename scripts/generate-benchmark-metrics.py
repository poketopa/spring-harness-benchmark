#!/usr/bin/env python3
from __future__ import annotations

import argparse
import csv
import sys
from collections import Counter
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
RUNS_PATH = ROOT / "benchmarks" / "benchmark-records" / "runs.csv"
FAILURE_MATRIX_PATH = ROOT / "benchmarks" / "benchmark-records" / "failure-recovery-matrix.csv"
ROBUSTNESS_MATRIX_PATH = ROOT / "benchmarks" / "benchmark-records" / "requirement-robustness-matrix.csv"
PROMPT_SUFFICIENCY_MATRIX_PATH = ROOT / "benchmarks" / "benchmark-records" / "prompt-sufficiency-matrix.csv"
PROMPT_SUFFICIENCY_BLIND_MATRIX_PATH = ROOT / "benchmarks" / "benchmark-records" / "prompt-sufficiency-blind-matrix.csv"
OUTPUT_PATH = ROOT / "benchmarks" / "reports" / "benchmark-metrics.md"


def read_csv(path: Path) -> list[dict[str, str]]:
    with path.open(newline="", encoding="utf-8") as file:
        return list(csv.DictReader(file))


def count_rows(rows: list[dict[str, str]], field: str) -> Counter[str]:
    return Counter(row.get(field, "") for row in rows)


def failure_run_kind(run_id: str) -> str:
    if "-baseline-" in run_id:
        return "baseline"
    if "-rerun-" in run_id:
        return "rerun"
    return "other"


def render_metric_table(metrics: list[tuple[str, str | int]]) -> list[str]:
    lines = [
        "| 지표 | 값 |",
        "| --- | ---: |",
    ]
    lines.extend(f"| {name} | {value} |" for name, value in metrics)
    return lines


def render() -> str:
    runs = read_csv(RUNS_PATH)
    failure_matrix = read_csv(FAILURE_MATRIX_PATH)
    robustness_matrix = read_csv(ROBUSTNESS_MATRIX_PATH)
    prompt_matrix = read_csv(PROMPT_SUFFICIENCY_MATRIX_PATH)
    blind_matrix = read_csv(PROMPT_SUFFICIENCY_BLIND_MATRIX_PATH) if PROMPT_SUFFICIENCY_BLIND_MATRIX_PATH.exists() else []

    run_statuses = count_rows(runs, "result_status")
    failure_runs = [row for row in runs if row["run_id"].startswith("roomescape-failure-")]
    failure_run_kinds = Counter(failure_run_kind(row["run_id"]) for row in failure_runs)
    failure_baselines = [row for row in failure_runs if failure_run_kind(row["run_id"]) == "baseline"]
    failure_reruns = [row for row in failure_runs if failure_run_kind(row["run_id"]) == "rerun"]
    failure_baseline_statuses = count_rows(failure_matrix, "baseline_status")
    failure_rerun_statuses = count_rows(failure_matrix, "rerun_status")
    failure_validated = count_rows(failure_matrix, "validated")
    failure_languages = count_rows(failure_matrix, "prompt_language")
    robustness_statuses = count_rows(robustness_matrix, "status")
    prompt_statuses = count_rows(prompt_matrix, "status")
    prompt_languages = count_rows(prompt_matrix, "prompt_language")
    prompt_features = count_rows(prompt_matrix, "feature")
    prompt_levels = count_rows(prompt_matrix, "prompt_level")
    blind_statuses = count_rows(blind_matrix, "status")
    blind_languages = count_rows(blind_matrix, "prompt_language")
    blind_features = count_rows(blind_matrix, "feature")
    blind_levels = count_rows(blind_matrix, "prompt_level")

    dates = sorted({row["date"] for row in runs if row.get("date")})
    latest_date = dates[-1] if dates else "unknown"
    convention_failures = sum(int(row["convention_violations_total"]) for row in runs)

    lines: list[str] = [
        "# 벤치마크 지표",
        "",
        "이 보고서는 benchmark CSV 기록에서 생성된다.",
        "",
        f"- 최신 run 날짜: `{latest_date}`",
        f"- Source: `{RUNS_PATH.relative_to(ROOT)}`",
        f"- Failure matrix: `{FAILURE_MATRIX_PATH.relative_to(ROOT)}`",
        f"- Robustness matrix: `{ROBUSTNESS_MATRIX_PATH.relative_to(ROOT)}`",
        f"- Prompt sufficiency matrix: `{PROMPT_SUFFICIENCY_MATRIX_PATH.relative_to(ROOT)}`",
        f"- Prompt-only blind matrix: `{PROMPT_SUFFICIENCY_BLIND_MATRIX_PATH.relative_to(ROOT)}`",
        "",
        "## 전체 Run",
        "",
    ]
    lines.extend(render_metric_table([
        ("기록된 run", len(runs)),
        ("Pass run", run_statuses["pass"]),
        ("Partial run", run_statuses["partial"]),
        ("Fail run", run_statuses["fail"]),
        ("기록된 convention failure", convention_failures),
    ]))

    lines.extend([
        "",
        "## 실패 복구",
        "",
    ])
    lines.extend(render_metric_table([
        ("Failure-recovery case", len(failure_matrix)),
        ("Failure-recovery run", len(failure_runs)),
        ("Baseline run", failure_run_kinds["baseline"]),
        ("Rerun", failure_run_kinds["rerun"]),
        ("Baseline status pass", failure_baseline_statuses["pass"]),
        ("Baseline status fail", failure_baseline_statuses["fail"]),
        ("Rerun status skipped", failure_rerun_statuses["skipped"]),
        ("Rerun status pass", failure_rerun_statuses["pass"]),
        ("Rerun status corrected", failure_rerun_statuses["corrected"]),
        ("Validated case", failure_validated["true"]),
        ("영어 prompt case", failure_languages["en"]),
        ("한국어 prompt case", failure_languages["ko"]),
    ]))

    lines.extend([
        "",
        "## 실패 복구 Run 상태",
        "",
    ])
    lines.extend(render_metric_table([
        ("Baseline result pass", count_rows(failure_baselines, "result_status")["pass"]),
        ("Baseline result partial", count_rows(failure_baselines, "result_status")["partial"]),
        ("Baseline result fail", count_rows(failure_baselines, "result_status")["fail"]),
        ("Rerun result pass", count_rows(failure_reruns, "result_status")["pass"]),
        ("Rerun result partial", count_rows(failure_reruns, "result_status")["partial"]),
        ("Rerun result fail", count_rows(failure_reruns, "result_status")["fail"]),
    ]))

    lines.extend([
        "",
        "## 요구사항 견고성",
        "",
    ])
    lines.extend(render_metric_table([
        ("Robustness matrix row", len(robustness_matrix)),
        ("Robustness pass row", robustness_statuses["pass"]),
        ("Robustness planned row", robustness_statuses["planned"]),
        ("Robustness fail row", robustness_statuses["fail"]),
    ]))

    lines.extend([
        "",
        "## 프롬프트 충분성",
        "",
    ])
    lines.extend(render_metric_table([
        ("Prompt-sufficiency planned run", prompt_statuses["planned"]),
        ("Prompt-sufficiency completed run", len(prompt_matrix) - prompt_statuses["planned"]),
        ("한국어 prompt-sufficiency row", prompt_languages["ko"]),
        ("영어 prompt-sufficiency row", prompt_languages["en"]),
        ("Cancel-waiting row", prompt_features["cancel-waiting"]),
        ("Manager-authz row", prompt_features["manager-authz"]),
        ("Concurrent-login row", prompt_features["concurrent-login"]),
        ("Waiting-rank row", prompt_features["waiting-rank"]),
        ("L5 row", prompt_levels["L5"]),
        ("L4 row", prompt_levels["L4"]),
        ("L3 row", prompt_levels["L3"]),
        ("L2 row", prompt_levels["L2"]),
        ("L1 row", prompt_levels["L1"]),
    ]))

    lines.extend([
        "",
        "## Prompt-Only Blind",
        "",
    ])
    lines.extend(render_metric_table([
        ("Blind matrix row", len(blind_matrix)),
        ("Blind planned row", blind_statuses["planned"]),
        ("Blind in-progress row", blind_statuses["in_progress"]),
        ("Blind pass row", blind_statuses["pass"]),
        ("Blind clarification-needed row", blind_statuses["clarification_needed"]),
        ("Blind partial row", blind_statuses["partial"]),
        ("Blind fail row", blind_statuses["fail"]),
        ("Blind invalid row", blind_statuses["invalid"]),
        ("Blind 한국어 row", blind_languages["ko"]),
        ("Blind 영어 row", blind_languages["en"]),
        ("Blind cancel-waiting row", blind_features["cancel-waiting"]),
        ("Blind manager-authz row", blind_features["manager-authz"]),
        ("Blind concurrent-login row", blind_features["concurrent-login"]),
        ("Blind waiting-rank row", blind_features["waiting-rank"]),
        ("Blind L5 row", blind_levels["L5"]),
        ("Blind L3 row", blind_levels["L3"]),
        ("Blind L3R row", blind_levels["L3R"]),
        ("Blind L3Q row", blind_levels["L3Q"]),
        ("Blind L1 row", blind_levels["L1"]),
    ]))

    lines.extend([
        "",
        "## 실패 복구 Case",
        "",
        "| Case | 언어 | 유형 | Baseline | Rerun | 검증 |",
        "| --- | --- | --- | --- | --- | --- |",
    ])
    for row in failure_matrix:
        lines.append(
            f"| `{row['case_id']}` | `{row['prompt_language']}` | `{row['failure_type']}` | "
            f"`{row['baseline_status']}` | `{row['rerun_status']}` | `{row['validated']}` |"
        )

    return "\n".join(lines) + "\n"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--write", action="store_true", help="write the generated report")
    parser.add_argument("--check", action="store_true", help="fail if the generated report is stale")
    args = parser.parse_args()

    output = render()

    if args.write:
        OUTPUT_PATH.write_text(output, encoding="utf-8")
        print(f"wrote {OUTPUT_PATH.relative_to(ROOT)}")
        return 0

    if args.check:
        if not OUTPUT_PATH.exists():
            print(f"missing {OUTPUT_PATH.relative_to(ROOT)}")
            return 1
        current = OUTPUT_PATH.read_text(encoding="utf-8")
        if current != output:
            print(f"stale {OUTPUT_PATH.relative_to(ROOT)}")
            return 1
        print(f"{OUTPUT_PATH.relative_to(ROOT)} is up to date")
        return 0

    print(output, end="")
    return 0


if __name__ == "__main__":
    sys.exit(main())
