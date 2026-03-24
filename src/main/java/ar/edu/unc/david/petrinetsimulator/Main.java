package ar.edu.unc.david.petrinetsimulator;

import static ar.edu.unc.david.petrinetsimulator.PetriNet.createProducerConsumer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Main class to run the Petri net simulation for a producer-consumer scenario. */
public class Main {
  /** Entry point of the application. */
  public static void main(String[] args) {
    int numCycles = 500;
    String logFile = "petri_log.txt";

    if (args.length >= 1) {
      numCycles = Integer.parseInt(args[0]);
    }

    if (args.length >= 2) {
      logFile = args[1];
    }

    PetriNet net = createProducerConsumer();
    PetriLogger logger = new PetriLogger(logFile);
    Policy policy = new RandomPolicy();
    Monitor monitor = new Monitor(net, policy, logger);

    int[] producerSeq = {0, 1, 2}; // Produce -> Deposits -> Finishes
    int[] consumerSeq = {3, 4, 5}; // Withdraws -> Consumes -> Finishes

    List<Thread> threads = new ArrayList<>();

    for (int i = 0; i < 2; i++) {
      Thread p = new Thread(new PetriAgent(monitor, producerSeq, numCycles), "Prod-" + i);
      threads.add(p);
      p.start();
    }

    for (int i = 0; i < 2; i++) {
      Thread c = new Thread(new PetriAgent(monitor, consumerSeq, numCycles), "Cons-" + i);
      threads.add(c);
      c.start();
    }

    long deadline = System.currentTimeMillis() + 30_000;
    for (Thread t : threads) {
      long remaining = deadline - System.currentTimeMillis();
      if (remaining <= 0) {
        break;
      }
      try {
        t.join(remaining);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
    }

    for (Thread t : threads) {
      if (t.isAlive()) {
        t.interrupt();
      }
    }
    for (Thread t : threads) {
      try {
        t.join(1000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
    }

    logger.close();

    List<String> errors = PetriLogValidator.validateFile(Path.of(logFile));
    if (errors.isEmpty()) {
      System.out.println("[OK] Log is consistent.");
    } else {
      System.out.println("[WARN] Log validation errors:");
      errors.forEach(System.out::println);
    }
  }
}
