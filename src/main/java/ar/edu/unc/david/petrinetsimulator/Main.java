package ar.edu.unc.david.petrinetsimulator;

import ar.edu.unc.david.petrinetsimulator.runner.ConsoleRunner;
import ar.edu.unc.david.petrinetsimulator.ui.javafx.PetriSimApp;
import java.util.Arrays;
import javafx.application.Application;

/**
 * Main entry point: decides whether to launch the JavaFX application or run in headless mode based
 * on the presence of the "--ui" flag.
 */
public class Main {
  /**
   * If "--ui" is present in the command-line arguments, launches the JavaFX application; otherwise,
   * runs the console-based simulation.
   *
   * @param args Command-line arguments, where "--ui" indicates that the UI should be launched.
   */
  public static void main(String[] args) {
    boolean useUi = Arrays.asList(args).contains("--ui");

    if (useUi) {
      String[] filtered = Arrays.stream(args).filter(a -> !a.equals("--ui")).toArray(String[]::new);
      Application.launch(PetriSimApp.class, filtered);
    } else {
      ConsoleRunner.main(args);
    }
  }
}
