package ar.edu.unc.david.petrinetsimulator.ui.nodes;

import javafx.animation.PauseTransition;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.util.Duration;

/** * Visual representation of a Petri net transition. Displays a rectangle with a label. */
public class TransitionView extends StackPane implements NodeView {
  private static final double W = 50.0;
  private static final double H = 20.0;
  private final String label;
  private final Rectangle rect;

  /** Creates a TransitionView with the given id, label, and position. */
  public TransitionView(String label, double x, double y) {
    this.label = label;

    this.rect = new Rectangle(W, H, Color.DARKSLATEGRAY);
    this.rect.setArcWidth(5);
    this.rect.setArcHeight(5);

    Text text = new Text(label);
    text.setTranslateY(-20);

    this.getChildren().addAll(rect, text);
    this.setCenter(x, y);
  }

  /** Flashes the transition to indicate it has fired. */
  public void flash() {
    rect.setFill(Color.LIME);
    PauseTransition pause = new PauseTransition(Duration.millis(300));
    pause.setOnFinished(e -> rect.setFill(Color.DARKSLATEGRAY));
    pause.play();
  }

  private void setCenter(double x, double y) {
    relocate(x - W / 2.0, y - H / 2.0);
  }

  @Override
  public double centerX() {
    return getLayoutX() + W / 2.0;
  }

  @Override
  public double centerY() {
    return getLayoutY() + H / 2.0;
  }

  public String label() {
    return label;
  }
}
