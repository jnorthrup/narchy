package nars.op;

import nars.term.Term;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.$.*;
import static nars.Op.CONJ;
import static nars.term.util.TermTest.assertEq;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class FactorizeTest {

    public static final Factorize.FactorIntroduction f = new Factorize.FactorIntroduction(); {
        f.volMax = Integer.MAX_VALUE; //HACK
    }


    @Test
    void testConjPar2() {
        assertEquals(
                INSTANCE.$$("(f(#1) && member(#1,{a,b}))"),
                f.applyAndNormalize(INSTANCE.$$("(f(a) && f(b))"))
        );
    }
    @Test
    void testConjPar2_xternal() {
        assertEquals(
            INSTANCE.$$("(f(#1) && member(#1,{a,b}))"),
            f.applyAndNormalize(INSTANCE.$$("(f(a) &&+- f(b))"))
        );
    }
    @Disabled
    @Test
    void testConjPar_in_Seq() {
        assertEquals(
            INSTANCE.$$("((f(#1) && member(#1,{a,b})) &&+1 f(c))"),
            f.applyAndNormalize(INSTANCE.$$("((f(a) && f(b)) &&+1 f(c))"))
        );
    }

    @Test
    void testDisjPar2() {
        assertEquals(
            INSTANCE.$$("--(--f(#1) && member(#1,{a,b}))"),
            f.applyAndNormalize(INSTANCE.$$("(f(a) || f(b))"))
        );
    }

    @Test
    void testConjSeq2() {
        String x = "(f(a) &&+3 f(b))";
        //factoring NOT POSSIBLE
        assertEq(x, f.applyAndNormalize(INSTANCE.$$(x)));
    }

    @Test
    void testTriple() {
        assertEquals(
                INSTANCE.$$("(f(#1) && member(#1,{a,b,c}))"),
                f.applyAndNormalize(INSTANCE.$$("(&&, f(a), f(b), f(c))"))
        );
    }
    @Test
    void testWithSomeNonInvolved() {
        assertEquals(
                INSTANCE.$$("(&&, g, f(#1), member(#1,{a,b}))"),
                f.applyAndNormalize(INSTANCE.$$("(&&, f(a), f(b), g)"))
        );
    }

    @Test
    void testDoubleCommutive() {
        assertEquals(
                INSTANCE.$$("(member(#1,{a,y})&&{x,#1})"),
                f.applyAndNormalize(INSTANCE.$$("({a,x} && {x,y})"))
        );
    }

    @Test
    void test2() {
        assertEquals(
                INSTANCE.$$("(f(x,#1) && member(#1,{a,b}))"),
                f.applyAndNormalize(INSTANCE.$$("(f(x,a) && f(x,b))"))
        );
    }

    @Test void three() {
        String s = "(&|,(--,isRow(tetris,(15,true),true)),isRow(tetris,(15,false),true),(--,nextColliding(tetris,true)),nextInBounds(tetris,true))";
        Term t = INSTANCE.$$(s);
        assertEquals(
                t, //unchanged
                f.applyAndNormalize(t)
        );
    }

    @Test
    void testInduction1() {
        assertEquals(
                INSTANCE.$$("(f(#1) && member(#1,{a,b,c}))"),
                f.applyAndNormalize(CONJ.the((Term) INSTANCE.$$("f(c)"), f.applyAndNormalize(INSTANCE.$$("(f(a) && f(b))"))))
        );
    }
}
