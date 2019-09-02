package nars.truth;

import nars.$;
import nars.truth.func.TruthFunctions2;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** experiments with alternate truth functions */
class TruthFunctions2Test {

	@Test
	void testUnion() {
		assertEquals($.t(1f, 0.81f), TruthFunctions2.union($.t(1,0.9f), $.t(1, 0.9f), 0));
		assertEquals($.t(0.88f, 0.58f), TruthFunctions2.union($.t(0.75f,0.9f), $.t(0.5f, 0.9f), 0));
		assertNull(TruthFunctions2.union($.t(1f,0.9f), $.t(0f, 0.9f), 0)); //full frequency distortion
	}

//    @Test
//    void testIntersectionX() {
//        assertEquals("%1.0;.67%", intersectionX($.t(1f, 0.5f), $.t(1f, 0.5f), 0).toString());
//        assertEquals("%.50;.25%", intersectionX($.t(1f, 0.5f), $.t(0f, 0.5f), 0).toString());
//
//
//        assertEquals(
//                intersectionX($.t(1f, 0.5f), $.t(0.75f, 0.5f), 0),
//                intersectionX($.t(0f, 0.5f), $.t(0.25f, 0.5f), 0).neg());
//        assertEquals("%.88;.56%", intersectionX($.t(1f, 0.5f), $.t(0.75f, 0.5f), 0).toString());
//        assertEquals("%.13;.56%", intersectionX($.t(0f, 0.5f), $.t(0.25f, 0.5f), 0).toString());
//
//    }

//    @Test
//    void testDifferenceX() {
//        assertEquals("%0.0;.67%", differenceX($.t(1f, 0.5f), $.t(1f, 0.5f), 0).toString());
//        assertEquals("%.50;.25%", differenceX($.t(1f, 0.5f), $.t(0.5f, 0.5f), 0).toString());
//        assertEquals("%1.0;.67%", differenceX($.t(1f, 0.5f), $.t(0f, 0.5f), 0).toString());
//        assertEquals("%0.0;.67%", differenceX($.t(0f, 0.5f), $.t(1f, 0.5f), 0).toString());
//
//
//    }
}
