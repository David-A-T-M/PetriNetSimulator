package ar.edu.unc.david.petrinetsimulator.ui.javafx;

import ar.edu.unc.david.petrinetsimulator.config.ConfigLoader;
import ar.edu.unc.david.petrinetsimulator.config.SimulationConfig;
import ar.edu.unc.david.petrinetsimulator.config.layout.LayoutConfig;
import ar.edu.unc.david.petrinetsimulator.config.layout.PlaceLayout;
import ar.edu.unc.david.petrinetsimulator.config.layout.TransitionLayout;
import ar.edu.unc.david.petrinetsimulator.core.PetriEvent;
import ar.edu.unc.david.petrinetsimulator.core.SimulatorEngine;
import ar.edu.unc.david.petrinetsimulator.core.SimulatorEngine.Components;
import ar.edu.unc.david.petrinetsimulator.log.UiAwarePetriLogger;
import ar.edu.unc.david.petrinetsimulator.ui.PetriCanvas;
import ar.edu.unc.david.petrinetsimulator.ui.QueuedNotifier;
import ar.edu.unc.david.petrinetsimulator.ui.SimulationRelay;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.stage.Stage;

/** JavaFX entry point: wires the simulation engine to the visual canvas. */
public class PetriSimApp extends Application {

  private static final String DEFAULT_CONFIG_FILE = "config.json";

  private static final double PLACE_START_X = 60;
  private static final double PLACE_SPACING = 90;
  private static final double TRANSITION_START_X = 80;
  private static final double TRANSITION_SPACING = 80;
  private static final double CANVAS_HEIGHT = 360;
  private static final double MAX_WINDOW_WIDTH = 1100;

  private List<Thread> agentThreads;
  private UiAwarePetriLogger logger;
  private SimulationRelay relay;

  @Override
  public void start(Stage stage) {
    List<String> rawArgs = getParameters().getRaw();
    String configPath = rawArgs.isEmpty() ? DEFAULT_CONFIG_FILE : rawArgs.getFirst();
    LayoutConfig layout = ConfigLoader.load("src/main/resources/layout.json", LayoutConfig.class);

    SimulationConfig config = ConfigLoader.load(configPath, SimulationConfig.class);

    BlockingQueue<PetriEvent> queue = new LinkedBlockingQueue<>();
    logger =
        new UiAwarePetriLogger(
            SimulatorEngine.resolveLogFile(config.logging()), new QueuedNotifier(queue));

    PetriCanvas canvas = buildCanvas(layout);
    canvas.setTopology(config.net().pre(), config.net().post()); // <- agregar
    canvas.updateUi(new PetriEvent(-1, null, config.net().initialMarking().clone(), 0));

    relay = new SimulationRelay(queue, canvas);
    relay.start();

    Components c = SimulatorEngine.build(config, logger);
    agentThreads = SimulatorEngine.startAgents(c.agents(), c.monitor());

    double canvasWidth =
        Math.max(
            PLACE_START_X + config.net().initialMarking().length * PLACE_SPACING + PLACE_SPACING,
            TRANSITION_START_X
                + c.net().matrix().numTransitions() * TRANSITION_SPACING
                + TRANSITION_SPACING);

    canvas.setPrefSize(canvasWidth, CANVAS_HEIGHT);
    ScrollPane scroll = new ScrollPane(canvas);
    scroll.setFitToHeight(true);

    stage.setScene(new Scene(scroll, Math.min(canvasWidth + 20, MAX_WINDOW_WIDTH), CANVAS_HEIGHT));
    stage.setTitle("Petri Net Simulator");
    stage.show();
  }

  @Override
  public void stop() {
    if (relay != null) {
      relay.cancel();
    }
    if (agentThreads != null) {
      agentThreads.forEach(Thread::interrupt);
    }
    if (logger != null) {
      logger.close();
    }
  }

  private PetriCanvas buildCanvas(LayoutConfig layout) {
    PetriCanvas canvas = new PetriCanvas();

    for (PlaceLayout p : layout.places()) {
      canvas.addPlace(p.id(), p.label(), p.x(), p.y());
    }

    for (TransitionLayout t : layout.transitions()) {
      canvas.addTransition(t.id(), t.label(), t.x(), t.y());
    }

    canvas.addArcs(layout.arcs());

    return canvas;
  }
}
