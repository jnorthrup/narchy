package nars.truth;

import nars.$;
import org.junit.jupiter.api.Test;

import static nars.truth.TruthFunctions2.differenceX;
import static nars.truth.TruthFunctions2.intersectionX;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** experiments with alternate truth functions */
public class TruthFunctions2Test {


    @Test
    public void testIntersectionX() {
        assertEquals("%1.0;.67%", intersectionX($.t(1f, 0.5f), $.t(1f, 0.5f), 0).toString());
        assertEquals("%.50;.25%", intersectionX($.t(1f, 0.5f), $.t(0f, 0.5f), 0).toString());

        //these 2 should be each other's negations, reflected about f=0.5
        assertEquals(
                intersectionX($.t(1f, 0.5f), $.t(0.75f, 0.5f), 0),
                intersectionX($.t(0f, 0.5f), $.t(0.25f, 0.5f), 0).neg());
        assertEquals("%.88;.56%", intersectionX($.t(1f, 0.5f), $.t(0.75f, 0.5f), 0).toString());
        assertEquals("%.13;.56%", intersectionX($.t(0f, 0.5f), $.t(0.25f, 0.5f), 0).toString());

    }

    @Test
    public void testDifferenceX() {
        assertEquals("%0.0;.67%", differenceX($.t(1f, 0.5f), $.t(1f, 0.5f), 0).toString());
        assertEquals("%.50;.25%", differenceX($.t(1f, 0.5f), $.t(0.5f, 0.5f), 0).toString());
        assertEquals("%1.0;.67%", differenceX($.t(1f, 0.5f), $.t(0f, 0.5f), 0).toString());
        assertEquals("%0.0;.67%", differenceX($.t(0f, 0.5f), $.t(1f, 0.5f), 0).toString());


    }
}
