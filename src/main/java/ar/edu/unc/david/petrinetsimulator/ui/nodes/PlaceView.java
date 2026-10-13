package ar.edu.unc.david.petrinetsimulator.ui.nodes;

import javafx.geometry.Pos;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

/** Visual representation of a Petri net place. */
public class PlaceView extends StackPane implements NodeView {
  private static final double PLACE_RADIUS = 25.0;
  private static final double TOKEN_RADIUS = 3.0;
  private final String label;
  private final FlowPane tokenContainer = new FlowPane();

  /** Creates a PlaceView with the given id, label, and position. */
  public PlaceView(String label, double x, double y) {
    this.label = label;

    Circle place = new Circle(PLACE_RADIUS, Color.WHITE);
    place.setStroke(Color.BLACK);
    place.setStrokeWidth(2);

    this.tokenContainer.setAlignment(Pos.CENTER);
    this.tokenContainer.setHgap(3);
    this.tokenContainer.setVgap(3);
    this.tokenContainer.setMaxSize(PLACE_RADIUS * 1.5, PLACE_RADIUS * 1.5);

    Tooltip tooltip = new Tooltip(label);
    Tooltip.install(this, tooltip);
    tooltip.setShowDelay(Duration.millis(200));

    this.getChildren().addAll(place, tokenContainer);
    this.setCenter(x, y);
  }

  /** Updates the visual marking of the place based on the given token count. */
  public void updateMarking(int tokenCount) {
    tokenContainer.getChildren().clear();
    for (int i = 0; i < tokenCount; i++) {
      tokenContainer.getChildren().add(new Circle(TOKEN_RADIUS, Color.BLACK));
    }
  }

  private void setCenter(double x, double y) {
    relocate(x - PLACE_RADIUS, y - PLACE_RADIUS);
  }

  @Override
  public double centerX() {
    return getLayoutX() + PLACE_RADIUS;
  }

  @Override
  public double centerY() {
    return getLayoutY() + PLACE_RADIUS;
  }

  public String label() {
    return label;
  }
}
