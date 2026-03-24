import csv
import re
from dataclasses import dataclass

# ===== Regex del formato actual =====
HEADER = "Timestamp,Thread,Transition,MarkingBefore,MarkingAfter"
FIRE_RE = re.compile(
    r'^\d{13},[A-Za-z0-9._-]+,\d+,"\[-?\d+(?:;\s-?\d+)*\]","\[-?\d+(?:;\s-?\d+)*\]"$'
)
END_RE = re.compile(r"^--- End of simulation: .+ ---$")

# ===== Modelo Producer-Consumer (9 plazas, 6 transiciones), formato [p][t] =====
PRE = [
    [1, 0, 0, 0, 0, 0],
    [0, 1, 0, 0, 0, 0],
    [0, 0, 1, 0, 0, 0],
    [0, 0, 0, 1, 0, 0],
    [0, 0, 0, 0, 1, 0],
    [0, 0, 0, 0, 0, 1],
    [1, 0, 0, 0, 0, 0],
    [1, 0, 0, 1, 0, 0],
    [0, 0, 0, 1, 0, 0],
]
POST = [
    [0, 0, 1, 0, 0, 0],
    [1, 0, 0, 0, 0, 0],
    [0, 1, 0, 0, 0, 0],
    [0, 0, 0, 0, 0, 1],
    [0, 0, 0, 1, 0, 0],
    [0, 0, 0, 0, 1, 0],
    [0, 0, 0, 0, 1, 0],
    [0, 1, 0, 0, 1, 0],
    [0, 1, 0, 0, 0, 0],
]

NUM_PLACES = len(PRE)
NUM_TRANSITIONS = len(PRE[0])

INCIDENCE = [[POST[p][t] - PRE[p][t] for t in range(NUM_TRANSITIONS)] for p in range(NUM_PLACES)]

# Invariantes conocidas de tu red
INVARIANTS = [
    ("producer_cycle", [1, 1, 1, 0, 0, 0, 0, 0, 0], 2),
    ("consumer_cycle", [0, 0, 0, 1, 1, 1, 0, 0, 0], 2),
    ("mutex",          [0, 1, 0, 0, 1, 0, 0, 1, 0], 1),
    ("buffer",         [0, 1, 0, 0, 1, 0, 1, 0, 1], 3),
]


@dataclass
class Event:
    timestamp: int
    thread: str
    transition: int
    before: list[int]
    after: list[int]


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


def check_event_semantics(event: Event, line_no: int) -> list[str]:
    errors = []

    t = event.transition
    before = event.before
    after = event.after

    if not (0 <= t < NUM_TRANSITIONS):
        errors.append(f"Line {line_no}: transition {t} out of bounds [0..{NUM_TRANSITIONS-1}]")
        return errors

    if len(before) != NUM_PLACES:
        errors.append(f"Line {line_no}: before length {len(before)} != {NUM_PLACES}")
    if len(after) != NUM_PLACES:
        errors.append(f"Line {line_no}: after length {len(after)} != {NUM_PLACES}")

    if errors:
        return errors

    # 1) transición sensibilizada en 'before'
    for p in range(NUM_PLACES):
        if before[p] < PRE[p][t]:
            errors.append(
                f"Line {line_no}: T{t} fired while disabled (place p{p}: {before[p]} < pre={PRE[p][t]})"
            )
            break

    # 2) after = before + incidencia
    expected_after = [before[p] + INCIDENCE[p][t] for p in range(NUM_PLACES)]
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

    # 4) invariantes
    for name, inv, expected in INVARIANTS:
        val = dot(inv, after)
        if val != expected:
            errors.append(
                f"Line {line_no}: invariant '{name}' broken. value={val}, expected={expected}"
            )

    return errors


def verify_log(file_path: str) -> int:
    errors = []
    events_count = 0
    end_line_seen = 0
    prev_after = None

    with open(file_path, "r", encoding="utf-8") as f:
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

        # Consistencia global: after anterior == before actual
        if prev_after is not None and ev.before != prev_after:
            errors.append(
                f"Line {line_no}: chain break. prev_after={prev_after}, current_before={ev.before}"
            )

        errors.extend(check_event_semantics(ev, line_no))
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
        print(f" ... and {len(errors)-50} more")

    return 0 if not errors else 1


if __name__ == "__main__":
    raise SystemExit(verify_log("petri_log.txt"))