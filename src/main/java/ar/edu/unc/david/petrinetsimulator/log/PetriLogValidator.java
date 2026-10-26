package ar.edu.unc.david.petrinetsimulator.log;

import ar.edu.unc.david.petrinetsimulator.config.logic.TransitionInvariantConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * PetriLogValidator provides methods to validate the structure and content of Petri net simulation
 * logs.
 */
public final class PetriLogValidator {
  private PetriLogValidator() {}

  private record CompiledInvariant(String name, int[] sequence, int lastTransition) {}

  /** Result of reducing a fire stream by configured transition invariants. */
  public record CompositionAnalysis(
      Map<String, Integer> invariantCounts, String remainingSequence) {
    public boolean isValid() {
      return remainingSequence.isBlank();
    }
  }

  /** Validates a log file by reading its lines and checking for structural and content errors. */
  public static List<String> validateFile(Path path) {
    return validateFile(path, List.of());
  }

  /**
   * Validates a log file by reading its lines, checking structure, and validating transition
   * invariant composition when configured.
   */
  public static List<String> validateFile(
      Path path, List<TransitionInvariantConfig> transitionInvariants) {
    try {
      return validateLines(Files.readAllLines(path), transitionInvariants);
    } catch (IOException e) {
      return List.of("I/O error reading log: " + e.getMessage());
    }
  }

  static List<String> validateLines(List<String> lines) {
    return validateLines(lines, List.of());
  }

  static List<String> validateLines(
      List<String> lines, List<TransitionInvariantConfig> transitionInvariants) {
    List<String> errors = new ArrayList<>();
    List<Integer> firedTransitions = new ArrayList<>();

    if (lines == null || lines.isEmpty()) {
      errors.add("Line 1: missing header (file is empty).");
      return errors;
    }

    if (!PetriLogPatterns.HEADER.matcher(lines.getFirst()).matches()) {
      errors.add("Line 1: invalid header.");
    }

    int fireCount = 0;
    int endLineCount = 0;
    int lastIndex = lines.size() - 1;

    for (int i = 1; i < lines.size(); i++) {
      String line = lines.get(i);

      if (line == null || line.isBlank()) {
        errors.add("Line " + (i + 1) + ": blank lines are not allowed.");
        continue;
      }

      boolean isFire = PetriLogPatterns.FIRE_EVENT.matcher(line).matches();
      boolean isEnd = PetriLogPatterns.END_LINE.matcher(line).matches();

      if (isFire) {
        fireCount++;
        firedTransitions.add(parseTransitionId(line));
        continue;
      }

      if (isEnd) {
        endLineCount++;
        if (i != lastIndex) {
          errors.add("Line " + (i + 1) + ": end line must be the last line.");
        }
        continue;
      }

      errors.add("Line " + (i + 1) + ": invalid entry.");
    }

    if (fireCount == 0) {
      errors.add("No fire events found.");
    }

    if (endLineCount > 1) {
      errors.add("More than one end line found.");
    }

    if (errors.isEmpty()
        && transitionInvariants != null
        && !transitionInvariants.isEmpty()
        && !isComposedByTransitionInvariants(firedTransitions, transitionInvariants)) {
      String remaining = reduceByTransitionInvariants(firedTransitions, transitionInvariants);
      errors.add(
          "Transition-invariant composition failed. Remaining unmatched sequence: " + remaining);
    }

    return errors;
  }

  /** Extracts transition IDs from valid fire lines in the provided log lines. */
  public static List<Integer> fireTransitionsFromLines(List<String> lines) {
    List<Integer> transitions = new ArrayList<>();
    if (lines == null || lines.size() < 2) {
      return transitions;
    }

    for (int i = 1; i < lines.size(); i++) {
      String line = lines.get(i);
      if (line != null && PetriLogPatterns.FIRE_EVENT.matcher(line).matches()) {
        transitions.add(parseTransitionId(line));
      }
    }

    return transitions;
  }

  /** Analyzes transition-invariant composition and counts reduced occurrences per invariant. */
  public static CompositionAnalysis analyzeComposition(
      List<Integer> firedTransitions, List<TransitionInvariantConfig> transitionInvariants) {
    if (transitionInvariants == null || transitionInvariants.isEmpty()) {
      return new CompositionAnalysis(Map.of(), asTransitionStream(firedTransitions));
    }

    List<Integer> remainingTokens = new ArrayList<>(firedTransitions);
    Set<Integer> lastTransitions = new HashSet<>();
    List<CompiledInvariant> invariants = new ArrayList<>(transitionInvariants.size());

    Map<String, Integer> counts = new LinkedHashMap<>();
    for (TransitionInvariantConfig invariant : transitionInvariants) {
      if (invariant.sequence() == null || invariant.sequence().length == 0) {
        throw new IllegalArgumentException(
            "Transition-invariant sequence cannot be null or empty.");
      }

      int[] sequence = invariant.sequence();
      int lastTransition = sequence[sequence.length - 1];
      invariants.add(
          new CompiledInvariant(
              invariant.name(), Arrays.copyOf(sequence, sequence.length), lastTransition));
      counts.put(invariant.name(), 0);
      lastTransitions.add(lastTransition);
    }

    while (true) {
      int completionIndex = firstCompletionIndex(remainingTokens, lastTransitions);
      if (completionIndex < 0) {
        break;
      }

      int completedTransition = remainingTokens.get(completionIndex);
      boolean reduced = false;
      for (CompiledInvariant invariant : invariants) {
        if (invariant.lastTransition() != completedTransition) {
          continue;
        }

        List<Integer> reducedPrefix =
            reducePrefixWithGreedyEndAnchoredMatch(
                remainingTokens, completionIndex, invariant.sequence());
        if (reducedPrefix == null) {
          continue;
        }

        remainingTokens = reducedPrefix;
        counts.put(invariant.name(), counts.get(invariant.name()) + 1);
        reduced = true;
        break;
      }

      if (!reduced) {
        // The earliest potential completion token cannot close any configured invariant.
        // Keep the unmatched prefix for diagnostics and drop that orphan completion.
        remainingTokens.remove(completionIndex);
        break;
      }
    }

    return new CompositionAnalysis(Map.copyOf(counts), asTransitionStream(remainingTokens));
  }

  static Pattern regexFor(int[] sequence) {
    if (sequence == null || sequence.length == 0) {
      throw new IllegalArgumentException("Transition sequence cannot be null or empty.");
    }

    StringBuilder regex = new StringBuilder("(?s)\\bT").append(sequence[0]).append("\\b\\s*");
    for (int i = 1; i < sequence.length; i++) {
      regex.append("(.*?)\\bT").append(sequence[i]).append("\\b\\s*");
    }
    return Pattern.compile(regex.toString());
  }

  static Pattern endAnchoredRegexFor(int[] sequence) {
    if (sequence == null || sequence.length == 0) {
      throw new IllegalArgumentException("Transition sequence cannot be null or empty.");
    }

    StringBuilder regex = new StringBuilder("(?s)^(.*)\\bT").append(sequence[0]).append("\\b");
    for (int i = 1; i < sequence.length; i++) {
      regex.append("(.*)\\bT").append(sequence[i]).append("\\b");
    }
    regex.append("\\s*$");
    return Pattern.compile(regex.toString());
  }

  static boolean isComposedByTransitionInvariants(
      List<Integer> firedTransitions, List<TransitionInvariantConfig> transitionInvariants) {
    return analyzeComposition(firedTransitions, transitionInvariants).isValid();
  }

  static String reduceByTransitionInvariants(
      List<Integer> firedTransitions, List<TransitionInvariantConfig> transitionInvariants) {
    return analyzeComposition(firedTransitions, transitionInvariants).remainingSequence();
  }

  private static int firstCompletionIndex(List<Integer> tokens, Set<Integer> lastTransitions) {
    for (int i = 0; i < tokens.size(); i++) {
      if (lastTransitions.contains(tokens.get(i))) {
        return i;
      }
    }
    return -1;
  }

  private static List<Integer> reducePrefixWithGreedyEndAnchoredMatch(
      List<Integer> tokens, int completionIndex, int[] sequence) {
    int seqLen = sequence.length;
    int[] positions = new int[seqLen];

    int searchEnd = completionIndex;
    for (int i = seqLen - 1; i >= 0; i--) {
      int found = findLastIndexOf(tokens, sequence[i], searchEnd);
      if (found < 0) {
        return null;
      }
      positions[i] = found;
      searchEnd = found - 1;
    }

    if (positions[seqLen - 1] != completionIndex) {
      return null;
    }

    List<Integer> reduced = new ArrayList<>();

    int groupStart = 0;
    for (int i = 0; i < seqLen; i++) {
      int groupEnd = positions[i];
      if (groupStart < groupEnd) {
        reduced.addAll(tokens.subList(groupStart, groupEnd));
      }
      groupStart = positions[i] + 1;
    }

    if (completionIndex + 1 < tokens.size()) {
      reduced.addAll(tokens.subList(completionIndex + 1, tokens.size()));
    }

    return reduced;
  }

  private static int findLastIndexOf(List<Integer> tokens, int value, int endInclusive) {
    for (int i = endInclusive; i >= 0; i--) {
      if (tokens.get(i) == value) {
        return i;
      }
    }
    return -1;
  }

  private static String asTransitionStream(List<Integer> firedTransitions) {
    return asTransitionStream(firedTransitions, firedTransitions.size());
  }

  private static String asTransitionStream(List<Integer> firedTransitions, int limitExclusive) {
    int limit = Math.min(limitExclusive, firedTransitions.size());
    if (limit <= 0) {
      return "";
    }

    StringBuilder stream = new StringBuilder(limit * 4);
    for (int i = 0; i < limit; i++) {
      if (i > 0) {
        stream.append(' ');
      }
      stream.append('T').append(firedTransitions.get(i));
    }

    return stream.toString();
  }

  private static int parseTransitionId(String fireLine) {
    String[] parts = fireLine.split(",", 4);
    if (parts.length < 3) {
      throw new IllegalArgumentException("Malformed fire line: " + fireLine);
    }
    return Integer.parseInt(parts[2]);
  }
}
