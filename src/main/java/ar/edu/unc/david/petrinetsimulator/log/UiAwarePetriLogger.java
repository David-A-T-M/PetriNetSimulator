package ar.edu.unc.david.petrinetsimulator.log;

import ar.edu.unc.david.petrinetsimulator.notify.FiringEvent;
import ar.edu.unc.david.petrinetsimulator.notify.PetriNotifier;
import ar.edu.unc.david.petrinetsimulator.notify.ThreadEvent;

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
        new FiringEvent(
            transition,
            Thread.currentThread().getName(),
            before.clone(),
            after.clone(),
            System.currentTimeMillis()));
  }

  @Override
  public synchronized void logWait(int t) {
    notifier.notify(new ThreadEvent(t, Thread.currentThread().getName()));
  }
}
