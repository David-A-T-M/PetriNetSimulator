package ar.edu.unc.david.petrinetsimulator.ui;

import ar.edu.unc.david.petrinetsimulator.core.PetriEvent;
import java.util.concurrent.BlockingQueue;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

/** A JavaFX Service that continuously takes PetriEvents from a BlockingQueue and updates the UI. */
public class SimulationRelay extends Service<Void> {
  private final BlockingQueue<PetriEvent> queue;
  private final PetriCanvas canvas;

  public SimulationRelay(BlockingQueue<PetriEvent> queue, PetriCanvas canvas) {
    this.queue = queue;
    this.canvas = canvas;
  }

  @Override
  protected Task<Void> createTask() {
    return new Task<>() {
      @Override
      protected Void call() throws Exception {
        while (!isCancelled()) {
          PetriEvent event = queue.take();

          Platform.runLater(() -> canvas.updateUi(event));
        }
        return null;
      }
    };
  }
}
