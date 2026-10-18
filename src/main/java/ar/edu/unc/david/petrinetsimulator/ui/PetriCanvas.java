package ar.edu.unc.david.petrinetsimulator.ui;

import ar.edu.unc.david.petrinetsimulator.config.layout.ArcLayoutConfig;
import ar.edu.unc.david.petrinetsimulator.config.layout.PathConfig;
import ar.edu.unc.david.petrinetsimulator.config.layout.SequenceConfig;
import ar.edu.unc.david.petrinetsimulator.config.layout.ThreadPathConfig;
import ar.edu.unc.david.petrinetsimulator.config.layout.WaypointConfig;
import ar.edu.unc.david.petrinetsimulator.notify.FiringEvent;
import ar.edu.unc.david.petrinetsimulator.notify.PetriEvent;
import ar.edu.unc.david.petrinetsimulator.notify.ThreadEvent;
import ar.edu.unc.david.petrinetsimulator.ui.monitor.MonitorView;
import ar.edu.unc.david.petrinetsimulator.ui.nodes.PlaceView;
import ar.edu.unc.david.petrinetsimulator.ui.nodes.TransitionView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PathTransition;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.util.Duration;

/**
 * A JavaFX Pane that visualizes a Petri net. It manages PlaceView and TransitionView nodes, and
 * updates the UI based on PetriEvents.
 */
public class PetriCanvas extends Pane {
  private final Map<Integer, PlaceView> placeMap = new HashMap<>();
  private final Map<Integer, TransitionView> transitionMap = new HashMap<>();
  private int[][] pre;
  private int[][] post;
  private final Map<String, Path> tokenPaths = new HashMap<>();
  private final Map<String, Path> threadPaths = new HashMap<>();
  Map<Integer, Integer> nextStepMap = new HashMap<>();
  private MonitorView monitorView;

  public PetriCanvas() {
    this.setStyle("-fx-background-color: #f4f4f4; -fx-border-color: #cccccc;");
  }

  /** Adds a place to the canvas with the given id, label, and position. */
  public void addPlace(int id, String label, double x, double y) {
    PlaceView pv = new PlaceView(label, x, y);
    placeMap.put(id, pv);
    this.getChildren().add(pv);
  }

  public void setTopology(int[][] pre, int[][] post) {
    this.pre = pre;
    this.post = post;
  }

  public void setMonitorView(int numTransitions) {
    monitorView = new MonitorView(numTransitions);
    monitorView.setPrefHeight(160);
    this.getChildren().add(monitorView);
  }

  public void setTransitionMap(List<SequenceConfig> sequences) {
    for (SequenceConfig seq : sequences) {
      int size = seq.sequence().size();

      for (int i = 0; i < size; i++) {
        Integer currentT = seq.sequence().get(i);

        Integer nextT = seq.sequence().get((i + 1) % size);

        nextStepMap.put(currentT, nextT);
      }
    }
  }

  /** Adds a transition to the canvas with the given id, label, and position. */
  public void addTransition(
      int id, String label, double x, double y, String threadAnchor, int numThreads, Color color) {
    TransitionView tv = new TransitionView(label, x, y, threadAnchor);
    transitionMap.put(id, tv);
    tv.updateMarking(numThreads, color);
    this.getChildren().add(tv);
  }

  /** Adds arcs to the canvas based on the given list of ArcLayoutConfig. */
  public void addArcs(List<ArcLayoutConfig> arcs) {
    if (arcs == null || arcs.isEmpty()) {
      return;
    }

    for (PathConfig arc : arcs) {
      if (arc == null || arc.waypoints() == null || arc.waypoints().size() < 2) {
        continue;
      }

      Path path = new Path();
      List<WaypointConfig> points = arc.waypoints();

      WaypointConfig first = points.getFirst();
      path.getElements().add(new MoveTo(first.x(), first.y()));

      for (int i = 1; i < points.size(); i++) {
        WaypointConfig wp = points.get(i);
        path.getElements().add(new LineTo(wp.x(), wp.y()));
      }

      path.setStroke(Color.BLACK);
      tokenPaths.put(arc.arc(), path);

      path.setStrokeWidth(1.5);
      path.setFill(null);

      this.getChildren().addFirst(path);
    }
  }

  public void addThreadPaths(List<ThreadPathConfig> arcs) {
    if (arcs == null || arcs.isEmpty()) {
      return;
    }

    for (PathConfig arc : arcs) {
      if (arc == null || arc.waypoints() == null || arc.waypoints().size() < 2) {
        continue;
      }

      Path path = new Path();
      List<WaypointConfig> points = arc.waypoints();

      WaypointConfig first = points.getFirst();
      path.getElements().add(new MoveTo(first.x(), first.y()));

      for (int i = 1; i < points.size(); i++) {
        WaypointConfig wp = points.get(i);
        path.getElements().add(new LineTo(wp.x(), wp.y()));
      }

      path.setStroke(Color.web("#f4f4f4"));
      threadPaths.put(arc.arc(), path);

      path.setStrokeWidth(1.5);
      path.setFill(null);

      this.getChildren().addFirst(path);
    }
  }

  /**
   * Updates the UI based on the given PetriEvent. It dispatches to specific handlers based on the
   * event type.
   */
  public void updateUi(PetriEvent event) {
    switch (event) {
      case FiringEvent fe -> handleFiring(fe);
      case ThreadEvent te -> handleThreadEvent(te);
    }
  }

  private void handleThreadEvent(ThreadEvent event) {
    monitorView.slots().get(event.transitionId()).addThread(event.threadId());
  }

  /**
   * Updates the UI based on the given PetriEvent. It animates token movements and updates markings
   * accordingly.
   */
  private void handleFiring(FiringEvent event) {
    int t = event.transitionId();
    TransitionView tv = transitionMap.get(t);

    if (tv == null
        || pre == null
        || post == null
        || event.markingBefore() == null
        || event.threadId() == null) {
      applyMarking(event.newMarking());
      return;
    }

    monitorView.slots().get(t).removeSpecificThread(event.threadId());

    Color color = tv.removeThread();
    String src = tv.label();
    Integer dstIndex = nextStepMap.get(t);
    String dst = transitionMap.get(dstIndex).label();
    String key = src + "-" + dst;
    Circle token = new Circle(2, color);
    token.setMouseTransparent(true);
    this.getChildren().add(token);
    PathTransition threadAnimation =
        new PathTransition(Duration.millis(300), threadPaths.get(key), token);
    threadAnimation.setInterpolator(Interpolator.EASE_IN);
    threadAnimation.setOnFinished(e -> this.getChildren().remove(token));

    List<PlaceView> toTransition = new ArrayList<>();
    List<PlaceView> fromTransition = new ArrayList<>();

    for (int p = 0; p < pre.length; p++) {
      if (pre[p][t] > 0) {
        PlaceView pv = placeMap.get(p);
        if (pv != null) {
          toTransition.add(pv);
        }
      }

      if (post[p][t] > 0) {
        PlaceView pv = placeMap.get(p);
        if (pv != null) {
          fromTransition.add(pv);
        }
      }
    }

    for (int p = 0; p < pre.length; p++) {
      if (pre[p][t] > 0) {
        PlaceView pv = placeMap.get(p);
        if (pv != null) {
          pv.updateMarking(event.markingBefore()[p] - pre[p][t]);
        }
      }
    }

    tv.flash();

    ParallelTransition phase1 = buildIncomingAnimation(toTransition, tv);
    phase1.getChildren().add(threadAnimation);
    phase1.setOnFinished(
        e -> {
          ParallelTransition phase2 = buildOutgoingAnimation(tv, fromTransition);
          phase2.setOnFinished(e2 -> applyMarking(event.newMarking()));
          transitionMap.get(dstIndex).addThread(color);
          phase2.play();
        });

    phase1.play();
  }

  private ParallelTransition buildIncomingAnimation(List<PlaceView> inputs, TransitionView tv) {
    return animate(inputs, tv, true);
  }

  private ParallelTransition buildOutgoingAnimation(TransitionView tv, List<PlaceView> outputs) {
    return animate(outputs, tv, false);
  }

  private ParallelTransition animate(List<PlaceView> places, TransitionView tv, boolean isIn) {
    ParallelTransition parallel = new ParallelTransition();

    for (PlaceView place : places) {
      String key = isIn ? (place.label() + "-" + tv.label()) : (tv.label() + "-" + place.label());

      Path road = tokenPaths.get(key);

      if (road != null) {
        Circle token = makeToken();
        this.getChildren().add(token);

        PathTransition pt = new PathTransition(Duration.millis(150), road, token);
        pt.setInterpolator(Interpolator.EASE_IN);

        pt.setOnFinished(e -> this.getChildren().remove(token));

        parallel.getChildren().add(pt);
      } else {
        System.err.println("No path found for " + key);
      }
    }
    return parallel;
  }

  private void applyMarking(int[] marking) {
    for (int i = 0; i < marking.length; i++) {
      PlaceView pv = placeMap.get(i);
      if (pv != null) {
        pv.updateMarking(marking[i]);
      }
    }
  }

  private Circle makeToken() {
    Circle c = new Circle(5, Color.BLACK);
    c.setMouseTransparent(true);
    return c;
  }
}
