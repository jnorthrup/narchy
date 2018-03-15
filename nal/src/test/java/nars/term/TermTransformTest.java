package nars.term;

import nars.$;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TermTransformTest {

    @Test
    public void testReplaceTemporalCorrectly() {
        Term x = $.$safe("((((_1,_2)&|(_1,_3)) &&+2 ((_1,_2)&|(_1,_3))) ==>+2 ((_1,_2)&|(_1,_3)))");
        Term y = x.replace($.$safe("((_1,_2)&|(_1,_3))"), $.varDep(1));
        assertEquals("((#1 &&+2 #1) ==>+2 #1)", y.toString());
    }
}
