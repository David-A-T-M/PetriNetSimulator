package ar.edu.unc.david.petrinetsimulator.log;

import ar.edu.unc.david.petrinetsimulator.config.logic.TransitionInvariantConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PetriLogValidator provides methods to validate the structure and content of Petri net simulation
 * logs.
 */
public final class PetriLogValidator {
  private PetriLogValidator() {}

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

  static boolean isComposedByTransitionInvariants(
      List<Integer> firedTransitions, List<TransitionInvariantConfig> transitionInvariants) {
    return reduceByTransitionInvariants(firedTransitions, transitionInvariants).isBlank();
  }

  static String reduceByTransitionInvariants(
      List<Integer> firedTransitions, List<TransitionInvariantConfig> transitionInvariants) {
    String current = asTransitionStream(firedTransitions);
    if (current.isBlank()) {
      return current;
    }

    List<TransitionInvariantConfig> orderedInvariants =
        transitionInvariants.stream()
            .sorted(
                Comparator.comparingInt((TransitionInvariantConfig ti) -> ti.sequence().length)
                    .reversed())
            .toList();

    boolean changed;
    do {
      changed = false;
      for (TransitionInvariantConfig invariant : orderedInvariants) {
        Pattern pattern = regexFor(invariant.sequence());
        String reduced = reduceOne(current, pattern);
        if (!reduced.equals(current)) {
          current = reduced;
          changed = true;
        }
      }
    } while (changed);

    return current;
  }

  private static String reduceOne(String stream, Pattern pattern) {
    Matcher matcher = pattern.matcher(stream);
    if (!matcher.find()) {
      return stream;
    }

    StringBuilder interleavings = new StringBuilder();
    for (int i = 1; i <= matcher.groupCount(); i++) {
      interleavings.append(matcher.group(i)).append(' ');
    }

    String reduced =
        stream.substring(0, matcher.start()) + interleavings + stream.substring(matcher.end());

    return normalizeTransitionStream(reduced);
  }

  private static String asTransitionStream(List<Integer> firedTransitions) {
    return firedTransitions.stream().map(t -> "T" + t).reduce((a, b) -> a + " " + b).orElse("");
  }

  private static String normalizeTransitionStream(String stream) {
    return List.of(stream.trim().split("\\s+")).stream()
        .filter(token -> !token.isBlank())
        .reduce((a, b) -> a + " " + b)
        .orElse("");
  }

  private static int parseTransitionId(String fireLine) {
    String[] parts = fireLine.split(",", 4);
    if (parts.length < 3) {
      throw new IllegalArgumentException("Malformed fire line: " + fireLine);
    }
    return Integer.parseInt(parts[2]);
  }
}
