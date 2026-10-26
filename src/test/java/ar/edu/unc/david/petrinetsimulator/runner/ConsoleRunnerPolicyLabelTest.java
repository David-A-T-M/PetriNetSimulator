package ar.edu.unc.david.petrinetsimulator.runner;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for ConsoleRunner policy label resolution. */
class ConsoleRunnerPolicyLabelTest {

  @Test
  @DisplayName("uses policy type lower-case when no policy path override is provided")
  void policyLabelUsesPolicyTypeWithoutOverride() {
    assertEquals("random", ConsoleRunner.policyLabel("data_proc", "RANDOM", null));
  }

  @Test
  @DisplayName("strips network prefix from policy file name")
  void policyLabelStripsNetworkPrefix() {
    assertEquals(
        "priority",
        ConsoleRunner.policyLabel(
            "data_proc", "proportion", "src/main/resources/policies/data_proc_priority.json"));
  }

  @Test
  @DisplayName("keeps full policy file name when network prefix is absent")
  void policyLabelKeepsFullNameWithoutPrefix() {
    assertEquals(
        "priority",
        ConsoleRunner.policyLabel(
            "reserve_sys", "proportion", "src/main/resources/policies/priority.json"));
  }

  @Test
  @DisplayName("supports windows-style policy paths")
  void policyLabelSupportsWindowsPaths() {
    assertEquals(
        "balanced",
        ConsoleRunner.policyLabel(
            "reserve_sys",
            "proportion",
            "src\\main\\resources\\policies\\reserve_sys_balanced.json"));
  }

  @Test
  @DisplayName("supports short policy file names")
  void policyLabelSupportsShortPolicyName() {
    assertEquals(
        "priority",
        ConsoleRunner.policyLabel("data_proc", "proportion", "data_proc_priority.json"));
  }
}
