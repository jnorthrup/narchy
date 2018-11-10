package nars.term;

import nars.term.atom.Bool;
import org.junit.jupiter.api.Test;

import static nars.$.$$;

/** intersection / diff terms */
public class SectTest {
    @Test
    void testSectCommutivity() {
        TermTest.assertEquivalentTerm("(&,a,b)", "(&,b,a)");
        TermTest.assertEquivalentTerm("(a & b)", "(b & a)"); //test for alternate syntax
        TermTest.assertEquivalentTerm("(|,a,b)", "(|,b,a)");
    }

    @Test void testSectDiffEquivAndReductions() {
        TermTest.assertEquivalentTerm("(&,b,--a)", "(b ~ a)");
        TermTest.assertEquivalentTerm("(|,b,--a)", "(b - a)");

        TermTest.assertEquivalentTerm("(&,b,a)", "(b ~ --a)");

        TermTest.assertEquivalentTerm("--(b~a)", "--(b~a)"); // 1 - (b * (1-a)) hyperbolic paraboloid

        TermTest.assertEquivalentTerm("(&,c, --(b & --a))", "(c ~ (b~a))");
        TermTest.assertEquivalentTerm("(c ~ (b-a))", "(c ~ (b-a))"); //different types, unchanged

        TermTest.assertEquivalentTerm("((b ~ c) ~ (b ~ a))", "((b ~ c) ~ (b ~ a))"); // (b * (1-c)) * (1-(b * (1-a)))
    }


    @Test
    void testSectConceptualization() {

        TermTest.assertEq("((a==>b)&x)", "((a==>b) & x)");
        TermTest.assertEq("((a ==>+1 b)&x)", "((a==>+1 b) & x)");
        TermTest.assertEq("((a ==>+- b)&x)", $$("((a==>+1 b) & x)").concept());


        TermTest.assertEq(Bool.Null, "((a==>+1 b) & (a ==>+2 b))");
        TermTest.assertEq(Bool.Null, "(&, (a==>b),(a ==>+2 b),(c==>d))");
        TermTest.assertEq("(((a ==>+2 b)-->d)&(a ==>+1 b))", "((a==>+1 b) & ((a ==>+2 b)-->d))");
        TermTest.assertEq(Bool.Null, "(((a ==> b)-->d) & ((a ==>+2 b)-->d))");
        TermTest.assertEq(Bool.Null, "(&, (a==>b),(a ==>+2 b),((c==>d)-->e))");


    }

    @Test
    void testDiffConceptualization() {
        TermTest.assertEq("((--,(a ==>+2 b))&(a ==>+1 b))", "((a==>+1 b)~(a ==>+2 b))");
        TermTest.assertEq("((--,(c ==>+2 d))&(a ==>+1 b))", "((a ==>+1 b)~(c ==>+2 d))");


        //TermTest.assertEq("((--,(c ==>+2 d))&(a ==>+1 b))", "((X &&+837 Y)~(--,(Y &&+1424 X)))");
    }
}
