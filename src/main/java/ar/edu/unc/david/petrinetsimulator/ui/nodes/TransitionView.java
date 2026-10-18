package ar.edu.unc.david.petrinetsimulator.ui.nodes;

import javafx.animation.PauseTransition;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/** * Visual representation of a Petri net transition. Displays a rectangle with a label. */
public class TransitionView extends StackPane implements NodeView {
  private static final double TOKEN_RADIUS = 2.0;
  private static double W;
  private static double H;
  private final String label;
  private final Rectangle rect;
  private final FlowPane threadContainer = new FlowPane();

  /** Creates a TransitionView with the given id, label, and position. */
  public TransitionView(String label, double x, double y, String threadAnchor) {
    this.label = label;

    this.rect = new Rectangle(W, H, Color.SALMON);
    this.rect.setArcWidth(5);
    this.rect.setArcHeight(5);

    this.threadContainer.setAlignment(Pos.CENTER);
    this.threadContainer.setHgap(2);
    this.threadContainer.setVgap(2);
    this.threadContainer.setMaxSize(10, 5);
    ThreadAnchor anchor = ThreadAnchor.valueOf(threadAnchor);
    anchor.apply(threadContainer);

    Tooltip tooltip = new Tooltip(label);
    Tooltip.install(this, tooltip);
    tooltip.setShowDelay(Duration.millis(200));

    this.getChildren().addAll(rect, threadContainer);
    this.setCenter(x, y);
  }

  public static void setTransitionSize(double width, double height) {
    W = width;
    H = height;
  }

  /** Flashes the transition to indicate it has fired. */
  public void flash() {
    rect.setFill(Color.ORANGERED);
    PauseTransition pause = new PauseTransition(Duration.millis(300));
    pause.setOnFinished(e -> rect.setFill(Color.SALMON));
    pause.play();
  }

  public void updateMarking(int tokenCount, Color color) {
    threadContainer.getChildren().clear();
    for (int i = 0; i < tokenCount; i++) {
      threadContainer.getChildren().add(new Circle(TOKEN_RADIUS, color));
    }
  }

  public void addThread(Color color) {
    threadContainer.getChildren().add(new Circle(TOKEN_RADIUS, color));
  }

  public Color removeThread() {
    Node removed = null;
    if (!threadContainer.getChildren().isEmpty()) {
      removed = threadContainer.getChildren().removeFirst();
    }
    return removed instanceof Circle ? (Color) ((Circle) removed).getFill() : null;
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

  public enum ThreadAnchor {
    TOP,
    TOP_RIGHT,
    TOP_LEFT,
    BOTTOM,
    BOTTOM_RIGHT,
    BOTTOM_LEFT,
    LEFT,
    RIGHT;

    public void apply(Region container) {
      switch (this) {
        case TOP -> {
          container.setTranslateY(-(H / 1.5));
          container.setTranslateX(0);
        }
        case TOP_RIGHT -> {
          container.setTranslateY(-(H / 1.5));
          container.setTranslateX(W / 1.5);
        }
        case TOP_LEFT -> {
          container.setTranslateY(-(H / 1.5));
          container.setTranslateX(-(W / 1.5));
        }
        case BOTTOM -> {
          container.setTranslateY(H / 1.5);
          container.setTranslateX(0);
        }
        case BOTTOM_RIGHT -> {
          container.setTranslateY(H / 1.5);
          container.setTranslateX(W / 1.5);
        }
        case BOTTOM_LEFT -> {
          container.setTranslateY(H / 1.5);
          container.setTranslateX(-(W / 1.5));
        }
        case LEFT -> {
          container.setTranslateX(-(W / 1.5));
          container.setTranslateY(0);
        }
        case RIGHT -> {
          container.setTranslateX(W / 1.5);
          container.setTranslateY(0);
        }
        default -> {
          container.setTranslateX(0);
          container.setTranslateY(0);
        }
      }
    }
  }
}
