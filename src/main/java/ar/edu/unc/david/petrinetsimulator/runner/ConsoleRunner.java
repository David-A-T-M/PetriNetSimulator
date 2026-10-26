package ar.edu.unc.david.petrinetsimulator.runner;

import ar.edu.unc.david.petrinetsimulator.config.ConfigLoader;
import ar.edu.unc.david.petrinetsimulator.config.logic.LoggingConfig;
import ar.edu.unc.david.petrinetsimulator.config.logic.PolicyConfig;
import ar.edu.unc.david.petrinetsimulator.config.logic.SimulationConfig;
import ar.edu.unc.david.petrinetsimulator.config.main.MainConfig;
import ar.edu.unc.david.petrinetsimulator.config.main.NetworkEntry;
import ar.edu.unc.david.petrinetsimulator.core.SimulatorEngine;
import ar.edu.unc.david.petrinetsimulator.log.PetriLogger;
import java.nio.file.Files;
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

    SimulationConfig loadedConfig = ConfigLoader.load(configPath, SimulationConfig.class);
    SimulationConfig baseConfig = withPolicyOverride(loadedConfig, options.policyPath());
    String configuredPolicyType =
        baseConfig.policy() == null || baseConfig.policy().type() == null
            ? "unknown"
            : baseConfig.policy().type().toLowerCase(Locale.ROOT);
    String policyType = policyLabel(net.id(), configuredPolicyType, options.policyPath());

    int batchRuns = Math.max(1, options.batchRuns());
    boolean batchMode = batchRuns > 1;
    List<RunStatistics> allRunStats = new ArrayList<>();

    for (int runIndex = 1; runIndex <= batchRuns; runIndex++) {
      String logFile = resolveRunLogFile(baseConfig, net.id(), policyType, runIndex, batchMode);
      SimulationConfig runConfig = withLogFile(baseConfig, logFile);

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
          baseConfig.net().transitions().size());
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
      SimulationConfig baseConfig,
      String networkId,
      String policyType,
      int runIndex,
      boolean batchMode) {
    if (!batchMode) {
      return SimulatorEngine.resolveLogFile(baseConfig.logging());
    }

    return String.format(Locale.ROOT, "runs/%s_%s_run%03d.csv", networkId, policyType, runIndex);
  }

  private static SimulationConfig withLogFile(SimulationConfig baseConfig, String logFile) {
    boolean logWaitWake = baseConfig.logging() != null && baseConfig.logging().logWaitWake();
    LoggingConfig logging = new LoggingConfig(logFile, logWaitWake);

    return new SimulationConfig(
        baseConfig.net(),
        baseConfig.placeInvariants(),
        baseConfig.agents(),
        logging,
        baseConfig.runtime(),
        baseConfig.policy(),
        baseConfig.stop(),
        baseConfig.transitionInvariants());
  }

  private static SimulationConfig withPolicyOverride(
      SimulationConfig baseConfig, String policyPath) {
    if (policyPath == null || policyPath.isBlank()) {
      return baseConfig;
    }

    PolicyConfig override = ConfigLoader.load(resolvePolicyPath(policyPath), PolicyConfig.class);
    return new SimulationConfig(
        baseConfig.net(),
        baseConfig.placeInvariants(),
        baseConfig.agents(),
        baseConfig.logging(),
        baseConfig.runtime(),
        override,
        baseConfig.stop(),
        baseConfig.transitionInvariants());
  }

  private static String resolvePolicyPath(String policyPath) {
    Path direct = Path.of(policyPath);
    if (Files.exists(direct)) {
      return direct.toString();
    }

    Path underPolicies = Path.of("src", "main", "resources", "policies", policyPath);
    if (Files.exists(underPolicies)) {
      return underPolicies.toString();
    }

    if (!policyPath.toLowerCase(Locale.ROOT).endsWith(".json")) {
      Path underPoliciesJson =
          Path.of("src", "main", "resources", "policies", policyPath + ".json");
      if (Files.exists(underPoliciesJson)) {
        return underPoliciesJson.toString();
      }
    }

    return policyPath;
  }

  static String policyLabel(String networkId, String policyType, String policyPath) {
    if (policyPath == null || policyPath.isBlank()) {
      return policyType.toLowerCase(Locale.ROOT);
    }

    String normalized = policyPath.replace("\\", "/");
    String fileName = normalized.substring(normalized.lastIndexOf("/") + 1);
    String withoutExtension =
        fileName.toLowerCase(Locale.ROOT).endsWith(".json")
            ? fileName.substring(0, fileName.length() - 5)
            : fileName;

    String networkPrefix = networkId + "_";
    if (withoutExtension.startsWith(networkPrefix)) {
      return withoutExtension.substring(networkPrefix.length());
    }

    return withoutExtension;
  }

  private static String stripCsvExtension(String filename) {
    return filename.toLowerCase(Locale.ROOT).endsWith(".csv")
        ? filename.substring(0, filename.length() - 4)
        : filename;
  }

  private record CliOptions(int batchRuns, String networkId, String policyPath) {
    private static CliOptions parse(String[] args) {
      int batchRuns = 1;
      String networkId = null;
      String policyPath = null;

      for (int i = 0; i < args.length; i++) {
        String arg = args[i];
        if ("--batch".equals(arg) && i + 1 < args.length) {
          batchRuns = Integer.parseInt(args[++i]);
          continue;
        }

        if ("--network".equals(arg) && i + 1 < args.length) {
          networkId = args[++i];
          continue;
        }

        if ("--policy".equals(arg) && i + 1 < args.length) {
          policyPath = args[++i];
        }
      }
      return new CliOptions(batchRuns, networkId, policyPath);
    }
  }
}
