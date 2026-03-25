package ar.edu.unc.david.petrinetsimulator.log;

import java.util.regex.Pattern;

/** Centralized regex patterns for Petri CSV logs. */
public final class PetriLogPatterns {
  private PetriLogPatterns() {}

  public static final Pattern HEADER =
      Pattern.compile("^Timestamp,Thread,Transition,MarkingBefore,MarkingAfter$");

  public static final Pattern FIRE_EVENT =
      Pattern.compile(
          "^\\d{13},[A-Za-z0-9._-]+,\\d+,"
              + "\"\\[-?\\d+(?:;\\s-?\\d+)*\\]\","
              + "\"\\[-?\\d+(?:;\\s-?\\d+)*\\]\"$");

  public static final Pattern END_LINE = Pattern.compile("^--- End of simulation: .+ ---$");
}
