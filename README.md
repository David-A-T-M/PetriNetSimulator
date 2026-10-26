# PetriNetSimulator

A configurable **Petri net simulator** in Java, focused on concurrent execution with monitor-based synchronization,
policy-driven transition selection, CSV logging, and log validation.

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
- [Documentation](#documentation)

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

All paths are relative to `src/main/java/ar/edu/unc/david/petrinetsimulator/`:

- Entry points: `Main.java`, `runner/ConsoleRunner.java` (console and `--batch` mode)
- Core model: `core/PetriNet.java`, `core/PetriNetMatrix.java`, `core/SimulatorEngine.java`
- Concurrency: `monitor/Monitor.java`, `monitor/WaitingQueues.java`, `monitor/StopCondition.java`,
  `agent/PetriAgent.java`
- Policies: `policy/Policy.java`, `policy/PolicyFactory.java`, `policy/RandomPolicy.java`,
  `policy/ProportionPolicy.java`
- Logging and validation: `log/PetriLogger.java`, `log/PetriLogPatterns.java`, `log/PetriLogValidator.java`
- Run statistics: `runner/RunStatistics.java`, `runner/RunStatisticsAnalyzer.java`, `runner/RunStatisticsCsvWriter.java`
- Config loading: `config/ConfigLoader.java`
- Config records:
  - `config/main/`: `MainConfig`, `NetworkEntry`
  - `config/logic/`: `SimulationConfig`, `NetConfig`, `TransitionConfig`, `AgentConfig`, `PolicyConfig`,
    `PolicyGroupConfig`, `StopConfig`, `LoggingConfig`, `RuntimeConfig`, `PlaceInvariantConfig`,
    `TransitionInvariantConfig`
  - `config/layout/`: layout records used only by the UI
- JavaFX UI (optional): `ui/`, `ui/javafx/PetriSimApp.java`, `ui/nodes/`, `ui/monitor/`
- Packaged networks: `src/main/resources/{prod_cons,reserve_sys,data_proc}_{logic,layout}.json` (`reserve_sys` is the
  2024 assignment, `data_proc` the 2025 one)
- PIPE exports of the 2024/2025 networks: `docs/net_2024.xml`, `docs/net_2025.xml`

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
    "type": "RANDOM"
  },
  "stop": {
    "transition": 5,
    "count": 50
  },
  "transitionInvariants": [
    { "name": "producer", "sequence": [0, 1, 2] },
    { "name": "consumer", "sequence": [3, 4, 5] }
  ]
}
```

This example is the small producer-consumer net (`prod_cons_logic.json`); the TP networks are in
`reserve_sys_logic.json` (2024) and `data_proc_logic.json` (2025).

Blocks beyond `net` and `agents`:

- `stop`: ends the run once `transition` has fired `count` times (the run is then complete; agents with `cycles: -1`
  loop until then). Without it, the run ends when the agents finish their cycles or when `runtime.joinTimeoutMs`
  expires.
- `agents[].cycles`: positive for a finite number of cycles, negative for infinite (zero is rejected).
- `agents[].count`: number of threads for that segment.
- `runtime.joinTimeoutMs`: safety net only; set it well above the expected duration of a full run.

For `RANDOM`, `groups` is ignored. For proportional runs, each group declares the transitions that compete with one
another and the weight assigned to each transition.

### Policy modes

- `RANDOM`: chooses one enabled competing transition uniformly at random.
- `PROPORTION`: chooses among competing transitions using configured group weights to bias long-run selection
  frequencies.

Example for `data_proc` (the three processing modes compete for the buffer P3; T5 is the simple mode). The weights bias
the long-run share of each transition:

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

Example for `reserve_sys` (two independent conflicts: upper/lower agent and confirm/cancel):

```json
{
  "policy": {
    "type": "PROPORTION",
    "groups": [
      { "transitions": [2, 3], "weights": [1, 1] },
      { "transitions": [6, 7], "weights": [1, 1] }
    ]
  }
}
```

Note: the policy results should be checked against the real fire counts of each run
(see [Known Limitations](#known-limitations)).
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
- Other transitions use `alpha = 0` (no opening delay), and all keep large `beta` windows.

## Logging and Validation

### Built-in Java validation

After each run, if `runtime.builtInValidation` is `true`, the simulator validates log structure using
`PetriLogValidator`.

When `runtime.checkPlaceInvariants` is `true` (default), the monitor validates all configured place invariants
immediately after each successful transition fire. A violation throws an `IllegalStateException` in the thread that
fired (the run is not yet aborted as a whole; see [Known Limitations](#known-limitations)).

When `transitionInvariants` is configured, built-in validation also checks transition-invariant composition over the
final fire-event stream and reports unmatched residues if composition fails.

### Batch statistics mode (Phase 7)

`ConsoleRunner` accepts:

- `--network <id>`: optional, overrides `activeNetworkId` from `config.json`.
- `--batch <N>`: optional, runs the same network `N` times (`N >= 1`).

Outputs generated in batch mode:

- Per-run event logs: `runs/<network>_<policy>_runNNN.csv`
- Per-run summary (key/value CSV): `runs/<network>_<policy>_runNNN_summary.csv`
- Batch table (one row per run): `runs/<network>_<policy>_runs.csv`
- Batch aggregate (mean/stddev/min/max): `runs/<network>_<policy>_batch_summary.csv`

Per-run summaries include:

- total fires and duration,
- transition fire counts (`T0`, `T1`, ...),
- transition-invariant counts and percentages,
- composition validity and unmatched remainder (if any).

Note: in batch mode, built-in validation checks log structure only. Full transition-invariant composition validation
remains enabled for single-run mode.

### External Python semantic validation

`verifier.py` validates:

- CSV format and syntax
- Event chain consistency (`after` == next `before`)
- Transition semantics (`before + incidence = after`)
- Place invariants loaded from JSON

Default files:

- Config: `config.json` (**no longer valid as a default**: it is now the network index and has no `net` block, so the
  verifier fails with `'net' object is required`; always pass a network logic JSON)
- Log: `petri_log.csv`

Run against a packaged network:

```bash
python3 verifier.py --config src/main/resources/data_proc_logic.json --log petri_log.csv
```

Run against a batch log:

```bash
python3 verifier.py --config src/main/resources/reserve_sys_logic.json --log runs/reserve_sys_random_run001.csv
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

- Transition invariants: `PetriLogValidator.endAnchoredRegexFor(sequence)` builds one pattern per invariant from its
  `sequence` in the JSON (for example `[0, 1, 5, 6, 11]`), anchored to the end of the examined prefix and with greedy
  captures for interleaved tokens:

```regex
(?s)^(.*)\bT0\b(.*)\bT1\b(.*)\bT5\b(.*)\bT6\b(.*)\bT11\b\s*$
```

The validator is reduced by completion order: it takes the earliest token that could close an invariant, matches only
invariants that end in that transition against the current prefix, then replaces the matched prefix with captured
interleaving plus the untouched suffix. The log is a valid composition when no residue remains.

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

### Validation artifacts

The repository includes PIPE exports in `docs/` that can be used to validate the structural definition of the packaged
networks against their JSON counterparts in `src/main/resources/`.

Current validation checks cover:

- `pre` and `post` matrices
- the initial marking
- the place and transition counts implied by the export
- the optional global stop configuration used by the packaged networks
- open-ended agent execution via negative cycle counts

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

- **Policy fidelity.** `ProportionPolicy.decide` updates its counters on every call, and the monitor can call it more
  than once per fire, and the thread that just fired is not a candidate in the next decision. Observed shares may not
  match the configured weights, and a thread count larger than one for a segment biases the result.
- **Runs may end by timeout.** `runtime.joinTimeoutMs` is only a safety net; with the packaged `data_proc` timings
  (`alpha = 500`) a 200-invariant run takes far longer than 10 s, and the runner does not yet report whether the `stop`
  condition was reached. Tune the timings and the timeout before taking measurements.
- **Transition-invariant per-type percentages on `reserve_sys`.** Composition validation is now stable for shared
  prefixes, but per-invariant counts in the 2024 network remain ambiguous because all invariants share transitions. For
  that network, use transition fire counts (`T2/T3` and `T6/T7`) for per-type metrics.
- A place-invariant violation throws in the firing thread but does not abort the whole run.
- The policy groups are declared manually in the network JSON files; the monitor does not infer them from the matrices.
- `verifier.py` needs an explicit `--config <network logic json>` (see above).
- `petri_log.csv` and `runs/` are not in `.gitignore`.
- Timing behavior depends on scheduling and monitor contention in concurrent runs.

## Documentation

- `docs/reference/ThreadCalc.java`: reference implementation of the thread-count algorithms (run with `java`, not part
  of the Gradle build).
- `docs/Enunciado TP Final Concurrente 2024.pdf` / `2025.pdf`: the assignments.
- `docs/Algoritmos_cantidad_responsabilidad_hilos_..._FINAL.pdf`: thread-count algorithms used for the thread analysis.
- `docs/net_2024.xml`, `docs/net_2025.xml`: PIPE exports of the two networks.