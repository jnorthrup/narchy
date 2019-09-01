package nars.op;

import nars.NARS;
import nars.term.Term;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.Op.CONJ;
import static nars.term.util.TermTest.assertEq;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class FactorizeTest {

    public static final Factorize.FactorIntroduction f = new Factorize.FactorIntroduction(NARS.shell());

    @Test
    void testConjPar2() {
        assertEquals(
                $$("(f(#1) && member(#1,{a,b}))"),
                f.applyAndNormalize($$("(f(a) && f(b))"))
        );
    }
    @Test
    void testDisjPar2() {
        assertEquals(
            $$("--(--f(#1) && member(#1,{a,b}))"),
            f.applyAndNormalize($$("(f(a) || f(b))"))
        );
    }

    @Test
    void testConjSeq2() {
        String x = "(f(a) &&+3 f(b))";
        //factoring NOT POSSIBLE
        assertEq(x, f.applyAndNormalize($$(x)));
    }

    @Test
    void testTriple() {
        assertEquals(
                $$("(f(#1) && member(#1,{a,b,c}))"),
                f.applyAndNormalize($$("(&&, f(a), f(b), f(c))"))
        );
    }
    @Test
    void testWithSomeNonInvolved() {
        assertEquals(
                $$("(&&, g, f(#1), member(#1,{a,b}))"),
                f.applyAndNormalize($$("(&&, f(a), f(b), g)"))
        );
    }

    @Test
    void testDoubleCommutive() {
        assertEquals(
                $$("(member(#1,{a,y})&&{x,#1})"),
                f.applyAndNormalize($$("({a,x} && {x,y})"))
        );
    }

    @Test
    void test2() {
        assertEquals(
                $$("(f(x,#1) && member(#1,{a,b}))"),
                f.applyAndNormalize($$("(f(x,a) && f(x,b))"))
        );
    }

    @Test void three() {
        String s = "(&|,(--,isRow(tetris,(15,true),true)),isRow(tetris,(15,false),true),(--,nextColliding(tetris,true)),nextInBounds(tetris,true))";
        Term t = $$(s);
        assertEquals(
                t, //unchanged
                f.applyAndNormalize(t)
        );
    }

    @Test
    void testInduction1() {
        assertEquals(
                $$("(f(#1) && member(#1,{a,b,c}))"),
                f.applyAndNormalize(CONJ.the((Term)$$("f(c)"), f.applyAndNormalize($$("(f(a) && f(b))"))))
        );
    }
}
