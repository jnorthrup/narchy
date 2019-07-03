package nars.op;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static nars.term.atom.Bool.*;

public class EqualTest {
    /**
     * same tautological assumptions should hold in equal(x,y) results
     */
    @Test
    void testEqualOperatorTautologies() {
        //TODO finish
//        NAR n = NARS.shell();
        Assertions.assertEquals(True, Equal.the(True, True));
        Assertions.assertEquals(False, Equal.the(True, False));
        Assertions.assertEquals(Null, Equal.the(True, Null));
        Assertions.assertEquals(Null, Equal.the(False, Null));
//        assertEq("(y-->x)", Equal.the($$("x:y"), True));
//        assertEq("(--,(y-->x))", Equal.the($$("x:y"), False));

//        assertEquals("[equal(true,true)]", Evaluation.eval($$("equal(true,true)"), n).toString());
//        assertEquals("[equal(false,false)]", Evaluation.eval($$("equal(false,false)"), n).toString());
        //assertEquals("[null]", Evaluation.eval($$("equal(null,null)"), n).toString());
    }
}
