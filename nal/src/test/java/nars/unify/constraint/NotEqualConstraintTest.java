package nars.unify.constraint;

import nars.term.Term;
import nars.term.Terms;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotEqualConstraintTest {


    @Test
    void testNeqRComConj() {
        //$.16 ((left&&rotate)-->((--,left)&&(--,rotate))). 7930⋈8160 %0.0;.23%
        assertEqRCom("left", "((--,left)&&(--,rotate))");
        assertEqRCom("--left", "((--,left)&&(--,rotate))");
        assertEqRCom("(left&&rotate)", "((--,left)&&(--,rotate))");
        assertEqRCom("(--left&&rotate)", "((--,left)&&(--,rotate))");
        assertEqRCom("(--left && --rotate)", "((--,left)&&(--,rotate))");
    }

    static void assertEqRCom(String a, String b) {
        Term A = $$(a);
        Term B = $$(b);
        assertTrue(Terms.eqRCom(A, B), ()->a + " " + b + " !eqRCom");
        assertTrue(Terms.eqRCom(B, A), ()->b + " " + a + " !eqRCom");
    }

}