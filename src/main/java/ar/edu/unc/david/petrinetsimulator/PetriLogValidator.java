package ar.edu.unc.david.petrinetsimulator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * PetriLogValidator provides methods to validate the structure and content of Petri net simulation
 * logs.
 */
public final class PetriLogValidator {
  private PetriLogValidator() {}

  /** Validates a log file by reading its lines and checking for structural and content errors. */
  public static List<String> validateFile(Path path) {
    try {
      return validateLines(Files.readAllLines(path));
    } catch (IOException e) {
      return List.of("I/O error reading log: " + e.getMessage());
    }
  }

  static List<String> validateLines(List<String> lines) {
    List<String> errors = new ArrayList<>();

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

    return errors;
  }
}
