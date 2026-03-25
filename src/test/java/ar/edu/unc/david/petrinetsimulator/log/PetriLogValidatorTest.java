package ar.edu.unc.david.petrinetsimulator.log;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
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
}
