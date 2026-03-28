package ar.edu.unc.david.petrinetsimulator.ui;

import ar.edu.unc.david.petrinetsimulator.core.PetriEvent;
import ar.edu.unc.david.petrinetsimulator.ui.nodes.NodeView;
import ar.edu.unc.david.petrinetsimulator.ui.nodes.PlaceView;
import ar.edu.unc.david.petrinetsimulator.ui.nodes.TransitionView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
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

  public PetriCanvas() {
    this.setStyle("-fx-background-color: #f4f4f4; -fx-border-color: #cccccc;");
  }

  /** Adds a place to the canvas with the given id, label, and position. */
  public void addPlace(int id, String label, double x, double y) {
    PlaceView pv = new PlaceView(id, label, x, y);
    placeMap.put(id, pv);
    this.getChildren().add(pv);
  }

  public void setTopology(int[][] pre, int[][] post) {
    this.pre = pre;
    this.post = post;
  }

  /** Adds a transition to the canvas with the given id, label, and position. */
  public void addTransition(int id, String label, double x, double y) {
    TransitionView tv = new TransitionView(id, label, x, y);
    transitionMap.put(id, tv);
    this.getChildren().add(tv);
  }

  /**
   * Adds an arc between a place and a transition. The direction is determined by isPlaceToTrans.
   */
  public void addArc(int fromId, int toId, boolean isPlaceToTrans) {
    NodeView source = isPlaceToTrans ? placeMap.get(fromId) : transitionMap.get(fromId);
    NodeView target = isPlaceToTrans ? transitionMap.get(toId) : placeMap.get(toId);

    if (source == null || target == null) {
      return;
    }

    double startX = source.centerX();
    double startY = source.centerY();
    double endX = target.centerX();
    double endY = target.centerY();

    Line line = new Line(startX, startY, endX, endY);
    line.setStroke(Color.web("#606060"));
    line.setStrokeWidth(1.5);

    this.getChildren().addFirst(line);
  }

  /**
   * Updates the UI based on the given PetriEvent. It animates token movements and updates markings
   * accordingly.
   */
  public void updateUi(PetriEvent event) {
    int t = event.transitionId();
    TransitionView tv = transitionMap.get(t);

    if (tv == null || pre == null || post == null || event.markingBefore() == null) {
      applyMarking(event.newMarking());
      return;
    }

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
    phase1.setOnFinished(
        e -> {
          ParallelTransition phase2 = buildOutgoingAnimation(tv, fromTransition);
          phase2.setOnFinished(e2 -> applyMarking(event.newMarking()));
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
      double startX = isIn ? place.centerX() : tv.centerX();
      double startY = isIn ? place.centerY() : tv.centerY();

      Circle token = makeToken();
      token.setCenterX(startX);
      token.setCenterY(startY);
      this.getChildren().add(token);

      double endX = isIn ? tv.centerX() : place.centerX();
      double endY = isIn ? tv.centerY() : place.centerY();

      TranslateTransition tt = new TranslateTransition(Duration.millis(400), token);
      tt.setToX(endX - startX);
      tt.setToY(endY - startY);
      tt.setOnFinished(e -> this.getChildren().remove(token));

      parallel.getChildren().add(tt);
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
