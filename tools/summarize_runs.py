#!/usr/bin/env python3
"""Summarize batch run CSV files produced by ConsoleRunner."""

from __future__ import annotations

import argparse
import csv
import statistics
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, List, Sequence, Tuple


@dataclass(frozen=True)
class GroupSpec:
    name: str
    transitions: List[str]


def parse_group_spec(raw: str) -> GroupSpec:
    if "=" not in raw:
        raise ValueError(f"Invalid --group '{raw}'. Expected format name=T2,T5,T7")

    name, transitions_raw = raw.split("=", 1)
    name = name.strip()
    if not name:
        raise ValueError(f"Invalid --group '{raw}'. Group name cannot be empty")

    transitions = [token.strip() for token in transitions_raw.split(",") if token.strip()]
    if not transitions:
        raise ValueError(
            f"Invalid --group '{raw}'. Provide at least one transition column"
        )

    for transition in transitions:
        if not transition.startswith("T") or not transition[1:].isdigit():
            raise ValueError(
                f"Invalid transition '{transition}' in --group '{raw}'. Use T<number>"
            )

    return GroupSpec(name=name, transitions=transitions)


def parse_int(value: str, column: str, row_number: int) -> int:
    try:
        return int(value)
    except Exception as exc:  # pragma: no cover - defensive parsing branch
        raise ValueError(
            f"Row {row_number}: cannot parse integer for column '{column}': {value}"
        ) from exc


def parse_bool(value: str) -> bool:
    return str(value).strip().lower() == "true"


def mean(values: Sequence[float]) -> float:
    return statistics.fmean(values) if values else 0.0


def pstdev(values: Sequence[float]) -> float:
    if not values:
        return 0.0
    return statistics.pstdev(values)


def format_stats(values: Sequence[float]) -> Tuple[float, float, float, float]:
    if not values:
        return (0.0, 0.0, 0.0, 0.0)
    return (mean(values), pstdev(values), min(values), max(values))


def load_rows(csv_path: Path) -> List[Dict[str, str]]:
    try:
        with csv_path.open("r", encoding="utf-8", newline="") as fh:
            reader = csv.DictReader(fh)
            rows = list(reader)
            if reader.fieldnames is None:
                raise ValueError("CSV header is missing")
            return rows
    except FileNotFoundError as exc:
        raise ValueError(f"CSV file not found: {csv_path}") from exc


def ensure_columns(
        available_columns: Sequence[str], required_columns: Sequence[str], context: str
) -> None:
    missing = [column for column in required_columns if column not in available_columns]
    if missing:
        missing_joined = ", ".join(missing)
        raise ValueError(f"Missing required column(s) for {context}: {missing_joined}")


def print_global_summary(
        csv_path: Path, duration_values: Sequence[float], valid_count: int, total_runs: int
) -> None:
    duration_mean, duration_stddev, duration_min, duration_max = format_stats(duration_values)

    print(f"File: {csv_path}")
    print(f"Runs: {total_runs}")
    print(
        "Duration (ms): "
        f"mean={duration_mean:.2f}, stddev={duration_stddev:.2f}, "
        f"min={duration_min:.0f}, max={duration_max:.0f}"
    )

    valid_pct = (100.0 * valid_count / total_runs) if total_runs else 0.0
    print(
        "Transition-invariant composition valid: "
        f"{valid_count}/{total_runs} ({valid_pct:.2f}%)"
    )


def print_group_summary(group: GroupSpec, values_by_transition: Dict[str, List[float]]) -> None:
    print()
    print(f"Group {group.name}: {', '.join(group.transitions)}")
    print(f"{'Transition':<12}{'mean%':>10}{'stddev':>10}{'min':>10}{'max':>10}")

    for transition in group.transitions:
        m, s, min_v, max_v = format_stats(values_by_transition[transition])
        print(f"{transition:<12}{m:>10.2f}{s:>10.2f}{min_v:>10.2f}{max_v:>10.2f}")


def main(argv: Sequence[str]) -> int:
    parser = argparse.ArgumentParser(
        description="Summarize per-run CSVs produced by batch mode"
    )
    parser.add_argument("csv_path", help="Path to *_runs.csv")
    parser.add_argument(
        "--group",
        action="append",
        default=[],
        metavar="NAME=T2,T5,T7",
        help="Transition group used to compute per-run percentage statistics",
    )

    args = parser.parse_args(argv)

    groups: List[GroupSpec] = [parse_group_spec(raw) for raw in args.group]
    csv_path = Path(args.csv_path)
    rows = load_rows(csv_path)

    if not rows:
        raise ValueError(f"CSV has no data rows: {csv_path}")

    header = list(rows[0].keys())
    ensure_columns(
        header,
        ["durationMs", "transitionInvariantCompositionValid"],
        "global metrics",
    )

    for group in groups:
        ensure_columns(header, group.transitions, f"group '{group.name}'")

    durations: List[float] = []
    valid_count = 0

    values_by_group: Dict[str, Dict[str, List[float]]] = {
        group.name: {transition: [] for transition in group.transitions} for group in groups
    }

    for row_index, row in enumerate(rows, start=2):
        durations.append(parse_int(row["durationMs"], "durationMs", row_index))
        valid_count += 1 if parse_bool(row["transitionInvariantCompositionValid"]) else 0

        for group in groups:
            transition_counts = [
                parse_int(row[transition], transition, row_index)
                for transition in group.transitions
            ]
            total = sum(transition_counts)

            for transition, count in zip(group.transitions, transition_counts):
                pct = (100.0 * count / total) if total > 0 else 0.0
                values_by_group[group.name][transition].append(pct)

    print_global_summary(csv_path, durations, valid_count, len(rows))

    for group in groups:
        print_group_summary(group, values_by_group[group.name])

    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main(sys.argv[1:]))
    except ValueError as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        raise SystemExit(1)