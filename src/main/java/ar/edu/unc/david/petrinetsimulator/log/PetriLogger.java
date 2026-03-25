package ar.edu.unc.david.petrinetsimulator.log;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * PetriLogger is responsible for logging the firing of transitions in a Petri net simulation. It
 * writes logs to a specified file in CSV format, including timestamps, thread names, transition
 * IDs, and markings before and after firing. It also provides methods for logging waiting and
 * waking events.
 */
public class PetriLogger {
  private PrintWriter writer;

  /**
   * Constructor that initializes the logger with a specified file name.
   *
   * @param fileName the name of the file where logs will be written.
   */
  public PetriLogger(String fileName) {
    try {
      this.writer = new PrintWriter(new FileWriter(fileName, false), true);

      writer.println("Timestamp,Thread,Transition,MarkingBefore,MarkingAfter");

      System.out.println("[INFO] Logger initialized. Logging to file: " + fileName);
    } catch (IOException e) {
      System.err.println("[ERROR] Failed to initialize logger: " + e.getMessage());
      this.writer = null;
    }
  }

  /** Logs the firing of a transition. */
  public synchronized void logFire(int transition, int[] before, int[] after) {
    if (writer == null) {
      return;
    }

    writer.printf(
        "%d,%s,%d,\"%s\",\"%s\"%n",
        System.currentTimeMillis(),
        Thread.currentThread().getName(),
        transition,
        Arrays.toString(before).replace(",", ";"),
        Arrays.toString(after).replace(",", ";"));
  }

  /** Logs when a thread is waiting for a transition to become enabled. */
  public synchronized void logWait(Thread current, int transition, int[] marking) {
    System.out.printf(
        "[WAIT] %s waits for T%d with marking %s%n",
        current.getName(), transition, Arrays.toString(marking));
  }

  /** Logs when a thread is woken up to fire a transition. */
  public synchronized void logWake(Thread toWake, int chosen, int[] marking) {
    System.out.printf(
        "[WAKE] Monitor wakes %s to fire T%d with marking %s%n",
        toWake.getName(), chosen, Arrays.toString(marking));
  }

  /** Closes the logger and releases any resources. */
  public void close() {
    if (writer != null) {
      writer.println("--- End of simulation: " + LocalDateTime.now() + " ---");
      writer.close();
    }
  }
}
