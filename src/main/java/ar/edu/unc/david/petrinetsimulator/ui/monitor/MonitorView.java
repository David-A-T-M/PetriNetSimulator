package ar.edu.unc.david.petrinetsimulator.ui.monitor;

import java.util.LinkedHashMap;
import java.util.Map;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

public class MonitorView extends HBox {

  private final Text activeThreadLabel;
  private final Map<Integer, TransitionSlot> slots = new LinkedHashMap<>();

  // necesitamos Pre y marking actual para calcular el color
  private int[][] pre;
  private long[] alpha;
  private long[] sensitizationTs; // snapshot del motor — solo para color
  private int[] currentMarking = new int[0];

  public MonitorView(int numTransitions) {
    this.setSpacing(1);
    this.setPadding(new Insets(12));
    this.setStyle("-fx-background-color: transparent; -fx-border-radius: 8;");
    this.setAlignment(Pos.TOP_LEFT);

    // HBox activeBox = new HBox(4);
    // activeBox.setAlignment(Pos.TOP_CENTER);
    Text c0Label = new Text("Monitor");
    c0Label.setFill(Color.WHITE);
    c0Label.setStyle("-fx-font-weight: bold;");
    activeThreadLabel = new Text("—");
    activeThreadLabel.setFill(Color.LIGHTGREEN);
    activeThreadLabel.setStyle("-fx-font-size: 11px;");
    Rectangle c0Box = new Rectangle(40, 20, Color.web("#444"));
    c0Box.setArcWidth(6);
    c0Box.setArcHeight(6);
    c0Box.setStroke(Color.LIGHTGRAY);
    c0Box.setStrokeWidth(1.5);
    // activeBox.getChildren().addAll(c0Label, c0Box, activeThreadLabel);
    // this.getChildren().add(activeBox);

    // separador visual
    // Separator sep = new Separator(Orientation.VERTICAL);
    // this.getChildren().add(sep);

    // slots por transición
    for (int t = 0; t < numTransitions; t++) {
      TransitionSlot slot = new TransitionSlot(t, "T" + t);
      slots.put(t, slot);
      this.getChildren().add(slot);
    }
  }

  public Map<Integer, TransitionSlot> slots() {
    return slots;
  }

  /** llamado por MonitorRelay vía Platform.runLater */
  /*public void update(MonitorStateEvent event) {
    // hilo activo en C0
    activeThreadLabel.setText(event.activeThread() != null ? event.activeThread() : "—");

    // colas de hilos por transición
    event
        .queues()
        .forEach(
            (t, list) -> {
              TransitionSlot slot = slots.get(t);
              if (slot != null) slot.setQueue(list);
            });

    // limpiar colas de transiciones no mencionadas en el evento
    slots.forEach(
        (t, slot) -> {
          if (!event.queues().containsKey(t)) slot.setQueue(List.of());
        });
  }*/

  /** llamado cuando llega un PetriEvent — actualiza colores */
  /*public void updateColors(int[] marking, int[][] pre, long[] alpha, long[] sensitizationTs) {
    this.currentMarking = marking;
    for (int t = 0; t < slots.size(); t++) {
      slots.get(t).setTransitionState(computeState(t, marking, pre, alpha, sensitizationTs));
    }
  }

  private TransitionSlot.TransitionState computeState(
      int t, int[] marking, int[][] pre, long[] alpha, long[] sensitizationTs) {

    // verificar si está sensibilizada (M >= Pre[·][t])
    boolean tokensOk = true;
    for (int p = 0; p < marking.length; p++) {
      if (pre[p][t] > marking[p]) {
        tokensOk = false;
        break;
      }
    }
    if (!tokensOk) return TransitionSlot.TransitionState.DISABLED;

    // tiene tokens pero ¿está en la ventana de tiempo?
    long ts = sensitizationTs[t];
    if (ts == -1) return TransitionSlot.TransitionState.DISABLED;
    long elapsed = System.currentTimeMillis() - ts;
    if (elapsed < alpha[t]) return TransitionSlot.TransitionState.WAITING_TIME;

    return TransitionSlot.TransitionState.ENABLED;
  }*/
}
