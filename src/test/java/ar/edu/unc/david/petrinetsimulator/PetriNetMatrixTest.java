package ar.edu.unc.david.petrinetsimulator;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for the PetriNetMatrix class. */
public class PetriNetMatrixTest {

  @Nested
  @DisplayName("Constructor")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor throws NullPointerException when pre is null")
    void constructor_throwsNullPointerExceptionWhenPreIsNull() {
      int[][] post = {
        {0, 1},
        {1, 0}
      };
      assertThrows(NullPointerException.class, () -> new PetriNetMatrix(null, post));
    }

    @Test
    @DisplayName("Constructor throws NullPointerException when post is null")
    void constructor_throwsNullPointerExceptionWhenPostIsNull() {
      int[][] pre = {
        {1, 0},
        {0, 1}
      };
      assertThrows(NullPointerException.class, () -> new PetriNetMatrix(pre, null));
    }

    @Test
    @DisplayName("Constructor supports non-square matrices in [p][t] convention")
    void constructor_supportsNonSquareMatrices() {
      int[][] pre = {
        {1, 0},
        {0, 2},
        {3, 1}
      };

      int[][] post = {
        {0, 4},
        {5, 2},
        {3, 0}
      };

      assertDoesNotThrow(() -> new PetriNetMatrix(pre, post));
    }

    @Test
    @DisplayName("Constructor throws IllegalArgumentException when rows are null")
    void constructor_throwsIllegalArgumentExceptionWhenRowsAreNull() {
      int[][] pre = {{1, 0}, null};
      int[][] post = {
        {0, 1},
        {1, 0}
      };
      assertThrows(IllegalArgumentException.class, () -> new PetriNetMatrix(pre, post));
      pre[1] = new int[] {0, 1};
      post[1] = null;
      assertThrows(IllegalArgumentException.class, () -> new PetriNetMatrix(pre, post));
    }

    @Test
    @DisplayName("Constructor throws when pre and post have different number of places")
    void constructor_throwsIllegalArgumentExceptionWhenDifferentPlaceCount() {
      int[][] pre = {
        {1, 0},
        {0, 1}
      };
      int[][] post = {{0, 1}};

      assertThrows(IllegalArgumentException.class, () -> new PetriNetMatrix(pre, post));
    }

    @Test
    @DisplayName("Constructor throws when pre and post have different number of transitions")
    void constructor_throwsIllegalArgumentExceptionWhenDifferentTransitionCount() {
      int[][] pre = {
        {1, 0},
        {0, 1}
      };
      int[][] post = {
        {0, 1, 0},
        {1, 0, 1}
      };

      assertThrows(IllegalArgumentException.class, () -> new PetriNetMatrix(pre, post));
    }

    @Test
    @DisplayName("Constructor throws IllegalArgumentException for empty matrices")
    void constructor_throwsIllegalArgumentExceptionWhenEmptyMatrices() {
      int[][] pre = {};
      int[][] post = {};

      assertThrows(IllegalArgumentException.class, () -> new PetriNetMatrix(pre, post));
      int[][] pre2 = {{}};
      assertThrows(IllegalArgumentException.class, () -> new PetriNetMatrix(pre2, post));
    }

    @Test
    @DisplayName("Constructor throws IllegalArgumentException when there are zero transitions")
    void constructor_throwsIllegalArgumentExceptionWhenZeroTransitions() {
      int[][] pre = {{}};
      int[][] post = {{}};

      assertThrows(IllegalArgumentException.class, () -> new PetriNetMatrix(pre, post));
    }

    @Test
    @DisplayName("Constructor throws IllegalArgumentException for jagged pre matrix")
    void constructor_throwsIllegalArgumentExceptionWhenPreIsJagged() {
      int[][] pre = {
        {1, 0},
        {0}
      };
      int[][] post = {
        {0, 1},
        {1, 0}
      };

      assertThrows(IllegalArgumentException.class, () -> new PetriNetMatrix(pre, post));
    }

    @Test
    @DisplayName("Constructor throws IllegalArgumentException for jagged post matrix")
    void constructor_throwsIllegalArgumentExceptionWhenPostIsJagged() {
      int[][] pre = {
        {1, 0},
        {0, 1}
      };
      int[][] post = {
        {0, 1},
        {1}
      };

      assertThrows(IllegalArgumentException.class, () -> new PetriNetMatrix(pre, post));
    }
  }

  @Nested
  @DisplayName("Dimensions")
  class DimensionsTests {

    @Test
    @DisplayName("numPlaces and numTransitions return expected values")
    void dimensions_returnExpectedValues() {
      int[][] pre = {
        {1, 0, 0},
        {0, 1, 1}
      };
      int[][] post = {
        {0, 1, 0},
        {1, 0, 1}
      };

      PetriNetMatrix matrix = new PetriNetMatrix(pre, post);

      assertEquals(2, matrix.numPlaces());
      assertEquals(3, matrix.numTransitions());
    }
  }

  @Nested
  @DisplayName("preCol")
  class PreColTests {

    @Test
    @DisplayName("preCol returns the transition input column from [p][t] matrix")
    void preCol_returnsExpectedColumn() {
      int[][] pre = {
        {1, 0, 2},
        {0, 1, 3}
      };
      int[][] post = {
        {0, 1, 2},
        {1, 0, 3}
      };

      PetriNetMatrix matrix = new PetriNetMatrix(pre, post);

      assertArrayEquals(new int[] {1, 0}, matrix.preCol(0));
      assertArrayEquals(new int[] {0, 1}, matrix.preCol(1));
      assertArrayEquals(new int[] {2, 3}, matrix.preCol(2));
    }

    @Test
    @DisplayName("preCol throws ArrayIndexOutOfBoundsException for negative index")
    void preCol_throwsOnNegativeIndex() {
      int[][] pre = {
        {1, 0},
        {0, 1}
      };
      int[][] post = {
        {0, 1},
        {1, 0}
      };

      PetriNetMatrix matrix = new PetriNetMatrix(pre, post);

      assertThrows(ArrayIndexOutOfBoundsException.class, () -> matrix.preCol(-1));
    }

    @Test
    @DisplayName("preCol throws ArrayIndexOutOfBoundsException for index equal to numTransitions")
    void preCol_throwsOnIndexEqualToNumTransitions() {
      int[][] pre = {
        {1, 0},
        {0, 1}
      };
      int[][] post = {
        {0, 1},
        {1, 0}
      };

      PetriNetMatrix matrix = new PetriNetMatrix(pre, post);

      assertThrows(
          ArrayIndexOutOfBoundsException.class, () -> matrix.preCol(matrix.numTransitions()));
    }
  }

  @Nested
  @DisplayName("incidenceCol")
  class IncidenceColTests {

    @Test
    @DisplayName("incidenceCol returns post-pre by place for each transition (non-square case)")
    void incidenceCol_returnsExpectedValues_nonSquare() {
      int[][] pre = {
        {1, 0},
        {0, 2},
        {3, 1}
      };
      int[][] post = {
        {0, 4},
        {5, 2},
        {3, 0}
      };

      PetriNetMatrix matrix = new PetriNetMatrix(pre, post);

      assertArrayEquals(new int[] {-1, 5, 0}, matrix.incidenceCol(0));
      assertArrayEquals(new int[] {4, 0, -1}, matrix.incidenceCol(1));
    }

    @Test
    @DisplayName("incidenceCol throws ArrayIndexOutOfBoundsException for invalid index")
    void incidenceCol_throwsOnInvalidIndex() {
      int[][] pre = {
        {1, 0},
        {0, 1}
      };
      int[][] post = {
        {0, 1},
        {1, 0}
      };

      PetriNetMatrix matrix = new PetriNetMatrix(pre, post);

      assertThrows(ArrayIndexOutOfBoundsException.class, () -> matrix.incidenceCol(-1));
      assertThrows(
          ArrayIndexOutOfBoundsException.class, () -> matrix.incidenceCol(matrix.numTransitions()));
    }
  }
}
