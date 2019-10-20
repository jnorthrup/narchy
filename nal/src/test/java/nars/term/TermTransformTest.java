package nars.term;

import nars.$;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.term.atom.IdempotentBool.True;
import static nars.term.util.TermTest.assertEq;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TermTransformTest {

    @Test
    void testReplaceTemporalCorrectly() {
        assertEq("(((_1,_2)&&(_1,_3)) &&+2 ((_1,_2)&&(_1,_3)))",
                $$("(((_1,_2)&&(_1,_3)) &&+2 ((_1,_2)&&(_1,_3)))"));

        Term x = $$("((((_1,_2)&&(_1,_3)) &&+2 ((_1,_2)&&(_1,_3))) ==>+2 ((_1,_2)&&(_1,_3)))");
        Term y = x.replace($$("((_1,_2)&&(_1,_3))"), $.varDep(1));
        assertEquals("((#1 &&+2 #1) ==>+2 #1)", y.toString());
    }

    @Test void testSimReplaceCollapse() {
        assertEq(True, $$("(x<->y)").replace($$("x"), $$("y")));
    }
}
