# PetriNetSimulator

A configurable **Petri net simulator** in Java, focused on concurrent execution with monitor-based synchronization, policy-driven transition selection, CSV logging, and log validation.

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Requirements](#requirements)
- [Quick Start](#quick-start)
- [Configuration (JSON)](#configuration-json)
- [Logging and Validation](#logging-and-validation)
- [Regex Patterns](#regex-patterns)
- [Testing and Quality](#testing-and-quality)
- [Known Limitations](#known-limitations)

## Overview

`PetriNetSimulator` executes Petri-net-based workflows using:

- A structural model (`pre`, `post`, initial marking)
- A monitor (`Monitor`) to coordinate concurrent firing
- A pluggable policy (`Policy`) to choose among enabled transitions
- Timing windows per transition (`alpha` / `beta`)
- CSV logs for traceability and external verification

Main entry point: `src/main/java/ar/edu/unc/david/petrinetsimulator/Main.java`

## Features

- JSON-driven simulation setup (`ConfigLoader` + `SimulationConfig`)
- Generic net definition (not tied to producer-consumer hardcoding)
- Transition timing support (`alpha`, `beta`)
- Concurrent agents with per-agent transition sequences and cycle counts
- Policy-driven transition selection with `RANDOM` and `PROPORTION` modes
- Temporal windows with post-fire alpha restart when a transition remains enabled
- Built-in Java log structure validator (`PetriLogValidator`)
- External Python verifier for semantic checks (`verifier.py`)
- Quality gates: JUnit 5, Checkstyle, Spotless, JaCoCo

## Tech Stack

- Java 21
- Gradle (with wrapper)
- JUnit 5
- Checkstyle
- Spotless
- JaCoCo
- Jackson (JSON parsing)
- Python 3 (optional verifier utilities)

## Project Structure

- Core model:
  - `src/main/java/ar/edu/unc/david/petrinetsimulator/core/PetriNet.java`
  - `src/main/java/ar/edu/unc/david/petrinetsimulator/core/PetriNetMatrix.java`
- Concurrency:
  - `src/main/java/ar/edu/unc/david/petrinetsimulator/monitor/Monitor.java`
  - `src/main/java/ar/edu/unc/david/petrinetsimulator/monitor/WaitingQueues.java`
- Policies:
  - `src/main/java/ar/edu/unc/david/petrinetsimulator/policy/Policy.java`
  - `src/main/java/ar/edu/unc/david/petrinetsimulator/policy/PolicyFactory.java`
  - `src/main/java/ar/edu/unc/david/petrinetsimulator/policy/RandomPolicy.java`
  - `src/main/java/ar/edu/unc/david/petrinetsimulator/policy/ProportionPolicy.java`
- Logging:
  - `src/main/java/ar/edu/unc/david/petrinetsimulator/log/PetriLogger.java`
  - `src/main/java/ar/edu/unc/david/petrinetsimulator/log/PetriLogPatterns.java`
  - `src/main/java/ar/edu/unc/david/petrinetsimulator/log/PetriLogValidator.java`
- Config:
  - `src/main/java/ar/edu/unc/david/petrinetsimulator/config/ConfigLoader.java`
  - `src/main/java/ar/edu/unc/david/petrinetsimulator/config/SimulationConfig.java`
  - `src/main/java/ar/edu/unc/david/petrinetsimulator/config/NetConfig.java`
  - `src/main/java/ar/edu/unc/david/petrinetsimulator/config/TransitionConfig.java`
  - `src/main/java/ar/edu/unc/david/petrinetsimulator/config/AgentConfig.java`
  - `src/main/java/ar/edu/unc/david/petrinetsimulator/config/PolicyConfig.java`
  - `src/main/java/ar/edu/unc/david/petrinetsimulator/config/PolicyGroupConfig.java`
  - `src/main/java/ar/edu/unc/david/petrinetsimulator/config/LoggingConfig.java`
  - `src/main/java/ar/edu/unc/david/petrinetsimulator/config/RuntimeConfig.java`
  - `src/main/java/ar/edu/unc/david/petrinetsimulator/config/StopConfig.java`
  - `src/main/java/ar/edu/unc/david/petrinetsimulator/config/TransitionInvariantConfig.java`
  - `src/main/java/ar/edu/unc/david/petrinetsimulator/config/PlaceInvariantConfig.java`

## Requirements

- JDK 21
- Python 3 (optional, for `verifier.py` and analysis scripts)

## Quick Start

Run tests and checks:

```bash
./gradlew clean test
./gradlew check
```

Run simulation with default config (`config.json`):

```bash
./gradlew run
```

Run simulation selecting a specific network from `config.json`:

```bash
./gradlew run --args="--network data_proc"
```

Run batch mode (multiple runs with per-run and aggregate CSV statistics):

```bash
./gradlew run --args="--network data_proc --batch 30"
```

## Configuration (JSON)

The simulator is configured via the `SimulationConfig` records. Example:

```json
{
  "net": {
    "pre": [
      [1, 0, 0, 0, 0, 0],
      [0, 1, 0, 0, 0, 0],
      [0, 0, 1, 0, 0, 0],
      [0, 0, 0, 1, 0, 0],
      [0, 0, 0, 0, 1, 0],
      [0, 0, 0, 0, 0, 1],
      [1, 0, 0, 0, 0, 0],
      [1, 0, 0, 1, 0, 0],
      [0, 0, 0, 1, 0, 0]
    ],
    "post": [
      [0, 0, 1, 0, 0, 0],
      [1, 0, 0, 0, 0, 0],
      [0, 1, 0, 0, 0, 0],
      [0, 0, 0, 0, 0, 1],
      [0, 0, 0, 1, 0, 0],
      [0, 0, 0, 0, 1, 0],
      [0, 0, 1, 0, 0, 0],
      [0, 1, 0, 0, 1, 0],
      [0, 1, 0, 0, 0, 0]
    ],
    "initialMarking": [2, 0, 0, 2, 0, 0, 3, 1, 0],
    "transitions": [
      { "id": 0, "alpha": 0, "beta": 1000 },
      { "id": 1, "alpha": 0, "beta": 1000 },
      { "id": 2, "alpha": 0, "beta": 1000 },
      { "id": 3, "alpha": 0, "beta": 1000 },
      { "id": 4, "alpha": 0, "beta": 1000 },
      { "id": 5, "alpha": 0, "beta": 1000 }
    ]
  },
  "placeInvariants": [
    { "places": [1, 1, 1, 0, 0, 0, 0, 0, 0], "constant": 2 },
    { "places": [0, 0, 0, 1, 1, 1, 0, 0, 0], "constant": 2 }
  ],
  "agents": [
    { "namePrefix": "P", "count": 2, "sequence": [0, 1, 2], "cycles": -1 },
    { "namePrefix": "C", "count": 2, "sequence": [3, 4, 5], "cycles": -1 }
  ],
  "logging": {
    "file": "petri_log.csv",
    "logWaitWake": false
  },
  "runtime": {
    "joinTimeoutMs": 40000,
    "builtInValidation": true,
    "checkPlaceInvariants": true
  },
  "policy": {
    "type": "PROPORTION",
    "groups": [
      {
        "transitions": [2, 5, 7],
        "weights": [1, 1, 1]
      }
    ]
  },
  "stop": {
    "transition": 11,
    "count": 200
  },
  "transitionInvariants": [
    { "name": "simple", "sequence": [0, 1, 5, 6, 11] },
    { "name": "medium", "sequence": [0, 1, 2, 3, 4, 11] },
    { "name": "high", "sequence": [0, 1, 7, 8, 9, 10, 11] }
  ]
}
```

For simple runs, `policy.type` can still be `RANDOM` with an empty or omitted `groups` array. For proportional run, each
group declares the transitions that compete with one another and the weights assigned to each transition.

### Policy modes

- `RANDOM`: chooses one enabled competing transition uniformly at random.
- `PROPORTION`: chooses among competing transitions using configured group weights to bias long-run selection frequencies.

Example:

```json
{
  "policy": {
    "type": "PROPORTION",
    "groups": [
      { "transitions": [2, 5, 7], "weights": [1, 1, 1] }
    ]
  }
}
```

`transitionInvariants` defines admissible transition sequences for runtime log validation. The built-in validator checks
that the fired transition stream can be reduced as a composition of those invariants.

### Temporal semantics (`alpha` / `beta`)

- A transition is fireable only while elapsed sensitization time is within `[alpha, beta]`.
- If a transition is fired and remains enabled, its sensitization timestamp is reset and `alpha` is enforced again
  before an immediate re-fire.
- If a transition loses sensitization (becomes disabled), its temporal state is cleared and starts again when it becomes
  enabled later.
- If a transition is enabled by tokens but still before `alpha`, the thread waits outside the monitor lock until its
  time window opens and then retries. Condition-variable queues are used only for logical resource blocking
  (tokens/policy), not for temporal waiting.

Packaged networks use selective timing:

- `reserve_sys_logic.json`: positive `alpha` only for transitions `1, 4, 5, 8, 9, 10`.
- `data_proc_logic.json`: positive `alpha` only for transitions `1, 3, 4, 6, 8, 9, 10`.
- Other transitions use `alpha = 0` (no opening delay) and all keep a large `beta` windows.

## Logging and Validation

### Built-in Java validation

At the end of `main`, if `runtime.builtInValidation` is `true`, the simulator validates log structure using `PetriLogValidator`.

When `runtime.checkPlaceInvariants` is `true` (default), the monitor validates all configured place invariants
immediately after each successful transition fire and fails fast if one is violated.

When `transitionInvariants` is configured, built-in validation also checks transition-invariant composition over the
final fire-event stream and reports unmatched residues if composition fails.

### Batch statistics mode

`ConsoleRunner` accepts:

- `--network <id>`: optional, overrides `activeNetworkId` from `config.json`.
- `--batch <N>`: optional, runs the same network `N` times (`N >= 1`).

Outputs generated in batch mode:

- Per-run event logs: `runs/<network>_<policy>_runNNN.csv`
- Per-run summary (key/value CSV): `runs/<network>_<policy>_runNNN_summary.csv`
- Batch table (one row per run): `runs/<network>_<policy>_runs.csv`
- Batch aggregate (mean/stddev/min/max): `runs/<network>_<policy>_batch_summary.csv`

Per-run summaries include:

- total fires and durations,
- transition fire counts (`T0`, `T1`, ...),
- transition-invariant counts and percentages,
- composition validity and unmatched remainder (if any).

### External Python semantic validation

`verifier.py` validates:

- CSV format and syntax
- Event chain consistency (`after` == next `before`)
- Transition semantics (`before + incidence = after`)
- Place invariants loaded from JSON

Default files:

- Config: `config.json`
- Log: `petri_log.csv`

Run with defaults:

```bash
python3 verifier.py
```

Run with custom files:

```bash
python3 verifier.py --config path/to/config.json --log path/to/petri_log.csv
```

## Regex Patterns

Regex definitions are centralized in `src/main/java/ar/edu/unc/david/petrinetsimulator/log/PetriLogPatterns.java`.

- Header:

```regex
^Timestamp,Thread,Transition,MarkingBefore,MarkingAfter$
```

- Fire event:

```regex
^\d{13},[A-Za-z0-9._-]+,\d+,"\[-?\d+(?:;\s-?\d+)*\]","\[-?\d+(?:;\s-?\d+)*\]"$
```

- Trailer:

```regex
^--- End of simulation: .+ ---$
```

## Testing and Quality

Run the full quality pipeline:

```bash
./gradlew test
./gradlew checkstyleMain checkstyleTest
./gradlew spotlessCheck
./gradlew jacocoTestReport
```

Auto-format Java code:

```bash
./gradlew spotlessApply
```

Reports are generated under `build/reports/`.

## Validation artifacts

The repository includes PIPE exports in `docs/` that can be used to validate the structural definition of the packaged
networks against their JSON counterparts in `src/main/resources/`.

Current validation checks cover:

- `pre` and `post` matrices.
- the initial marking.
- the place and transition counts implied by the export.
- the optional global stop configuration used by the packaged networks.
- open-ended agent execution via negative cycle counts.

This validation is structural only. It does not execute the simulator.

The packaged JSON network files also include the runtime stop configuration used by the console runner, so a configured
network can end after the expected number of completion events instead of depending only on per-agent cycle limits.

Policy behavior is covered by unit tests for `ProportionPolicy` and by monitor integration tests that exercise policy
selection while threads are waiting.

Temporal behavior is covered by `PetriNetTest` cases for:

- re-fire while still enabled requiring a fresh `alpha` wait,
- immediate re-fire when `alpha = 0`,
- enabled-loss/enabled-recovery resetting temporal eligibility.

Place invariant runtime enforcement is covered by monitor tests for valid invariants, explicit violations, and the
runtime opt-out flag.

The monitor timing model is covered by integration tests where temporal eligibility and policy competition coexist,
ensuring time waiting does not occupy monitor condition queues.

## Known Limitations

- The policy groups are still declared manually in the network JSON files; the monitor does not infer them automatically
  from the matrices.
- Built-in Java validation checks log structure; deeper semantic validation is handled by `verifier.py`.
- Timing behavior depends on scheduling and monitor contention in concurrent runs.

