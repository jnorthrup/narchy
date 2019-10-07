package nars.unify;

import nars.term.Term;
import org.junit.jupiter.api.Test;

import java.util.TreeSet;

import static nars.$.$$;
import static nars.unify.Unification.Null;
import static org.junit.jupiter.api.Assertions.assertEquals;

class UnificationTest {

    @Test
    void test1() {

        Unification u;
        Unify unify = new UnifyAny();
        if (!unify.unify($$("(#1-->x)"), $$("(a-->x)"), false)) {
            unify.clear();
            u = Null;
        } else {
            u = unify.unification(true);
        }
        //        assertTrue(u.toString().startsWith("unification((#1-->x),(a-->x),○"));

        assertSubst("[(x,a)]", u, "(x,#1)");
        assertSubst("[(a&&x)]", u, "(x && #1)");
    }

    @Test void Permute2() {
        Unification u = new UnifyAny().unification($$("(%1<->%2)"),$$("(a<->b)"), 4);
        assertSubst("[(a,b), (b,a)]", u,
                "(%1,%2)");
    }

    @Test void Permute6() {
        Unification u = new UnifyAny().unification($$("{%1,%2,%3}"),$$("{x,y,z}"), 8);
        assertSubst("[(x,y,z), (x,z,y), (y,x,z), (y,z,x), (z,x,y), (z,y,x)]", u,
                "(%1,%2,%3)");
    }

    static void assertSubst(String expecteds, Unification u, String x) {
        TreeSet ts = new TreeSet();
        Term X = $$(x);
        u.apply(X).forEach(ts::add);
        assertEquals(expecteds, ts.toString());
    }

}