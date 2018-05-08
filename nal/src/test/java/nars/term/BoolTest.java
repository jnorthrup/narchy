package nars.term;

import nars.$;
import nars.Op;
import nars.truth.Truth;
import nars.truth.TruthFunctions;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.Op.*;
import static nars.term.TermReductionsTest.assertReduction;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Bool and Tautology tests */
public class BoolTest {


    @Test
    public void testNegationTautologies() {
        assertEquals(True, True.unneg());
        assertEquals(False, True.neg());
        assertEquals(True, False.unneg());
        assertEquals(True, False.neg());
        assertEquals(Null, Null.neg());
        assertEquals(Null, Null.unneg());
    }

    @Test public void testStatementTautologies()  {
        for (Op o : new Op[]{INH, SIM, IMPL}) {
            assertEquals(True, o.the(True, True));
            assertEquals(True, o.the(False, False));
            assertEquals(Null, o.the(Null, Null));
        }

        assertEquals("(x-->†)", INH.the(x, True).toString()); //??
        assertEquals(Null, INH.the(True, x));
        assertEquals("((--,x)-->†)", INH.the(x.neg(), True).toString());
    }


//    @Test public void testInheritanceTaskReduction() throws Narsese.NarseseException {
//        {
//            NAR n = NARS.shell(); //HACK separate NAR to prevent revision
//            //HACK using "true:true" to produce True, since i forget if True/False has a parse
//            Task aIsTrue = n.inputTask("(a-->true:true).");
//            assertEquals("$.50 a. %1.0;.90%", aIsTrue.toString());
//        }
//
//        {
//            NAR n = NARS.shell(); //HACK separate NAR to prevent revision
//            Task aIsFalse = n.inputTask("(a --> --(true-->true)).");
//            assertEquals("$.50 a. %0.0;.90%", aIsFalse.toString());
//        }
//
//        {
//            NAR n = NARS.shell(); //HACK separate NAR to prevent revision
//            Task notAIsFalse = n.inputTask("(--a --> --(true-->true)).");
//            assertEquals("$.50 a. %1.0;.90%", notAIsFalse.toString());
//        }
//
//        {
//            NAR n = NARS.shell(); //HACK separate NAR to prevent revision
//            Task notAIsntFalse = n.inputTask("--(--a --> --(true-->true)).");
//            assertEquals("$.50 a. %0.0;.90%", notAIsntFalse.toString());
//        }
//    }

    @Test
    public void testImplicationTautologies() {
        assertEquals("x", IMPL.the(True, x).toString());
        assertEquals("(--,x)", IMPL.the(False, x).toString());
        assertEquals(Null, IMPL.the(Null, x));
        assertEquals(x, IMPL.the(x, True));
        assertEquals(x.neg(), IMPL.the(x, False));
//        assertEquals(Null, IMPL.the(x, True));
//        assertEquals(Null, IMPL.the(x, False));
        assertEquals(Null, IMPL.the(x, Null));
    }

    @Test
    public void testConjTautologies() {
        assertEquals("x", CONJ.the(True, x).toString());
        assertEquals(False, CONJ.the(False, x));
        assertEquals(False, CONJ.the(False, True));
        assertEquals(True, CONJ.the(True, True));
        assertEquals(False, CONJ.the(False, False));
        assertEquals(Null, CONJ.the(Null, x));
        assertEquals(Null, CONJ.the(Null, Null));
    }


    @Test
    public void testDiffTautologies() {

        @Nullable Truth selfDiff = TruthFunctions.difference($.t(1, 0.9f), $.t(1f, 0.9f), 0);
        assertEquals($.t(0, 0.81f), selfDiff);

        @Nullable Truth negDiff = TruthFunctions.difference($.t(0, 0.9f), $.t(1f, 0.9f), 0);
        assertEquals($.t(0, 0.81f), negDiff);

        @Nullable Truth posDiff = TruthFunctions.difference($.t(1, 0.9f), $.t(0f, 0.9f), 0);
        assertEquals($.t(1, 0.81f), posDiff);

        //@Nullable Truth semiDiff = TruthFunctions.difference($.t(0.75f, 0.9f), $.t(0.25f, 0.9f), 0);


        for (Op o : new Op[] { DIFFe, DIFFi } ) {

            String diff = o.str;

            //raw
            assertReduction(False, "(x" + diff + "x)");
            assertReduction(
                    //"(x" + diff + "(--,x))",
                    True,
                    "(x" + diff + "(--,x))");  //unchanged
            assertReduction(
                    //"(x" + diff + "(--,x))",
                    False,
                    "((--,x)" + diff + "x)");  //unchanged

            //subj
            assertReduction(Null, "((x" + diff + "x)-->y)");
            assertReduction(Null, "(--(x" + diff + "x)-->y)");
//            assertReduction("((x" + diff + "(--,x))-->y)", "((x" + diff + "(--,x))-->y)"); //unchanged
//            assertReduction("(((--,x)" + diff + "x)-->y)", "(((--,x)" + diff + "x)-->y)"); //unchanged

            //pred
            assertReduction("(y-->Ⅎ)",  "(y --> (x" + diff + "x))");
            assertReduction("(y-->†)",  "(y --> --(x" + diff + "x))");
//            assertReduction("(y-->(x" + diff + "(--,x)))", "(y-->(x" + diff + "(--,x)))"); //unchanged
//            assertReduction("(y-->((--,x)" + diff + "x))", "(y-->((--,x)" + diff + "x))"); //unchanged



            assertEquals(False, o.the(x,x));
            assertEquals(True, o.the(x,x.neg()));
            assertEquals(False, o.the(x.neg(),x));

            assertEquals(Null, o.the(x,False));
            assertEquals(Null, o.the(x,True));


            assertEquals(False, o.the(True,True));
            assertEquals(False, o.the(False,False));
            assertEquals(Null, o.the(Null,Null));

            assertEquals(True, o.the(True,False));
            assertEquals(False, o.the(False,True));


        }
    }

    @Test
    public void testDiffOfIntersectionsWithCommonSubterms() {
        //diff of intersection, approx:
        // a*x - b*x
        //    = x * (a-b)
        // ex: (c --> (a & b)), (Belief:Intersection)
        //     (c --> ((a & x)-(b & x)))
        //        = (c --> ((a-b)&x))

        assertReduction("(c-->((a-b)&x))", $$("(c --> ((a & x)-(b & x)))"));
        assertReduction("(((a~b)|x)-->c)", $$("(((a | x)~(b | x)) --> c)"));

        //completely contained by the other
        assertEquals(Null, $$("((&,x,a)-(&,x,a,b))"));
        assertEquals(Null, $$("((&,x,a,b)-(&,x,a))"));
        assertEquals(Null, $$("((&,x,a)-(&,x,a,b))"));
    }

    @Test
    public void testDiffOfUnionsWithCommonSubterms() {

        //diff of union, approx:
            // (1-(1-a)*(1-x)) - (1-(1-b)*(1-x))
            //    = (1 - x) * (a - b)

        assertReduction("(c-->((a-b)|(--,x)))", $$("(c --> ((a | x)-(b | x)))"));
        assertReduction("(((a~b)&(--,x))-->c)", $$("(((a & x)~(b & x)) --> c)"));
    }


    @Disabled
    @Test
    public void testIntersectionOfDiffsWithCommonSubterms() {

        //these dont seem to reduce any better

        //intersection
        // (a-x)*(c-x) =
        // (x-a)*(x-c) =
        // (a-x)*(x-c) =
        // (x-a)*(c-x) =

        //union
        // 1-(1-(a-x))*(1-(c-x)) =
        //...
    }

    @Test
    public void testIntersectionTautologies() {
        for (Op o : new Op[] { SECTe, SECTi } ) {

            String sect = o.str;

            //raw
            assertEquals(x, o.the(x, x));
            assertReduction("((--,x)" + sect + "x)", o.the(x, x.neg())); //unchanged

            assertEquals(x, o.the(x, True));
            assertEquals(Null /* False ?  */, o.the(x, False));
            assertEquals(Null, o.the(x, Null));
        }
    }

    @Test
    public void testSetTautologies() {
        //TODO
    }

    static final Term x = $$("x");
    static final Term y = $$("y");

}
