package ar.edu.unc.david.petrinetsimulator.ui;

import ar.edu.unc.david.petrinetsimulator.core.PetriEvent;
import ar.edu.unc.david.petrinetsimulator.core.PetriNotifier;
import java.util.concurrent.BlockingQueue;

/**
 * A PetriNotifier that enqueues events into a BlockingQueue for later processing by the
 * SimulationRelay.
 */
public class QueuedNotifier implements PetriNotifier {
  private final BlockingQueue<PetriEvent> eventQueue;

  public QueuedNotifier(BlockingQueue<PetriEvent> queue) {
    this.eventQueue = queue;
  }

  @Override
  public void notify(PetriEvent event) {
    eventQueue.offer(event);
  }
}
