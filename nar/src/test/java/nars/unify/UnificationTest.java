package nars.unify;

import nars.term.Term;
import org.junit.jupiter.api.Test;

import java.util.TreeSet;

import static nars.$.*;
import static nars.unify.Unification.Null;
import static org.junit.jupiter.api.Assertions.assertEquals;

class UnificationTest {

    @Test
    void test1() {

        Unification u;
        Unify unify = new UnifyAny();
        if (!unify.unify(INSTANCE.$$("(#1-->x)"), INSTANCE.$$("(a-->x)"), false)) {
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
        Unification u = new UnifyAny().unification(INSTANCE.$$("(%1<->%2)"), INSTANCE.$$("(a<->b)"), 4);
        assertSubst("[(a,b), (b,a)]", u,
                "(%1,%2)");
    }

    @Test void Permute6() {
        Unification u = new UnifyAny().unification(INSTANCE.$$("{%1,%2,%3}"), INSTANCE.$$("{x,y,z}"), 8);
        assertSubst("[(x,y,z), (x,z,y), (y,x,z), (y,z,x), (z,x,y), (z,y,x)]", u,
                "(%1,%2,%3)");
    }

    static void assertSubst(String expecteds, Unification u, String x) {
        TreeSet ts = new TreeSet();
        Term X = INSTANCE.$$(x);
        for (Term term : u.apply(X)) {
            ts.add(term);
        }
        assertEquals(expecteds, ts.toString());
    }

}