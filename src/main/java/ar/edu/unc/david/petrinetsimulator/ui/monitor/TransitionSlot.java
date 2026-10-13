package ar.edu.unc.david.petrinetsimulator.ui.monitor;

import java.util.List;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

public class TransitionSlot extends VBox {

  private static final Color COLOR_DISABLED = Color.web("#e74c3c");
  private static final Color COLOR_WAITING = Color.web("#f39c12");
  private static final Color COLOR_ENABLED = Color.web("#2ecc71");

  private final Rectangle box;
  private final Text label;
  private final VBox queueBox;

  public TransitionSlot(int id, String label) {
    this.setAlignment(Pos.TOP_CENTER);
    this.setSpacing(4);

    this.label = new Text(label);
    this.label.setStyle("-fx-font-weight: bold;");

    this.box = new Rectangle(20, 20, Color.WHITE);
    this.box.setArcWidth(6);
    this.box.setArcHeight(6);
    this.box.setStroke(Color.DARKGRAY);
    this.box.setStrokeWidth(1);

    this.queueBox = new VBox(2);
    this.queueBox.setAlignment(Pos.TOP_CENTER);

    StackPane boxWithLabel = new StackPane(this.box, this.label);
    boxWithLabel.setAlignment(Pos.CENTER);
    this.getChildren().addAll(boxWithLabel, this.queueBox);
  }

  /** color según estado de la transición */
  public void setTransitionState(TransitionState state) {
    box.setFill(
        switch (state) {
          case DISABLED -> COLOR_DISABLED;
          case WAITING_TIME -> COLOR_WAITING;
          case ENABLED -> COLOR_ENABLED;
        });
  }

  public void addThread(String threadId) {
    boolean exists = queueBox.getChildren().stream()
            .filter(node -> node instanceof Text)
            .map(node -> (Text) node)
            .anyMatch(textNode -> textNode.getText().equals(threadId));

    if (!exists) {
      queueBox.getChildren().add(new Text(threadId));
    }
  }

  public void removeSpecificThread(String threadId) {
    queueBox.getChildren().removeIf(node -> {
      // 1. Verificamos que el nodo sea de tipo Text
      if (node instanceof Text textNode) {
        // 2. Comparamos el contenido del texto
        return textNode.getText().equals(threadId);
      }
      return false;
    });
  }

  /** lista de nombres de hilos en cola */
  public void setQueue(List<String> threads) {
    queueBox.getChildren().clear();
    threads.forEach(
        name -> {
          Text t = new Text(name);
          t.setStyle("-fx-font-size: 10px;");
          queueBox.getChildren().add(t);
        });
  }

  public enum TransitionState {
    DISABLED,
    WAITING_TIME,
    ENABLED
  }
}
