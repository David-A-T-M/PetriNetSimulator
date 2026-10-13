package ar.edu.unc.david.petrinetsimulator.ui.javafx;

import ar.edu.unc.david.petrinetsimulator.config.ConfigLoader;
import ar.edu.unc.david.petrinetsimulator.config.layout.LayoutConfig;
import ar.edu.unc.david.petrinetsimulator.config.layout.PathConfig;
import ar.edu.unc.david.petrinetsimulator.config.layout.PlaceLayout;
import ar.edu.unc.david.petrinetsimulator.config.layout.TransitionLayout;
import ar.edu.unc.david.petrinetsimulator.config.logic.AgentConfig;
import ar.edu.unc.david.petrinetsimulator.config.logic.SimulationConfig;
import ar.edu.unc.david.petrinetsimulator.config.main.MainConfig;
import ar.edu.unc.david.petrinetsimulator.config.main.NetworkEntry;
import ar.edu.unc.david.petrinetsimulator.core.SimulatorEngine;
import ar.edu.unc.david.petrinetsimulator.core.SimulatorEngine.Components;
import ar.edu.unc.david.petrinetsimulator.log.UiAwarePetriLogger;
import ar.edu.unc.david.petrinetsimulator.notify.FiringEvent;
import ar.edu.unc.david.petrinetsimulator.notify.PetriEvent;
import ar.edu.unc.david.petrinetsimulator.notify.QueuedNotifier;
import ar.edu.unc.david.petrinetsimulator.ui.PetriCanvas;
import ar.edu.unc.david.petrinetsimulator.ui.SimulationRelay;
import ar.edu.unc.david.petrinetsimulator.ui.monitor.MonitorView;
import ar.edu.unc.david.petrinetsimulator.ui.nodes.TransitionView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/** JavaFX entry point: wires the simulation engine to the visual canvas. */
public class PetriSimApp extends Application {
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
    MainConfig mainConfig = ConfigLoader.load("config.json", MainConfig.class);

    NetworkEntry network =
        mainConfig.networks().stream()
            .filter(n -> n.id().equals(mainConfig.activeNetworkId()))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Active network not found in config"));

    String configPath = network.logicPath();
    String layoutPath = network.layoutPath();

    System.out.println("Starting simulation with active network: " + network.name());

    LayoutConfig layout = ConfigLoader.load(layoutPath, LayoutConfig.class);

    SimulationConfig config = ConfigLoader.load(configPath, SimulationConfig.class);
    TransitionView.setTransitionSize(layout.transW(), layout.transH());

    BlockingQueue<PetriEvent> queue = new LinkedBlockingQueue<>();
    logger =
        new UiAwarePetriLogger(
            SimulatorEngine.resolveLogFile(config.logging()), new QueuedNotifier(queue));

    PetriCanvas canvas = buildCanvas(layout, config);
    canvas.setTopology(config.net().pre(), config.net().post());
    canvas.setTransitionMap(layout.sequences());
    canvas.updateUi(new FiringEvent(-1, null, null, config.net().initialMarking().clone(), 0));

//    MonitorView monitorView = new MonitorView(config.net().transitions().size());
//    monitorView.setPrefHeight(160);

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

  private PetriCanvas buildCanvas(LayoutConfig layout, SimulationConfig config) {
    List<AgentConfig> threads = config.agents();
    Map<Integer, Integer> transitionIds = new HashMap<>();
    Map<Integer, Color> transitionColors = new HashMap<>();
    for (AgentConfig agent : threads) {
      transitionIds.put(agent.sequence()[0], agent.count());
      transitionColors.put(agent.sequence()[0], Color.valueOf(agent.color()));
    }
    PetriCanvas canvas = new PetriCanvas();

    for (PlaceLayout p : layout.places()) {
      canvas.addPlace(p.id(), p.label(), p.x(), p.y());
    }

    for (TransitionLayout t : layout.transitions()) {
      int threadCount = transitionIds.getOrDefault(t.id(), 0);
      Color color = transitionColors.getOrDefault(t.id(), Color.BLACK);

      canvas.addTransition(t.id(), t.label(), t.x(), t.y(), t.threadAnchor(), threadCount, color);
    }

    canvas.setMonitorView(layout.transitions().size());

    canvas.addArcs(layout.tokenPaths());
    canvas.addThreadPaths(layout.threadPaths());

    return canvas;
  }
}
