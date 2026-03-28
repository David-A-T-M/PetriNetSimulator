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
- [Roadmap Ideas](#roadmap-ideas)

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
  - `src/main/java/ar/edu/unc/david/petrinetsimulator/config/LoggingConfig.java`
  - `src/main/java/ar/edu/unc/david/petrinetsimulator/config/RuntimeConfig.java`
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

Run simulation with a custom config file:

```bash
./gradlew run --args="path/to/your-config.json"
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
    { "namePrefix": "P", "count": 2, "sequence": [0, 1, 2], "cycles": 10 },
    { "namePrefix": "C", "count": 2, "sequence": [3, 4, 5], "cycles": 10 }
  ],
  "logging": {
    "file": "petri_log.csv",
    "logWaitWake": false
  },
  "runtime": {
    "joinTimeoutMs": 40000,
    "builtInValidation": true
  },
  "policy": {
    "type": "RANDOM"
  }
}
```

## Logging and Validation

### Built-in Java validation

At the end of `main`, if `runtime.builtInValidation` is `true`, the simulator validates log structure using `PetriLogValidator`.

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

## Known Limitations

- `policy.type` currently supports only `RANDOM`.
- Built-in Java validation checks log structure; deeper semantic validation is handled by `verifier.py`.
- Timing behavior depends on scheduling and monitor contention in concurrent runs.

