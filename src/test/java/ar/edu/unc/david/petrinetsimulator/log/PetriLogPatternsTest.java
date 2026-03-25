package ar.edu.unc.david.petrinetsimulator.log;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for PetriLogPatterns to ensure regex patterns match expected log formats. */
public class PetriLogPatternsTest {

  @Test
  @DisplayName("HEADER matches exact CSV header")
  void header_matchesExpectedHeader() {
    assertTrue(
        PetriLogPatterns.HEADER
            .matcher("Timestamp,Thread,Transition,MarkingBefore,MarkingAfter")
            .matches());
  }

  @Test
  @DisplayName("HEADER rejects altered header")
  void header_rejectsAlteredHeader() {
    assertFalse(
        PetriLogPatterns.HEADER.matcher("Timestamp,Thread,Transition,Before,After").matches());
  }

  @Test
  @DisplayName("FIRE_EVENT matches a valid log line")
  void fireEvent_matchesValidLine() {
    String line =
        "1774364518453,Prod-0,0,\"[2; 0; 0; 2; 0; 0; 3; 1; 0]\",\"[1; 1; 0; 2; 0; 0; 2; 0; 0]\"";
    assertTrue(PetriLogPatterns.FIRE_EVENT.matcher(line).matches());
  }

  @Test
  @DisplayName("FIRE_EVENT supports negative tokens in vectors")
  void fireEvent_supportsNegativeValues() {
    String line = "1774364518453,Prod-0,0,\"[-1; 0; 2]\",\"[0; -2; 3]\"";
    assertTrue(PetriLogPatterns.FIRE_EVENT.matcher(line).matches());
  }

  @Test
  @DisplayName("FIRE_EVENT rejects non-13-digit timestamp")
  void fireEvent_rejectsBadTimestamp() {
    String line = "12345,Prod-0,0,\"[1; 0]\",\"[0; 1]\"";
    assertFalse(PetriLogPatterns.FIRE_EVENT.matcher(line).matches());
  }

  @Test
  @DisplayName("FIRE_EVENT rejects missing quotes around vectors")
  void fireEvent_rejectsMissingQuotes() {
    String line = "1774364518453,Prod-0,0,[1; 0],[0; 1]";
    assertFalse(PetriLogPatterns.FIRE_EVENT.matcher(line).matches());
  }

  @Test
  @DisplayName("END_LINE matches expected trailer format")
  void endLine_matchesTrailer() {
    assertTrue(
        PetriLogPatterns.END_LINE
            .matcher("--- End of simulation: 2026-03-24T10:00:00 ---")
            .matches());
  }

  @Test
  @DisplayName("END_LINE rejects random text")
  void endLine_rejectsRandomText() {
    assertFalse(PetriLogPatterns.END_LINE.matcher("simulation ended").matches());
  }
}
