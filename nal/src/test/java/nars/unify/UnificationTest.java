package nars.unify;

import org.junit.jupiter.api.Test;

import java.util.TreeSet;

import static nars.$.$$;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnificationTest {

    @Test
    void test1() {

        Unification u = new UnifyAny().unification($$("(#1-->x)"),$$("(a-->x)"));
//        assertTrue(u.toString().startsWith("unification((#1-->x),(a-->x),○"));

        assertSubst("[(x,a)]", u, "(x,#1)");
        assertSubst("[(a&&x)]", u, "(x && #1)");
    }
    @Test void testPermute() {
        Unification u = new UnifyAny().unification($$("(%1<->%2)"),$$("(a<->b)"), 20);
        assertSubst("[(a,b), (b,a)]", u, "(%1,%2)");
    }

    static void assertSubst(String expecteds, Unification u, String x) {
        TreeSet ts = new TreeSet();
        u.apply($$(x)).forEach(ts::add);
        assertEquals(expecteds, ts.toString());
    }
}