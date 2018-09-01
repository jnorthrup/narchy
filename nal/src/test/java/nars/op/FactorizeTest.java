package nars.op;

import nars.term.Term;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.Op.CONJ;
import static nars.op.Factorize.applyAndNormalize;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class FactorizeTest {

    @Test
    void testDouble() {
        assertEquals(
                $$("(f(#1) && member(#1,{a,b}))"),
                applyAndNormalize($$("(f(a) && f(b))"))
        );
    }
    @Test
    void testTriple() {
        assertEquals(
                $$("(f(#1) && member(#1,{a,b,c}))"),
                applyAndNormalize($$("(&&, f(a), f(b), f(c))"))
        );
    }
    @Test
    void testWithSomeNonInvolved() {
        assertEquals(
                $$("(&&, g, f(#1), member(#1,{a,b}))"),
                applyAndNormalize($$("(&&, f(a), f(b), g)"))
        );
    }

    @Test
    void test2() {
        assertEquals(
                $$("(f(x,#1) && member(#1,{a,b}))"),
                applyAndNormalize($$("(f(x,a) && f(x,b))"))
        );
    }

    @Test void test3() {
        String s = "(&|,(--,isRow(tetris,(15,true),true)),isRow(tetris,(15,false),true),(--,nextColliding(tetris,true)),nextInBounds(tetris,true))";
        Term t = $$(s);
        assertEquals(
                t, //unchanged
                applyAndNormalize(t)
        );
    }

    @Test
    void testInduction1() {
        assertEquals(
                $$("(f(#1) && member(#1,{a,b,c}))"),
                applyAndNormalize(CONJ.the($$("f(c)"), applyAndNormalize($$("(f(a) && f(b))"))))
        );
    }
}
