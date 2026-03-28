package ar.edu.unc.david.petrinetsimulator.core;

/** Interface for notifying about Petri net events, such as transition firings. */
public interface PetriNotifier {
  void notify(PetriEvent event);
}
