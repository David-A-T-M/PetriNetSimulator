import argparse
import csv
import json
import re
from dataclasses import dataclass
from typing import Any


# ===== Regex del formato actual =====
HEADER = "Timestamp,Thread,Transition,MarkingBefore,MarkingAfter"
FIRE_RE = re.compile(
    r'^\d{13},[A-Za-z0-9._-]+,\d+,"\[-?\d+(?:;\s-?\d+)*\]","\[-?\d+(?:;\s-?\d+)*\]"$'
)
END_RE = re.compile(r"^--- End of simulation: .+ ---$")


@dataclass
class Event:
    timestamp: int
    thread: str
    transition: int
    before: list[int]
    after: list[int]


@dataclass
class Invariant:
    name: str
    vector: list[int]
    constant: int


@dataclass
class Model:
    pre: list[list[int]]
    post: list[list[int]]
    incidence: list[list[int]]
    initial_marking: list[int]
    invariants: list[Invariant]
    num_places: int
    num_transitions: int


def parse_marking(s: str) -> list[int]:
    # Ej: "[2; 0; 1]"
    if not (s.startswith("[") and s.endswith("]")):
        raise ValueError(f"Invalid marking format: {s}")
    body = s[1:-1].strip()
    if body == "":
        return []
    return [int(x.strip()) for x in body.split(";")]


def parse_event_line(line: str, line_no: int) -> Event:
    if not FIRE_RE.match(line):
        raise ValueError(f"Line {line_no}: does not match FIRE regex")

    row = next(csv.reader([line]))
    if len(row) != 5:
        raise ValueError(f"Line {line_no}: expected 5 CSV columns, got {len(row)}")

    ts_s, thread, t_s, before_s, after_s = row
    timestamp = int(ts_s)
    transition = int(t_s)
    before = parse_marking(before_s)
    after = parse_marking(after_s)

    return Event(timestamp, thread, transition, before, after)


def dot(a: list[int], b: list[int]) -> int:
    return sum(x * y for x, y in zip(a, b))


def _require_list(value: Any, field_name: str) -> list[Any]:
    if not isinstance(value, list):
        raise ValueError(f"'{field_name}' must be a JSON array")
    return value


def _validate_matrix(matrix: list[list[int]], name: str) -> None:
    if not matrix:
        raise ValueError(f"'{name}' cannot be empty")
    row_len = None
    for r, row in enumerate(matrix):
        if not isinstance(row, list):
            raise ValueError(f"'{name}[{r}]' must be an array")
        if row_len is None:
            row_len = len(row)
            if row_len == 0:
                raise ValueError(f"'{name}' must have at least one transition")
        elif len(row) != row_len:
            raise ValueError(f"'{name}' must be rectangular (non-jagged)")
        for c, val in enumerate(row):
            if not isinstance(val, int):
                raise ValueError(f"'{name}[{r}][{c}]' must be an integer")
            if val < 0:
                raise ValueError(f"'{name}[{r}][{c}]' cannot be negative")


def load_model_from_config(config_path: str) -> Model:
    with open(config_path, "r", encoding="utf-8") as f:
        config = json.load(f)

    if not isinstance(config, dict):
        raise ValueError("Root JSON must be an object")

    net = config.get("net")
    if not isinstance(net, dict):
        raise ValueError("'net' object is required")

    pre_raw = _require_list(net.get("pre"), "net.pre")
    post_raw = _require_list(net.get("post"), "net.post")
    initial_marking = _require_list(net.get("initialMarking"), "net.initialMarking")

    pre = [list(row) for row in pre_raw]
    post = [list(row) for row in post_raw]

    _validate_matrix(pre, "net.pre")
    _validate_matrix(post, "net.post")

    num_places = len(pre)
    num_transitions = len(pre[0])

    if len(post) != num_places:
        raise ValueError("net.post must have the same number of places as net.pre")
    if len(post[0]) != num_transitions:
        raise ValueError("net.post must have the same number of transitions as net.pre")

    if len(initial_marking) != num_places:
        raise ValueError(
            f"net.initialMarking length {len(initial_marking)} != number of places {num_places}"
        )
    for i, tk in enumerate(initial_marking):
        if not isinstance(tk, int):
            raise ValueError(f"net.initialMarking[{i}] must be an integer")
        if tk < 0:
            raise ValueError(f"net.initialMarking[{i}] cannot be negative")

    incidence = [[post[p][t] - pre[p][t] for t in range(num_transitions)] for p in range(num_places)]

    invariants_cfg = config.get("placeInvariants", [])
    invariants_list = _require_list(invariants_cfg, "placeInvariants")

    invariants: list[Invariant] = []
    for i, inv_cfg in enumerate(invariants_list):
        if not isinstance(inv_cfg, dict):
            raise ValueError(f"placeInvariants[{i}] must be an object")

        places = _require_list(inv_cfg.get("places"), f"placeInvariants[{i}].places")
        constant = inv_cfg.get("constant")

        if not isinstance(constant, int):
            raise ValueError(f"placeInvariants[{i}].constant must be an integer")
        if constant < 0:
            raise ValueError(f"placeInvariants[{i}].constant cannot be negative")
        if not places:
            raise ValueError(f"placeInvariants[{i}].places cannot be empty")

        for j, v in enumerate(places):
            if not isinstance(v, int):
                raise ValueError(f"placeInvariants[{i}].places[{j}] must be an integer")

        # Mode A: coefficient vector (same length as number of places)
        if len(places) == num_places:
            vector = places.copy()
            for j, coeff in enumerate(vector):
                if coeff < 0:
                    raise ValueError(
                        f"placeInvariants[{i}].places[{j}] coefficient cannot be negative"
                    )
        else:
            # Mode B: list of place indexes
            vector = [0] * num_places
            for j, p in enumerate(places):
                if p < 0 or p >= num_places:
                    raise ValueError(
                        f"placeInvariants[{i}].places[{j}]={p} out of bounds [0..{num_places - 1}]"
                    )
                vector[p] += 1

        invariants.append(
            Invariant(
                name=f"invariant_{i}",
                vector=vector,
                constant=constant,
            )
        )

    return Model(
        pre=pre,
        post=post,
        incidence=incidence,
        initial_marking=[int(x) for x in initial_marking],
        invariants=invariants,
        num_places=num_places,
        num_transitions=num_transitions,
    )


def check_event_semantics(event: Event, line_no: int, model: Model) -> list[str]:
    errors = []

    t = event.transition
    before = event.before
    after = event.after

    if not (0 <= t < model.num_transitions):
        errors.append(
            f"Line {line_no}: transition {t} out of bounds [0..{model.num_transitions - 1}]"
        )
        return errors

    if len(before) != model.num_places:
        errors.append(f"Line {line_no}: before length {len(before)} != {model.num_places}")
    if len(after) != model.num_places:
        errors.append(f"Line {line_no}: after length {len(after)} != {model.num_places}")

    if errors:
        return errors

    # 1) transición sensibilizada en 'before'
    for p in range(model.num_places):
        if before[p] < model.pre[p][t]:
            errors.append(
                f"Line {line_no}: T{t} fired while disabled "
                f"(place p{p}: {before[p]} < pre={model.pre[p][t]})"
            )
            break

    # 2) after = before + incidencia
    expected_after = [before[p] + model.incidence[p][t] for p in range(model.num_places)]
    if after != expected_after:
        errors.append(
            f"Line {line_no}: marking mismatch after firing T{t}. "
            f"expected={expected_after}, got={after}"
        )

    # 3) no negatividad
    for p, tk in enumerate(after):
        if tk < 0:
            errors.append(f"Line {line_no}: negative tokens at p{p} => {tk}")
            break

    # 4) invariantes desde config (si hay)
    for inv in model.invariants:
        val = dot(inv.vector, after)
        if val != inv.constant:
            errors.append(
                f"Line {line_no}: invariant '{inv.name}' broken. "
                f"value={val}, expected={inv.constant}"
            )

    return errors


def verify_log(log_path: str, model: Model) -> int:
    errors = []
    events_count = 0
    end_line_seen = 0
    prev_after = None
    first_event_checked = False

    with open(log_path, "r", encoding="utf-8") as f:
        lines = [ln.rstrip("\n") for ln in f]

    if not lines:
        print("ERROR: empty log file")
        return 1

    if lines[0] != HEADER:
        errors.append("Line 1: invalid header")

    for i in range(1, len(lines)):
        line_no = i + 1
        line = lines[i]

        if END_RE.match(line):
            end_line_seen += 1
            if i != len(lines) - 1:
                errors.append(f"Line {line_no}: end line must be the last line")
            continue

        if line.strip() == "":
            errors.append(f"Line {line_no}: blank line not allowed")
            continue

        try:
            ev = parse_event_line(line, line_no)
        except Exception as e:
            errors.append(str(e))
            continue

        # Primer evento debe arrancar desde la marca inicial de config.
        if not first_event_checked:
            first_event_checked = True
            if ev.before != model.initial_marking:
                errors.append(
                    f"Line {line_no}: first before marking does not match initialMarking. "
                    f"expected={model.initial_marking}, got={ev.before}"
                )

        # Consistencia global: after anterior == before actual
        if prev_after is not None and ev.before != prev_after:
            errors.append(
                f"Line {line_no}: chain break. "
                f"prev_after={prev_after}, current_before={ev.before}"
            )

        errors.extend(check_event_semantics(ev, line_no, model))
        prev_after = ev.after
        events_count += 1

    if events_count == 0:
        errors.append("No fire events found")
    if end_line_seen > 1:
        errors.append("More than one end line found")

    print(f"Analyzed fire events: {events_count}")
    print(f"Errors: {len(errors)}")
    for e in errors[:50]:
        print(" -", e)
    if len(errors) > 50:
        print(f" ... and {len(errors) - 50} more")

    return 0 if not errors else 1


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Validate Petri net log format and semantics against JSON config."
    )
    parser.add_argument(
        "--config",
        default="config.json",
        help="Path to simulation config JSON (default: config.json)",
    )
    parser.add_argument(
        "--log",
        default="petri_log.csv",
        help="Path to simulation log file (default: petri_log.csv)",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    try:
        model = load_model_from_config(args.config)
    except Exception as e:
        print(f"CONFIG ERROR: {e}")
        return 2

    try:
        return verify_log(args.log, model)
    except FileNotFoundError:
        print(f"ERROR: log file not found: {args.log}")
        return 2
    except Exception as e:
        print(f"ERROR: unexpected failure validating log: {e}")
        return 2


if __name__ == "__main__":
    raise SystemExit(main())