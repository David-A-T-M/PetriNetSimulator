package ar.edu.unc.david.petrinetsimulator.agent;

import ar.edu.unc.david.petrinetsimulator.monitor.MonitorInterface;
import java.util.Objects;

/**
 * Represents an agent that executes a sequence of transitions on a Petri net monitor continuously
 * in a loop.
 *
 * <p>The PetriAgent is designed to run as a separate thread and repeatedly attempts to fire
 * transitions specified by the provided sequence on a shared {@code Monitor}.
 */
public class PetriAgent implements Runnable {
  private final MonitorInterface monitor;
  private final int[] sequence;
  private final int cycles;

  public PetriAgent(MonitorInterface monitor, int[] sequence) {
    this(monitor, sequence, -1);
  }

  /** Creates a PetriAgent with the specified monitor, transition sequence, and number of cycles. */
  public PetriAgent(MonitorInterface monitor, int[] sequence, int cycles) {
    this.monitor = Objects.requireNonNull(monitor, "monitor cannot be null");
    Objects.requireNonNull(sequence, "sequence cannot be null");
    if (sequence.length == 0) {
      throw new IllegalArgumentException("sequence cannot be empty");
    }
    if (cycles == 0) {
      throw new IllegalArgumentException("cycles must be > 0 for finite mode, or < 0 for infinite");
    }
    this.sequence = sequence.clone();
    this.cycles = cycles;
  }

  @Override
  public void run() {
    int completedCycles = 0;

    while (!Thread.currentThread().isInterrupted() && (cycles < 0 || completedCycles < cycles)) {
      for (int t : sequence) {
        if (!monitor.fireTransition(t)) {
          return;
        }
        if (Thread.currentThread().isInterrupted()) {
          return;
        }
      }
      completedCycles++;
    }
  }
}
