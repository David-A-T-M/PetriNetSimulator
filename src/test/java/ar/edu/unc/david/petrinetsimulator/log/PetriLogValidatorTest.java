package ar.edu.unc.david.petrinetsimulator.log;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.unc.david.petrinetsimulator.config.logic.TransitionInvariantConfig;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for PetriLogValidator to ensure correct validation of log lines. */
public class PetriLogValidatorTest {

  @Test
  @DisplayName("Accepts header + fire events + single end line at EOF")
  void validateLines_acceptsStrictValidLog() {
    List<String> lines =
        List.of(
            "Timestamp,Thread,Transition,MarkingBefore,MarkingAfter",
            "1774364518453,Prod-0,0,\"[2; 0; 0]\",\"[1; 1; 0]\"",
            "1774364518460,Cons-0,3,\"[1; 0; 1]\",\"[1; 1; 0]\"",
            "--- End of simulation: 2026-03-24T10:00:00 ---");

    List<String> errors = PetriLogValidator.validateLines(lines);

    assertTrue(errors.isEmpty());
  }

  @Test
  @DisplayName("Rejects log without fire events")
  void validateLines_rejectsNoFireEvents() {
    List<String> lines =
        List.of(
            "Timestamp,Thread,Transition,MarkingBefore,MarkingAfter",
            "--- End of simulation: 2026-03-24T10:00:00 ---");

    List<String> errors = PetriLogValidator.validateLines(lines);

    assertTrue(errors.stream().anyMatch(e -> e.contains("No fire events found")));
  }

  @Test
  @DisplayName("Rejects end line if not last")
  void validateLines_rejectsEndLineNotLast() {
    List<String> lines =
        List.of(
            "Timestamp,Thread,Transition,MarkingBefore,MarkingAfter",
            "--- End of simulation: 2026-03-24T10:00:00 ---",
            "1774364518453,Prod-0,0,\"[2; 0]\",\"[1; 1]\"");

    List<String> errors = PetriLogValidator.validateLines(lines);

    assertTrue(errors.stream().anyMatch(e -> e.contains("must be the last line")));
  }

  @Test
  @DisplayName("Rejects multiple end lines")
  void validateLines_rejectsMultipleEndLines() {
    List<String> lines =
        List.of(
            "Timestamp,Thread,Transition,MarkingBefore,MarkingAfter",
            "1774364518453,Prod-0,0,\"[2; 0]\",\"[1; 1]\"",
            "--- End of simulation: 2026-03-24T10:00:00 ---",
            "--- End of simulation: 2026-03-24T10:00:01 ---");

    List<String> errors = PetriLogValidator.validateLines(lines);

    assertTrue(errors.stream().anyMatch(e -> e.contains("More than one end line found")));
  }

  @Test
  @DisplayName("Rejects blank lines")
  void validateLines_rejectsBlankLines() {
    List<String> lines =
        List.of(
            "Timestamp,Thread,Transition,MarkingBefore,MarkingAfter",
            "",
            "1774364518453,Prod-0,0,\"[2; 0]\",\"[1; 1]\"");

    List<String> errors = PetriLogValidator.validateLines(lines);

    assertTrue(errors.stream().anyMatch(e -> e.contains("blank lines are not allowed")));
  }

  @Test
  @DisplayName("Rejects malformed fire line")
  void validateLines_rejectsMalformedFire() {
    List<String> lines =
        List.of(
            "Timestamp,Thread,Transition,MarkingBefore,MarkingAfter",
            "1774364518453,Prod-0,0,[2; 0],[1; 1]");

    List<String> errors = PetriLogValidator.validateLines(lines);

    assertFalse(errors.isEmpty());
    assertTrue(errors.stream().anyMatch(e -> e.contains("Line 2: invalid entry")));
  }

  @Test
  @DisplayName("accepts interleaved sequence when it is a composition of transition invariants")
  void validateLines_acceptsValidTransitionInvariantComposition() {
    List<String> lines = linesFromTransitions(0, 0, 1, 1, 5, 2, 6, 3, 4, 11, 11);

    List<TransitionInvariantConfig> invariants =
        List.of(
            new TransitionInvariantConfig("simple", new int[] {0, 1, 5, 6, 11}),
            new TransitionInvariantConfig("medium", new int[] {0, 1, 2, 3, 4, 11}));
    List<String> errors = PetriLogValidator.validateLines(lines, invariants);

    assertTrue(errors.isEmpty());
  }

  @Test
  @DisplayName("rejects sequence that cannot be reduced by configured transition invariants")
  void validateLines_rejectsInvalidTransitionInvariantComposition() {
    List<String> lines = linesFromTransitions(0, 1, 5, 11);

    List<TransitionInvariantConfig> invariants =
        List.of(new TransitionInvariantConfig("simple", new int[] {0, 1, 5, 6, 11}));
    List<String> errors = PetriLogValidator.validateLines(lines, invariants);

    assertTrue(
        errors.stream().anyMatch(e -> e.contains("Transition-invariant composition failed")));
    assertTrue(
        errors.stream().anyMatch(e -> e.contains("T0") || e.contains("T1") || e.contains("T5")));
  }

  @Test
  @DisplayName("regexFor creates ordered pattern with interleaving captures")
  void regexFor_createsOrderedPatternWithInterleaving() {
    Pattern pattern = PetriLogValidator.regexFor(new int[] {0, 1, 5, 6, 11});
    String stream = "T0 T0 T1 T5 T6 T11 T11";

    assertTrue(pattern.matcher(stream).find());
  }

  private static List<String> linesFromTransitions(int... transitions) {
    java.util.ArrayList<String> lines = new java.util.ArrayList<>();
    lines.add("Timestamp,Thread,Transition,MarkingBefore,MarkingAfter");

    long ts = 1_774_364_518_000L;
    for (int transition : transitions) {
      lines.add(ts + ",T-0," + transition + ",\"[1; 0; 0]\",\"[1; 0; 0]\"");
      ts += 7;
    }

    lines.add("--- End of simulation: 2026-03-24T10:00:00 ---");
    return lines;
  }
}
