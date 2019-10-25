package nars.term;

import nars.term.util.TermTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.$.*;
import static nars.Op.CONJ;
import static nars.term.atom.IdempotentBool.Null;
import static nars.term.util.TermTest.assertEq;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** intersection / diff terms */
public class SectTest {
    @Test
    void testSectCommutivity() {
        TermTest.assertEquivalentTerm("(&,a,b)", "(&,b,a)");
        TermTest.assertEquivalentTerm("(a & b)", "(b & a)"); //test for alternate syntax
        TermTest.assertEquivalentTerm("(|,a,b)", "(|,b,a)");
    }

    @Test
    void testSectWrap() {
        TermTest.assertEquivalentTerm("(|,(b&a),c)", "(|,(b&a),c)");
        TermTest.assertEquivalentTerm("(&,(b|a),c)", "(&,(b|a),c)");
        TermTest.assertEquivalentTerm("(|,a,b,c)", "(|,(b|a),c)");
    }

    @Test void SectDiffEquivAndReductions() {
        TermTest.assertEquivalentTerm("(&,b,--a)", "(b - a)");
        TermTest.assertEquivalentTerm("(|,b,--a)", "(b ~ a)");

        TermTest.assertEquivalentTerm("(&,b,a)", "(b - --a)");

        TermTest.assertEquivalentTerm("--(b~a)", "--(b~a)"); // 1 - (b * (1-a)) hyperbolic paraboloid

        TermTest.assertEquivalentTerm("(&,c, --(b & --a))", "(c - (b-a))");
        TermTest.assertEquivalentTerm("(c ~ (b-a))", "(c ~ (b-a))"); //different types, unchanged

        TermTest.assertEquivalentTerm("((b ~ c) ~ (b ~ a))", "((b ~ c) ~ (b ~ a))"); // (b * (1-c)) * (1-(b * (1-a)))
    }

    //    @Test
//    void testSectConceptualization() {
//
//        assertEq("((a==>b)&x)", "((a==>b) & x)");
//        assertEq("((a ==>+1 b)&x)", "((a==>+1 b) & x)");
//        assertEq("((a ==>+- b)&x)", $$("((a==>+1 b) & x)").concept());
//
//
////        TermTest.assertEq(Bool.Null, "((a==>+1 b) & (a ==>+2 b))");
////        TermTest.assertEq(Bool.Null, "(&, (a==>b),(a ==>+2 b),(c==>d))");
////        TermTest.assertEq("(((a ==>+2 b)-->d)&(a ==>+1 b))", "((a==>+1 b) & ((a ==>+2 b)-->d))");
////        TermTest.assertEq(Bool.Null, "(((a ==> b)-->d) & ((a ==>+2 b)-->d))");
////        TermTest.assertEq(Bool.Null, "(&, (a==>b),(a ==>+2 b),((c==>d)-->e))");
//
//
//    }


    @Test void SectDiff() {
        Term t = CONJ.the((Term) INSTANCE.$$("(--,(?2~(|,(--,(?2~?1)),?2,?3)))"), INSTANCE.$$("?2"), INSTANCE.$$("?3"));
        assertEquals(t, t);
    }

    @Test void factorCommonSectConj() {
        assertEq("(a-->c)", "((a&b)-->(b&c))");
    }
    @Test void recursiveSect() {
        assertEq(Null /*True*/, "(b-->(b&c))");
        assertEq(Null /*False*/, "(b-->(--b&c))");
        assertEq(Null /*True*/, "((a&b)-->a)");
    }
    @Test void factorCommonSectDisj() {
        assertEq("(a-->c)", "((a|b)-->(b|c))");
    }
    @Test void factorCommonSectMix() {
        //TODO
        //??
        //assertEq("(a-->c)", "((a|b)-->(b&c))");
        //assertEq("(a-->c)", "((a&b)-->(b|c))");
    }

    @Test void TooComplexSectDiff() {
//        assertEq("", "(a --> --(x-y))");

        /*
                           x-y  =            x*(1-y)
                        --(x-y) =         1-(x*(1-y))
                  (&,x,--(x-y)) =     x * (1-x*(1-y))
                (&,--y,--(x-y)) = (1-y) * (1-x*(1-y))
         */
        Term n3a = INSTANCE.$$("(&,(--,(x-y)),(--,y),x)");

        Term n3b = INSTANCE.$$("(&, (--,(x-y)), (--,y), x)");
        Term n2 = INSTANCE.$$("(&, (--,(&,(--,(x-y)),(--,y),x)), (--,(x-y)), (--,y), x)");
        Term n1 = INSTANCE.$$("(y-(&,(--,(&,(--,(x-y)),(--,y),x)),(--,(x-y)),(--,y),x))");

        Term n = INSTANCE.$$("(a-->(y-(&,(--,(&,(--,(x-y)),(--,y),x)),(--,(x-y)),(--,y),x)))");

    }

    @Disabled
    @Test void InvalidTemporal1() {
        String a = "(x &&+1 y)";
        String b = "(x &&+2 y)";
        //        assertEq(Null, '(' + a + '|' + b + ')');
//        assertEq(Null, '(' + a + '&' + b + ')');
        assertEq(Null, '{' + a + ',' + b + '}');
        assertEq(Null, '[' + a + ',' + b + ']');
//        assertEq(Null, '(' + a + "<->" + b + ')');

        //one is negated
//        assertEq(Null, '(' + a + "| --" + b + ')');
//        assertEq(Null, '(' + a + "& --" + b + ')');
        assertEq(Null, '{' + a + ",--" + b + '}');
        assertEq(Null, '[' + a + ",--" + b + ']');
//        assertEq(Null, '(' + a + "<-> --" + b + ')');

        //3-ary
//        assertEq(Null, "(|," + a + ',' + b + ',' + c + ')');
//        assertEq(Null, "(&," + a + ',' + b + ',' + c + ')');
        String c = "z";
        assertEq(Null, '{' + a + ',' + b + ',' + c + '}');
        assertEq(Null, '[' + a + ',' + b + ',' + c + ']');

    }


//    @Test
//    void testInvalidTemporal3() {
//        assertEq(Null, "((a==>+1 b)~(a ==>+2 b))");
//        //TermTest.assertEq("((--,(c ==>+2 d))&(a ==>+1 b))", "((X &&+837 Y)~(--,(Y &&+1424 X)))");
//    }
//    @Test void ValidTemporal1() {
//        assertEq("((x &&+1 y)|(x &&+2 z))", "((x &&+1 y)|(x &&+2 z))");
//    }
}
