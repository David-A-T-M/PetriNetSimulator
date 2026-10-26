package ar.edu.unc.david.petrinetsimulator.log;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.unc.david.petrinetsimulator.config.logic.TransitionInvariantConfig;
import java.time.Duration;
import java.util.ArrayList;
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
  void regexFor_createsOrderedPatternWithInterleavings() {
    var pattern = PetriLogValidator.regexFor(new int[] {0, 1, 5, 6, 11});
    String stream = "T0 T0 T1 T5 T6 T11 T11";

    assertTrue(pattern.matcher(stream).find());
  }

  @Test
  @DisplayName("accepts shared-prefix sequence by reducing in completion order")
  void analyzeComposition_acceptsSharedPrefixByCompletionOrder() {
    List<Integer> fired = List.of(0, 1, 5, 6, 11, 0, 1, 2, 3, 4, 11);

    PetriLogValidator.CompositionAnalysis analysis =
        PetriLogValidator.analyzeComposition(fired, dataProcInvariants());

    assertTrue(analysis.isValid());
    assertEquals(1, analysis.invariantCounts().get("simple"));
    assertEquals(1, analysis.invariantCounts().get("medium"));
    assertEquals(0, analysis.invariantCounts().get("high"));
  }

  @Test
  @DisplayName("accepts interleaved shared-prefix instances")
  void analyzeComposition_acceptsInterleavedSharedPrefixInstances() {
    List<Integer> fired = List.of(0, 1, 0, 1, 5, 2, 6, 3, 4, 11, 11);

    PetriLogValidator.CompositionAnalysis analysis =
        PetriLogValidator.analyzeComposition(fired, dataProcInvariants());

    assertTrue(analysis.isValid());
    assertEquals(1, analysis.invariantCounts().get("simple"));
    assertEquals(1, analysis.invariantCounts().get("medium"));
    assertEquals(0, analysis.invariantCounts().get("high"));
  }

  @Test
  @DisplayName("rejects incomplete medium invariant and keeps expected residue")
  void analyzeComposition_rejectsIncompleteMediumInvariant() {
    List<Integer> fired = List.of(0, 1, 2, 3, 11);

    PetriLogValidator.CompositionAnalysis analysis =
        PetriLogValidator.analyzeComposition(fired, dataProcInvariants());

    assertFalse(analysis.isValid());
    assertEquals("T0 T1 T2 T3", analysis.remainingSequence());
  }

  @Test
  @DisplayName("rejects dangling shared prefix residue")
  void analyzeComposition_rejectsDanglingPrefix() {
    List<Integer> fired = List.of(0, 1, 5, 6, 11, 0, 1);

    PetriLogValidator.CompositionAnalysis analysis =
        PetriLogValidator.analyzeComposition(fired, dataProcInvariants());

    assertFalse(analysis.isValid());
    assertEquals("T0 T1", analysis.remainingSequence());
  }

  @Test
  @DisplayName("accepts interleaved reserve_sys invariants")
  void analyzeComposition_acceptsReserveSysInterleaving() {
    List<Integer> fired = List.of(0, 1, 0, 1, 2, 3, 5, 4, 6, 7, 9, 8, 10, 11, 11);

    PetriLogValidator.CompositionAnalysis analysis =
        PetriLogValidator.analyzeComposition(fired, reserveSysInvariants());

    assertTrue(analysis.isValid());
    assertEquals(1, analysis.invariantCounts().get("upper_confirm"));
    assertEquals(1, analysis.invariantCounts().get("lower_cancel"));
  }

  @Test
  @DisplayName("reduces 200 interleaved invariants in under one second")
  void analyzeComposition_reducesLargeInterleavingUnderOneSecond() {
    List<int[]> templates =
        List.of(
            new int[] {0, 1, 5, 6, 11},
            new int[] {0, 1, 2, 3, 4, 11},
            new int[] {0, 1, 7, 8, 9, 10, 11});
    List<int[]> instances = new ArrayList<>();
    for (int i = 0; i < 200; i++) {
      instances.add(templates.get(i % templates.size()));
    }

    List<Integer> fired = new ArrayList<>();
    int maxLen = instances.stream().mapToInt(seq -> seq.length).max().orElse(0);
    for (int step = 0; step < maxLen; step++) {
      for (int[] sequence : instances) {
        if (step < sequence.length) {
          fired.add(sequence[step]);
        }
      }
    }

    PetriLogValidator.CompositionAnalysis analysis =
        assertTimeoutPreemptively(
            Duration.ofSeconds(1),
            () -> PetriLogValidator.analyzeComposition(fired, dataProcInvariants()));

    assertNotNull(analysis);
    assertTrue(analysis.isValid());
    int totalReduced =
        analysis.invariantCounts().values().stream().mapToInt(Integer::intValue).sum();
    assertEquals(200, totalReduced);
  }

  @Test
  @DisplayName("endAnchoredRegexFor anchors matching at the prefix end")
  void endAnchoredRegexFor_matchesOnlyWhenSequenceEndsPrefix() {
    var pattern = PetriLogValidator.endAnchoredRegexFor(new int[] {0, 1, 5, 6, 11});

    assertTrue(pattern.matcher("T0 T1 T5 T6 T11").matches());
    assertFalse(pattern.matcher("T0 T1 T5 T6 T11 T2").matches());
  }

  private static List<TransitionInvariantConfig> dataProcInvariants() {
    return List.of(
        new TransitionInvariantConfig("simple", new int[] {0, 1, 5, 6, 11}),
        new TransitionInvariantConfig("medium", new int[] {0, 1, 2, 3, 4, 11}),
        new TransitionInvariantConfig("high", new int[] {0, 1, 7, 8, 9, 10, 11}));
  }

  private static List<TransitionInvariantConfig> reserveSysInvariants() {
    return List.of(
        new TransitionInvariantConfig("upper_confirm", new int[] {0, 1, 2, 5, 6, 9, 10, 11}),
        new TransitionInvariantConfig("upper_cancel", new int[] {0, 1, 2, 5, 7, 8, 11}),
        new TransitionInvariantConfig("lower_confirm", new int[] {0, 1, 3, 4, 6, 9, 10, 11}),
        new TransitionInvariantConfig("lower_cancel", new int[] {0, 1, 3, 4, 7, 8, 11}));
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
