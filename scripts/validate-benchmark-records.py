#!/usr/bin/env python3
from __future__ import annotations

import csv
import sys
from collections import Counter, defaultdict
from datetime import datetime
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
RUNS_PATH = ROOT / "benchmarks" / "benchmark-records" / "runs.csv"
COMPARISONS_PATH = ROOT / "benchmarks" / "benchmark-records" / "convention-comparisons.csv"
ROBUSTNESS_MATRIX_PATH = ROOT / "benchmarks" / "benchmark-records" / "requirement-robustness-matrix.csv"
FAILURE_RECOVERY_MATRIX_PATH = ROOT / "benchmarks" / "benchmark-records" / "failure-recovery-matrix.csv"
PROMPT_SUFFICIENCY_MATRIX_PATH = ROOT / "benchmarks" / "benchmark-records" / "prompt-sufficiency-matrix.csv"
PROMPT_SUFFICIENCY_BLIND_MATRIX_PATH = (
    ROOT / "benchmarks" / "benchmark-records" / "prompt-sufficiency-blind-matrix.csv"
)

RUN_HEADERS = [
    "run_id",
    "sequence",
    "date",
    "mission",
    "cycle",
    "requirement_path",
    "baseline_path",
    "target_path",
    "skill_name",
    "started_at",
    "finished_at",
    "duration_min",
    "result_status",
    "full_verification",
    "full_verification_result",
    "convention_violations_total",
    "manual_fix_required",
    "manual_fix_files_count",
    "skill_updated",
    "notes_path",
]

COMPARISON_HEADERS = [
    "run_id",
    "category",
    "expected",
    "observed",
    "status",
    "severity",
    "evidence",
    "skill_update_needed",
]

ROBUSTNESS_MATRIX_HEADERS = [
    "run_id",
    "unit",
    "variant_type",
    "requirement_variant_path",
    "semantic_checklist_path",
    "baseline_path",
    "target_path",
    "status",
]

FAILURE_RECOVERY_MATRIX_HEADERS = [
    "case_id",
    "unit",
    "failure_type",
    "prompt_language",
    "requirement_path",
    "baseline_path",
    "target_path",
    "baseline_run_id",
    "baseline_status",
    "intervention_type",
    "intervention_path",
    "rerun_id",
    "rerun_status",
    "validated",
]

PROMPT_SUFFICIENCY_MATRIX_HEADERS = [
    "run_id",
    "feature",
    "prompt_language",
    "prompt_level",
    "repeat",
    "case_path",
    "oracle_path",
    "baseline_path",
    "target_path",
    "status",
]

PROMPT_SUFFICIENCY_BLIND_MATRIX_HEADERS = [
    "run_id",
    "feature",
    "prompt_language",
    "prompt_level",
    "repeat",
    "case_path",
    "oracle_path",
    "baseline_path",
    "target_path",
    "status",
    "implementation_context",
    "verifier_context",
    "source_hash",
    "source_similarity",
    "clarification_needed",
    "isolation_status",
    "notes_path",
]

RUN_STATUSES = {"pass", "partial", "fail"}
VERIFICATION_STATUSES = {"pass", "fail", "not_run"}
COMPARISON_STATUSES = {"pass", "fail", "not_applicable"}
SEVERITIES = {"P0", "P1", "P2", "P3"}
BOOLS = {"true", "false"}
MATRIX_STATUSES = {"planned", "in_progress", "pass", "fail", "corrected", "skipped"}
FAILURE_TYPES = {
    "ambiguity_handling",
    "requirement_conflict",
    "decoy_scope_control",
    "semantic_drift",
    "architecture_drift",
    "missing_verification",
    "concurrency_weakness",
    "record_integrity_gap",
    "evaluator_blind_spot",
}
PROMPT_LANGUAGES = {"en", "ko"}
PROMPT_SUFFICIENCY_FEATURES = {"cancel-waiting", "manager-authz", "concurrent-login", "waiting-rank"}
PROMPT_LEVELS = {"L1", "L2", "L3", "L4", "L5"}
BLIND_PROMPT_LEVELS = {"L0", "L1", "L2", "L3", "L3R", "L3Q", "L4", "L5"}
PROMPT_REPEATS = {"001", "002", "003"}
COMPLETED_MATRIX_STATUSES = {"pass", "fail", "corrected"}
BLIND_MATRIX_STATUSES = {"planned", "in_progress", "pass", "clarification_needed", "partial", "fail", "invalid"}
BLIND_COMPLETED_MATRIX_STATUSES = {"pass", "clarification_needed", "partial", "fail"}
BLIND_ISOLATION_STATUSES = {"pending", "pass", "fail", "not_applicable"}
REAL_CONCURRENT_TEST_MARKERS = {
    "CountDownLatch",
    "ExecutorService",
    "Executors.newFixedThreadPool",
    "CyclicBarrier",
    "CompletableFuture",
}


def read_csv(path: Path) -> tuple[list[str], list[dict[str, str]]]:
    if not path.exists():
        return [], []
    with path.open(newline="", encoding="utf-8") as file:
        reader = csv.DictReader(file)
        return reader.fieldnames or [], list(reader)


def is_strict_benchmark(run_id: str) -> bool:
    return (
        run_id.startswith("roomescape-repeat-")
        or run_id.startswith("roomescape-robustness-")
        or run_id.startswith("roomescape-failure-")
        or run_id.startswith("roomescape-prompt-")
    )


def parse_datetime(value: str) -> datetime | None:
    try:
        return datetime.fromisoformat(value)
    except ValueError:
        return None


def numeric(value: str) -> float | None:
    try:
        return float(value)
    except ValueError:
        return None


def relative_exists(path_text: str) -> bool:
    return bool(path_text) and (ROOT / path_text).exists()


def add_missing_path_error(errors: list[str], source: str, index: int, field: str, row_id: str, value: str) -> None:
    errors.append(f"{source}:{index} missing {field} for {row_id}: {value}")


def has_real_concurrent_test(target_path_text: str) -> bool:
    if not target_path_text:
        return False

    test_root = ROOT / target_path_text / "src" / "test"
    if not test_root.exists():
        return False

    for path in test_root.rglob("*.java"):
        text = path.read_text(encoding="utf-8")
        if any(marker in text for marker in REAL_CONCURRENT_TEST_MARKERS):
            return True
    return False


def validate() -> tuple[list[str], list[str]]:
    errors: list[str] = []
    warnings: list[str] = []

    run_headers, runs = read_csv(RUNS_PATH)
    comparison_headers, comparisons = read_csv(COMPARISONS_PATH)
    matrix_headers, matrix_rows = read_csv(ROBUSTNESS_MATRIX_PATH)
    failure_matrix_headers, failure_matrix_rows = read_csv(FAILURE_RECOVERY_MATRIX_PATH)
    prompt_matrix_headers, prompt_matrix_rows = read_csv(PROMPT_SUFFICIENCY_MATRIX_PATH)
    blind_matrix_headers, blind_matrix_rows = read_csv(PROMPT_SUFFICIENCY_BLIND_MATRIX_PATH)

    if run_headers != RUN_HEADERS:
        errors.append(f"{RUNS_PATH.relative_to(ROOT)} header mismatch")
    if comparison_headers != COMPARISON_HEADERS:
        errors.append(f"{COMPARISONS_PATH.relative_to(ROOT)} header mismatch")
    if ROBUSTNESS_MATRIX_PATH.exists() and matrix_headers != ROBUSTNESS_MATRIX_HEADERS:
        errors.append(f"{ROBUSTNESS_MATRIX_PATH.relative_to(ROOT)} header mismatch")
    if FAILURE_RECOVERY_MATRIX_PATH.exists() and failure_matrix_headers != FAILURE_RECOVERY_MATRIX_HEADERS:
        errors.append(f"{FAILURE_RECOVERY_MATRIX_PATH.relative_to(ROOT)} header mismatch")
    if PROMPT_SUFFICIENCY_MATRIX_PATH.exists() and prompt_matrix_headers != PROMPT_SUFFICIENCY_MATRIX_HEADERS:
        errors.append(f"{PROMPT_SUFFICIENCY_MATRIX_PATH.relative_to(ROOT)} header mismatch")
    if (
        PROMPT_SUFFICIENCY_BLIND_MATRIX_PATH.exists()
        and blind_matrix_headers != PROMPT_SUFFICIENCY_BLIND_MATRIX_HEADERS
    ):
        errors.append(f"{PROMPT_SUFFICIENCY_BLIND_MATRIX_PATH.relative_to(ROOT)} header mismatch")

    run_ids = [row.get("run_id", "") for row in runs]
    run_counts = Counter(run_ids)
    for run_id, count in sorted(run_counts.items()):
        if not run_id:
            errors.append("runs.csv contains a blank run_id")
        elif count > 1:
            errors.append(f"duplicate run_id in runs.csv: {run_id}")

    run_by_id = {row["run_id"]: row for row in runs if row.get("run_id")}
    fail_counts: dict[str, int] = defaultdict(int)
    comparison_status_by_run_category: dict[tuple[str, str, str], int] = defaultdict(int)

    for index, row in enumerate(runs, start=2):
        run_id = row.get("run_id", "")
        if row.get("result_status") not in RUN_STATUSES:
            errors.append(f"runs.csv:{index} invalid result_status for {run_id}")
        if row.get("full_verification_result") not in VERIFICATION_STATUSES:
            errors.append(f"runs.csv:{index} invalid full_verification_result for {run_id}")
        if row.get("manual_fix_required") not in BOOLS:
            errors.append(f"runs.csv:{index} invalid manual_fix_required for {run_id}")
        if row.get("skill_updated") not in BOOLS:
            errors.append(f"runs.csv:{index} invalid skill_updated for {run_id}")
        if not relative_exists(row.get("notes_path", "")):
            errors.append(f"runs.csv:{index} missing notes_path for {run_id}: {row.get('notes_path')}")

        total = row.get("convention_violations_total", "")
        if total == "" or not total.isdigit():
            errors.append(f"runs.csv:{index} invalid convention_violations_total for {run_id}")

        if is_strict_benchmark(run_id):
            for field in ("started_at", "finished_at", "duration_min"):
                if not row.get(field):
                    errors.append(f"runs.csv:{index} missing {field} for strict benchmark {run_id}")

            started_at = parse_datetime(row.get("started_at", ""))
            finished_at = parse_datetime(row.get("finished_at", ""))
            duration = numeric(row.get("duration_min", ""))
            if started_at is None:
                errors.append(f"runs.csv:{index} invalid started_at for {run_id}")
            if finished_at is None:
                errors.append(f"runs.csv:{index} invalid finished_at for {run_id}")
            if duration is None:
                errors.append(f"runs.csv:{index} invalid duration_min for {run_id}")
            if started_at and finished_at and duration is not None:
                actual_minutes = (finished_at - started_at).total_seconds() / 60
                if actual_minutes < 0:
                    errors.append(f"runs.csv:{index} finished_at precedes started_at for {run_id}")
                elif abs(actual_minutes - duration) > 1.0:
                    warnings.append(
                        f"runs.csv:{index} duration_min differs from timestamps for {run_id}: "
                        f"recorded={duration:.1f}, actual={actual_minutes:.1f}"
                    )

        if row.get("result_status") == "pass" and row.get("full_verification_result") != "pass":
            message = f"runs.csv:{index} pass run without passing full verification: {run_id}"
            if is_strict_benchmark(run_id):
                errors.append(message)
            else:
                warnings.append(message)

    for index, row in enumerate(comparisons, start=2):
        run_id = row.get("run_id", "")
        category = row.get("category", "")
        status = row.get("status", "")
        if run_id not in run_by_id:
            errors.append(f"convention-comparisons.csv:{index} unknown run_id: {run_id}")
        if not category:
            errors.append(f"convention-comparisons.csv:{index} blank category for {run_id}")
        if status not in COMPARISON_STATUSES:
            errors.append(f"convention-comparisons.csv:{index} invalid status for {run_id}")
        if row.get("severity") not in SEVERITIES:
            errors.append(f"convention-comparisons.csv:{index} invalid severity for {run_id}")
        if row.get("skill_update_needed") not in BOOLS:
            errors.append(f"convention-comparisons.csv:{index} invalid skill_update_needed for {run_id}")

        comparison_status_by_run_category[(run_id, category, status)] += 1

        if category == "real_concurrent_test" and status == "pass":
            target_path = run_by_id.get(run_id, {}).get("target_path", "")
            if not has_real_concurrent_test(target_path):
                errors.append(
                    f"convention-comparisons.csv:{index} real_concurrent_test pass "
                    f"without real concurrent test marker for {run_id}"
                )

        if status == "fail":
            fail_counts[run_id] += 1
            if row.get("severity") in {"P0", "P1"} and not row.get("evidence"):
                errors.append(f"convention-comparisons.csv:{index} missing evidence for {run_id} failure")
        if is_strict_benchmark(run_id) and row.get("skill_update_needed") == "true":
            warnings.append(f"strict benchmark comparison needs skill update: {run_id}")

    for run_id, row in sorted(run_by_id.items()):
        if is_strict_benchmark(run_id):
            expected = int(row.get("convention_violations_total", "0"))
            actual = fail_counts.get(run_id, 0)
            if expected != actual:
                errors.append(
                    f"strict benchmark violation mismatch for {run_id}: "
                    f"runs.csv={expected}, convention-comparisons.csv fails={actual}"
                )

    matrix_ids = [row.get("run_id", "") for row in matrix_rows]
    matrix_counts = Counter(matrix_ids)
    for run_id, count in sorted(matrix_counts.items()):
        if not run_id:
            errors.append("requirement-robustness-matrix.csv contains a blank run_id")
        elif count > 1:
            errors.append(f"duplicate run_id in requirement-robustness-matrix.csv: {run_id}")

    for index, row in enumerate(matrix_rows, start=2):
        run_id = row.get("run_id", "")
        status = row.get("status", "")
        if not run_id.startswith("roomescape-robustness-"):
            errors.append(f"requirement-robustness-matrix.csv:{index} invalid robustness run_id: {run_id}")
        if status not in MATRIX_STATUSES:
            errors.append(f"requirement-robustness-matrix.csv:{index} invalid status for {run_id}")
        for field in ("requirement_variant_path", "semantic_checklist_path", "baseline_path"):
            if not relative_exists(row.get(field, "")):
                add_missing_path_error(
                    errors,
                    "requirement-robustness-matrix.csv",
                    index,
                    field,
                    run_id,
                    row.get(field, ""),
                )
        if status != "planned" and run_id not in run_by_id:
            errors.append(f"requirement-robustness-matrix.csv:{index} completed matrix run missing runs.csv row: {run_id}")
        if status == "planned" and run_id in run_by_id:
            warnings.append(f"requirement-robustness-matrix.csv:{index} planned run already appears in runs.csv: {run_id}")
        if status in {"pass", "fail", "corrected"} and not relative_exists(row.get("target_path", "")):
            errors.append(f"requirement-robustness-matrix.csv:{index} completed run missing target_path: {run_id}")

    case_ids = [row.get("case_id", "") for row in failure_matrix_rows]
    case_counts = Counter(case_ids)
    for case_id, count in sorted(case_counts.items()):
        if not case_id:
            errors.append("failure-recovery-matrix.csv contains a blank case_id")
        elif count > 1:
            errors.append(f"duplicate case_id in failure-recovery-matrix.csv: {case_id}")

    for index, row in enumerate(failure_matrix_rows, start=2):
        case_id = row.get("case_id", "")
        baseline_status = row.get("baseline_status", "")
        rerun_status = row.get("rerun_status", "")
        if not case_id.startswith("failure-recovery-"):
            errors.append(f"failure-recovery-matrix.csv:{index} invalid case_id: {case_id}")
        if row.get("failure_type") not in FAILURE_TYPES:
            errors.append(f"failure-recovery-matrix.csv:{index} invalid failure_type for {case_id}")
        if row.get("prompt_language") not in PROMPT_LANGUAGES:
            errors.append(f"failure-recovery-matrix.csv:{index} invalid prompt_language for {case_id}")
        if baseline_status not in MATRIX_STATUSES:
            errors.append(f"failure-recovery-matrix.csv:{index} invalid baseline_status for {case_id}")
        if rerun_status not in MATRIX_STATUSES:
            errors.append(f"failure-recovery-matrix.csv:{index} invalid rerun_status for {case_id}")
        if row.get("validated") not in BOOLS:
            errors.append(f"failure-recovery-matrix.csv:{index} invalid validated for {case_id}")

        baseline_run_id = row.get("baseline_run_id", "")
        rerun_id = row.get("rerun_id", "")
        if baseline_run_id and not baseline_run_id.startswith("roomescape-failure-"):
            errors.append(f"failure-recovery-matrix.csv:{index} invalid baseline_run_id for {case_id}")
        if rerun_id and not rerun_id.startswith("roomescape-failure-"):
            errors.append(f"failure-recovery-matrix.csv:{index} invalid rerun_id for {case_id}")
        if baseline_status != "planned" and baseline_run_id not in run_by_id:
            errors.append(f"failure-recovery-matrix.csv:{index} baseline run missing runs.csv row: {case_id}")
        if baseline_status == "planned" and baseline_run_id in run_by_id:
            warnings.append(f"failure-recovery-matrix.csv:{index} planned baseline already appears in runs.csv: {case_id}")
        if rerun_status not in {"planned", "skipped"} and rerun_id not in run_by_id:
            errors.append(f"failure-recovery-matrix.csv:{index} rerun missing runs.csv row: {case_id}")
        if rerun_status == "planned" and rerun_id in run_by_id:
            warnings.append(f"failure-recovery-matrix.csv:{index} planned rerun already appears in runs.csv: {case_id}")
        if row.get("failure_type") == "evaluator_blind_spot" and rerun_status == "corrected":
            if comparison_status_by_run_category.get((rerun_id, "real_concurrent_test", "fail"), 0) == 0:
                errors.append(
                    f"failure-recovery-matrix.csv:{index} corrected evaluator blind spot "
                    f"missing failed real_concurrent_test comparison: {case_id}"
                )
            if comparison_status_by_run_category.get((rerun_id, "evaluator_blind_spot", "pass"), 0) == 0:
                errors.append(
                    f"failure-recovery-matrix.csv:{index} corrected evaluator blind spot "
                    f"missing passing evaluator_blind_spot comparison: {case_id}"
                )

        requirement_path = row.get("requirement_path", "")
        if baseline_status in COMPLETED_MATRIX_STATUSES and not relative_exists(requirement_path):
            add_missing_path_error(errors, "failure-recovery-matrix.csv", index, "requirement_path", case_id, requirement_path)
        elif requirement_path and not relative_exists(requirement_path):
            add_missing_path_error(errors, "failure-recovery-matrix.csv", index, "requirement_path", case_id, requirement_path)

        baseline_path = row.get("baseline_path", "")
        if not relative_exists(baseline_path):
            add_missing_path_error(errors, "failure-recovery-matrix.csv", index, "baseline_path", case_id, baseline_path)

        target_path = row.get("target_path", "")
        if (
            baseline_status in COMPLETED_MATRIX_STATUSES or rerun_status in COMPLETED_MATRIX_STATUSES
        ) and not relative_exists(target_path):
            add_missing_path_error(errors, "failure-recovery-matrix.csv", index, "target_path", case_id, target_path)

        intervention_path = row.get("intervention_path", "")
        if rerun_status in COMPLETED_MATRIX_STATUSES and not relative_exists(intervention_path):
            add_missing_path_error(
                errors,
                "failure-recovery-matrix.csv",
                index,
                "intervention_path",
                case_id,
                intervention_path,
            )
        elif intervention_path and not relative_exists(intervention_path):
            add_missing_path_error(
                errors,
                "failure-recovery-matrix.csv",
                index,
                "intervention_path",
                case_id,
                intervention_path,
            )

    scan_paths = [
        ROOT / "README.md",
        ROOT / "benchmarks" / "README.md",
        ROOT / "benchmarks" / "benchmark-records",
        ROOT / "benchmarks" / "reports",
    ]
    stale_patterns = ["benchmarks/records", "benchmarks/repeats", "챕터 정리"]
    for scan_path in scan_paths:
        paths = [scan_path] if scan_path.is_file() else list(scan_path.rglob("*"))
        for path in paths:
            if path.is_file() and path.suffix in {".md", ".csv"}:
                text = path.read_text(encoding="utf-8")
                for pattern in stale_patterns:
                    if pattern in text:
                        errors.append(f"stale path reference {pattern!r} in {path.relative_to(ROOT)}")

    prompt_run_ids = [row.get("run_id", "") for row in prompt_matrix_rows]
    prompt_run_counts = Counter(prompt_run_ids)
    for run_id, count in sorted(prompt_run_counts.items()):
        if not run_id:
            errors.append("prompt-sufficiency-matrix.csv contains a blank run_id")
        elif count > 1:
            errors.append(f"duplicate run_id in prompt-sufficiency-matrix.csv: {run_id}")

    for index, row in enumerate(prompt_matrix_rows, start=2):
        run_id = row.get("run_id", "")
        status = row.get("status", "")
        if not run_id.startswith("roomescape-prompt-"):
            errors.append(f"prompt-sufficiency-matrix.csv:{index} invalid run_id: {run_id}")
        if row.get("feature") not in PROMPT_SUFFICIENCY_FEATURES:
            errors.append(f"prompt-sufficiency-matrix.csv:{index} invalid feature for {run_id}")
        if row.get("prompt_language") not in PROMPT_LANGUAGES:
            errors.append(f"prompt-sufficiency-matrix.csv:{index} invalid prompt_language for {run_id}")
        if row.get("prompt_level") not in PROMPT_LEVELS:
            errors.append(f"prompt-sufficiency-matrix.csv:{index} invalid prompt_level for {run_id}")
        if row.get("repeat") not in PROMPT_REPEATS:
            errors.append(f"prompt-sufficiency-matrix.csv:{index} invalid repeat for {run_id}")
        if status not in MATRIX_STATUSES:
            errors.append(f"prompt-sufficiency-matrix.csv:{index} invalid status for {run_id}")
        for field in ("case_path", "oracle_path", "baseline_path"):
            if not relative_exists(row.get(field, "")):
                add_missing_path_error(
                    errors,
                    "prompt-sufficiency-matrix.csv",
                    index,
                    field,
                    run_id,
                    row.get(field, ""),
                )
        if status != "planned" and run_id not in run_by_id:
            errors.append(f"prompt-sufficiency-matrix.csv:{index} completed matrix run missing runs.csv row: {run_id}")
        if status == "planned" and run_id in run_by_id:
            warnings.append(f"prompt-sufficiency-matrix.csv:{index} planned run already appears in runs.csv: {run_id}")
        if status in COMPLETED_MATRIX_STATUSES and not relative_exists(row.get("target_path", "")):
            errors.append(f"prompt-sufficiency-matrix.csv:{index} completed run missing target_path: {run_id}")

    blind_run_ids = [row.get("run_id", "") for row in blind_matrix_rows]
    blind_run_counts = Counter(blind_run_ids)
    for run_id, count in sorted(blind_run_counts.items()):
        if not run_id:
            errors.append("prompt-sufficiency-blind-matrix.csv contains a blank run_id")
        elif count > 1:
            errors.append(f"duplicate run_id in prompt-sufficiency-blind-matrix.csv: {run_id}")

    for index, row in enumerate(blind_matrix_rows, start=2):
        run_id = row.get("run_id", "")
        status = row.get("status", "")
        if not run_id.startswith("roomescape-prompt-blind-"):
            errors.append(f"prompt-sufficiency-blind-matrix.csv:{index} invalid run_id: {run_id}")
        if row.get("feature") not in PROMPT_SUFFICIENCY_FEATURES:
            errors.append(f"prompt-sufficiency-blind-matrix.csv:{index} invalid feature for {run_id}")
        if row.get("prompt_language") not in PROMPT_LANGUAGES:
            errors.append(f"prompt-sufficiency-blind-matrix.csv:{index} invalid prompt_language for {run_id}")
        if row.get("prompt_level") not in BLIND_PROMPT_LEVELS:
            errors.append(f"prompt-sufficiency-blind-matrix.csv:{index} invalid prompt_level for {run_id}")
        if row.get("repeat") not in PROMPT_REPEATS:
            errors.append(f"prompt-sufficiency-blind-matrix.csv:{index} invalid repeat for {run_id}")
        if status not in BLIND_MATRIX_STATUSES:
            errors.append(f"prompt-sufficiency-blind-matrix.csv:{index} invalid status for {run_id}")
        if row.get("isolation_status") not in BLIND_ISOLATION_STATUSES:
            errors.append(f"prompt-sufficiency-blind-matrix.csv:{index} invalid isolation_status for {run_id}")
        if row.get("clarification_needed") and row.get("clarification_needed") not in BOOLS:
            errors.append(f"prompt-sufficiency-blind-matrix.csv:{index} invalid clarification_needed for {run_id}")

        for field in ("case_path", "oracle_path", "baseline_path"):
            if not relative_exists(row.get(field, "")):
                add_missing_path_error(
                    errors,
                    "prompt-sufficiency-blind-matrix.csv",
                    index,
                    field,
                    run_id,
                    row.get(field, ""),
                )

        if status != "planned" and run_id not in run_by_id:
            errors.append(f"prompt-sufficiency-blind-matrix.csv:{index} completed matrix run missing runs.csv row: {run_id}")
        if status == "planned" and run_id in run_by_id:
            warnings.append(f"prompt-sufficiency-blind-matrix.csv:{index} planned run already appears in runs.csv: {run_id}")
        if status in BLIND_COMPLETED_MATRIX_STATUSES:
            if not relative_exists(row.get("target_path", "")):
                errors.append(f"prompt-sufficiency-blind-matrix.csv:{index} completed run missing target_path: {run_id}")
            if not row.get("source_hash"):
                errors.append(f"prompt-sufficiency-blind-matrix.csv:{index} completed run missing source_hash: {run_id}")
            if not row.get("source_similarity"):
                errors.append(f"prompt-sufficiency-blind-matrix.csv:{index} completed run missing source_similarity: {run_id}")
            if row.get("clarification_needed") not in BOOLS:
                errors.append(f"prompt-sufficiency-blind-matrix.csv:{index} completed run missing clarification_needed: {run_id}")
            if not relative_exists(row.get("notes_path", "")):
                errors.append(f"prompt-sufficiency-blind-matrix.csv:{index} completed run missing notes_path: {run_id}")

    return errors, warnings


def main() -> int:
    errors, warnings = validate()
    for warning in warnings:
        print(f"WARN: {warning}")
    for error in errors:
        print(f"ERROR: {error}")
    if errors:
        print(f"Benchmark record validation failed: {len(errors)} error(s), {len(warnings)} warning(s)")
        return 1
    print(f"Benchmark record validation passed: {len(warnings)} warning(s)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
