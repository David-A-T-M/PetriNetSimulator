package ar.edu.unc.david.petrinetsimulator.log;

import ar.edu.unc.david.petrinetsimulator.core.PetriEvent;
import ar.edu.unc.david.petrinetsimulator.core.PetriNotifier;

/** Extends PetriLogger to also notify a UI about transition firings. */
public class UiAwarePetriLogger extends PetriLogger {
  private final PetriNotifier notifier;

  public UiAwarePetriLogger(String fileName, PetriNotifier notifier) {
    super(fileName);
    this.notifier = notifier;
  }

  @Override
  public synchronized void logFire(int transition, int[] before, int[] after) {
    super.logFire(transition, before, after);

    notifier.notify(
        new PetriEvent(transition, before.clone(), after.clone(), System.currentTimeMillis()));
  }
}
