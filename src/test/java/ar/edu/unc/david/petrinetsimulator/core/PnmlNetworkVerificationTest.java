package ar.edu.unc.david.petrinetsimulator.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import ar.edu.unc.david.petrinetsimulator.config.ConfigLoader;
import ar.edu.unc.david.petrinetsimulator.config.logic.SimulationConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This test verifies that the PNML files exported by PIPE match the expected network definitions
 * provided in JSON format. It checks the pre- and post-matrices as well as the initial marking of
 * places.
 */
public class PnmlNetworkVerificationTest {
  private static final Pattern NUMERIC_SUFFIX = Pattern.compile("(\\d+)$");

  @Test
  @DisplayName("PIPE exports match the JSON networks for 2024 and 2025")
  void pnmlExports_matchJsonDefinitions() throws Exception {
    verifyNetwork(
        Path.of("docs", "net_2024.xml"),
        Path.of("src", "main", "resources", "reserve_sys_logic.json"));
    verifyNetwork(
        Path.of("docs", "net_2025.xml"),
        Path.of("src", "main", "resources", "data_proc_logic.json"));
  }

  private void verifyNetwork(Path pnmlPath, Path jsonPath) throws Exception {
    assertTrueExists(pnmlPath);
    assertTrueExists(jsonPath);

    ParsedPnml parsedPnml = parsePnml(pnmlPath);
    SimulationConfig config = ConfigLoader.load(jsonPath.toString(), SimulationConfig.class);

    int[][] expectedPre = config.net().pre();
    assertEquals(expectedPre.length, parsedPnml.pre.length, "place count");
    assertEquals(expectedPre[0].length, parsedPnml.pre[0].length, "transition count");
    assertMatrixEquals(expectedPre, parsedPnml.pre, "pre matrix");

    int[][] expectedPost = config.net().post();
    assertMatrixEquals(expectedPost, parsedPnml.post, "post matrix");

    int[] expectedInitial = config.net().initialMarking();
    assertArrayEquals(expectedInitial, parsedPnml.initialMarking, "initial marking");
  }

  private ParsedPnml parsePnml(Path pnmlPath)
      throws IOException, ParserConfigurationException, SAXException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(false);
    factory.setExpandEntityReferences(false);
    factory.setXIncludeAware(false);

    Document document;
    try (var inputStream = Files.newInputStream(pnmlPath)) {
      document = factory.newDocumentBuilder().parse(inputStream);
    }
    document.getDocumentElement().normalize();

    Map<String, Integer> placeIndexById = new HashMap<>();
    Map<String, Integer> transitionIndexById = new HashMap<>();

    NodeList placeNodes = document.getElementsByTagName("place");
    int placeCount = placeNodes.getLength();
    int transitionCount = document.getElementsByTagName("transition").getLength();

    int[][] pre = new int[placeCount][transitionCount];
    int[][] post = new int[placeCount][transitionCount];
    int[] initialMarking = new int[placeCount];

    for (int i = 0; i < placeNodes.getLength(); i++) {
      Element place = (Element) placeNodes.item(i);
      int placeIndex = parseNumericSuffix(place.getAttribute("id"), "place id");
      placeIndexById.put(place.getAttribute("id"), placeIndex);
      initialMarking[placeIndex] = parseInitialMarking(place);
    }

    NodeList transitionNodes = document.getElementsByTagName("transition");
    for (int i = 0; i < transitionNodes.getLength(); i++) {
      Element transition = (Element) transitionNodes.item(i);
      int transitionIndex = parseNumericSuffix(transition.getAttribute("id"), "transition id");
      transitionIndexById.put(transition.getAttribute("id"), transitionIndex);
    }

    NodeList arcNodes = document.getElementsByTagName("arc");
    for (int i = 0; i < arcNodes.getLength(); i++) {
      Element arc = (Element) arcNodes.item(i);
      String source = arc.getAttribute("source");
      String target = arc.getAttribute("target");
      int weight = parseArcWeight(arc);

      if (placeIndexById.containsKey(source) && transitionIndexById.containsKey(target)) {
        pre[placeIndexById.get(source)][transitionIndexById.get(target)] = weight;
      } else if (transitionIndexById.containsKey(source) && placeIndexById.containsKey(target)) {
        post[placeIndexById.get(target)][transitionIndexById.get(source)] = weight;
      } else {
        throw new IllegalArgumentException(
            "Unexpected arc endpoints in " + pnmlPathForError(pnmlPath, source, target));
      }
    }

    return new ParsedPnml(pre, post, initialMarking);
  }

  private int parseInitialMarking(Element place) {
    NodeList initialMarkingNodes = place.getElementsByTagName("initialMarking");
    if (initialMarkingNodes.getLength() != 1) {
      throw new IllegalArgumentException("Each place must have exactly one initialMarking block");
    }

    Element initialMarking = (Element) initialMarkingNodes.item(0);
    NodeList valueNodes = initialMarking.getElementsByTagName("value");
    if (valueNodes.getLength() != 1) {
      throw new IllegalArgumentException("Each initialMarking must contain one value block");
    }

    String rawValue = valueNodes.item(0).getTextContent().trim();
    return parseNumericSuffix(rawValue, "initial marking");
  }

  private int parseArcWeight(Element arc) {
    NodeList inscriptionNodes = arc.getElementsByTagName("inscription");
    if (inscriptionNodes.getLength() != 1) {
      return 1;
    }

    NodeList valueNodes = ((Element) inscriptionNodes.item(0)).getElementsByTagName("value");
    if (valueNodes.getLength() != 1) {
      return 1;
    }

    String rawValue = valueNodes.item(0).getTextContent().trim();
    return parseNumericSuffix(rawValue, "arc weight");
  }

  private int parseNumericSuffix(String rawValue, String fieldName) {
    Matcher matcher = NUMERIC_SUFFIX.matcher(rawValue);
    if (!matcher.find()) {
      throw new IllegalArgumentException(
          "Cannot parse numeric value from " + fieldName + ": " + rawValue);
    }
    return Integer.parseInt(matcher.group(1));
  }

  private void assertMatrixEquals(int[][] expected, int[][] actual, String label) {
    assertEquals(expected.length, actual.length, label + " rows");
    for (int row = 0; row < expected.length; row++) {
      assertArrayEquals(expected[row], actual[row], label + " row " + row);
    }
  }

  private void assertTrueExists(Path path) {
    if (!Files.exists(path)) {
      throw new IllegalArgumentException("Missing required file: " + path);
    }
  }

  private String pnmlPathForError(Path pnmlPath, String source, String target) {
    return pnmlPath + " (source=" + source + ", target=" + target + ")";
  }

  private record ParsedPnml(int[][] pre, int[][] post, int[] initialMarking) {}
}
