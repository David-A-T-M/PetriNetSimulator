package ar.edu.unc.david.petrinetsimulator.runner;

import ar.edu.unc.david.petrinetsimulator.config.ConfigLoader;
import ar.edu.unc.david.petrinetsimulator.config.logic.LoggingConfig;
import ar.edu.unc.david.petrinetsimulator.config.logic.SimulationConfig;
import ar.edu.unc.david.petrinetsimulator.config.main.MainConfig;
import ar.edu.unc.david.petrinetsimulator.config.main.NetworkEntry;
import ar.edu.unc.david.petrinetsimulator.core.SimulatorEngine;
import ar.edu.unc.david.petrinetsimulator.log.PetriLogger;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Console-based runner: executes the simulation without any UI. */
public class ConsoleRunner {
  /** Runs the simulation based on the provided configuration file. */
  public static void main(String[] args) {
    CliOptions options = CliOptions.parse(args);

    MainConfig mainConfig = ConfigLoader.load("config.json", MainConfig.class);
    String selectedNetworkId =
        options.networkId() == null ? mainConfig.activeNetworkId() : options.networkId();

    NetworkEntry net =
        mainConfig.networks().stream()
            .filter(n -> n.id().equals(selectedNetworkId))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Selected network not found in config"));

    System.out.println("Starting simulation with network: " + net.name());

    String configPath = net.logicPath();

    SimulationConfig baseconfig = ConfigLoader.load(configPath, SimulationConfig.class);
    String policyType =
        baseconfig.policy() == null || baseconfig.policy().type() == null
            ? "unknown"
            : baseconfig.policy().type().toLowerCase(Locale.ROOT);

    int batchRuns = Math.max(1, options.batchRuns());
    boolean batchMode = batchRuns > 1;
    List<RunStatistics> allRunStats = new ArrayList<>();

    for (int runIndex = 1; runIndex <= batchRuns; runIndex++) {
      String logFile = resolveRunLogFile(baseconfig, net.id(), policyType, runIndex, batchMode);
      SimulationConfig runConfig = withLogFile(baseconfig, logFile);

      runOne(runConfig, batchMode);

      RunStatistics runStats =
          RunStatisticsAnalyzer.analyze(
              runIndex, net.id(), policyType, Path.of(logFile), runConfig);
      allRunStats.add(runStats);

      String summaryName = stripCsvExtension(logFile) + "_summary.csv";
      RunStatisticsCsvWriter.writeRunSummary(
          Path.of(summaryName), runStats, runConfig.net().transitions().size());
    }

    if (batchMode) {
      Path runsDir = Path.of("runs");
      String prefix = net.id() + "_" + policyType;
      RunStatisticsCsvWriter.writeBatchRuns(
          runsDir.resolve(prefix + "_runs.csv"),
          allRunStats,
          baseconfig.net().transitions().size());
      RunStatisticsCsvWriter.writeBatchAggregate(
          runsDir.resolve(prefix + "_batch_summary.csv"), allRunStats);
    }
  }

  private static void runOne(SimulationConfig config, boolean batchMode) {
    PetriLogger logger = new PetriLogger(SimulatorEngine.resolveLogFile(config.logging()));
    SimulatorEngine.Components c = SimulatorEngine.build(config, logger);

    List<Thread> threads = SimulatorEngine.startAgents(c.agents(), c.monitor());
    SimulatorEngine.shutdownGracefully(threads, c.runtime().joinTimeoutMs());

    logger.close();

    if (c.runtime().builtInValidation()) {
      if (batchMode) {
        SimulatorEngine.validateLog(c.logFile());
      } else {
        SimulatorEngine.validateLog(c.logFile(), config.transitionInvariants());
      }
    }
  }

  private static String resolveRunLogFile(
      SimulationConfig baseconfig,
      String networkId,
      String policyType,
      int runIndex,
      boolean batchMode) {
    if (!batchMode) {
      return SimulatorEngine.resolveLogFile(baseconfig.logging());
    }

    return String.format(Locale.ROOT, "runs/%s_%s_run%03d.csv", networkId, policyType, runIndex);
  }

  private static SimulationConfig withLogFile(SimulationConfig baseconfig, String logFile) {
    boolean logWaitWake = baseconfig.logging() != null && baseconfig.logging().logWaitWake();
    LoggingConfig logging = new LoggingConfig(logFile, logWaitWake);

    return new SimulationConfig(
        baseconfig.net(),
        baseconfig.placeInvariants(),
        baseconfig.agents(),
        logging,
        baseconfig.runtime(),
        baseconfig.policy(),
        baseconfig.stop(),
        baseconfig.transitionInvariants());
  }

  private static String stripCsvExtension(String filename) {
    return filename.toLowerCase(Locale.ROOT).endsWith(".csv")
        ? filename.substring(0, filename.length() - 4)
        : filename;
  }

  private record CliOptions(int batchRuns, String networkId) {
    private static CliOptions parse(String[] args) {
      int batchRuns = 1;
      String networkId = null;

      for (int i = 0; i < args.length; i++) {
        String arg = args[i];
        if ("--batch".equals(arg) && i + 1 < args.length) {
          batchRuns = Integer.parseInt(args[++i]);
          continue;
        }

        if ("--network".equals(arg) && i + 1 < args.length) {
          networkId = args[++i];
        }
      }
      return new CliOptions(batchRuns, networkId);
    }
  }
}
