package nars.unify.constraint;

import nars.term.Term;
import nars.term.Terms;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static nars.$.$$;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotEqualConstraintTest {


    @Test
    void testNeqRComConj() {
        assertEqRCom("left", "((--,left)&&(--,rotate))");
        assertEqRCom("--left", "((--,left)&&(--,rotate))");
        assertEqRCom("(left&&rotate)", "((--,left)&&(--,rotate))");
        assertEqRCom("(--left&&rotate)", "((--,left)&&(--,rotate))");
        assertEqRCom("(--left && --rotate)", "((--,left)&&(--,rotate))");
        assertEqRCom("x", "(x&&y)");
        assertEqRCom("--x", "(--x&&y)");
    }

//    @Test
//    void notNeqRComConj() {
//        assertNotEqRCom("(x && y)", "(&&, x, y, z)");
//    }

    static void assertNotEqRCom(String a, String b) {
        assertEqRCom(a, b, false);
    }
    static void assertEqRCom(String a, String b) {
        assertEqRCom(a, b, true);
    }
    static void assertEqRCom(String a, String b, boolean isTrue) {
        Term A = $$(a);
        Term B = $$(b);
        Supplier<String> msg = () -> a + " " + b + " " + (isTrue ? "!eqRCom" : "eqRCom");
        assertTrue(isTrue==Terms.eqRCom(A, B), msg);
        assertTrue(isTrue==Terms.eqRCom(B, A), msg);
    }

}