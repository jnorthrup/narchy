package nars.term;

import jcog.data.list.FasterList;
import nars.$;
import nars.Narsese;
import nars.Op;
import nars.term.atom.IdempotentBool;
import nars.term.util.conj.Conj;
import nars.term.util.conj.ConjBuilder;
import nars.term.util.conj.ConjTree;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static nars.$.*;
import static nars.Op.CONJ;
import static nars.term.atom.IdempotentBool.False;
import static nars.term.atom.IdempotentBool.True;
import static nars.term.util.TermTest.assertEq;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.ETERNAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class ConjTest2 {
    static final Term x = $.INSTANCE.the("x");
    static final Term y = $.INSTANCE.the("y");
    static final Term z = $.INSTANCE.the("z");
    static final Term a = $.INSTANCE.the("a");
    static final Term b = $.INSTANCE.the("b");

    static void assertConjDiff(String inc, String exc, String expect, boolean excludeNeg) {
        Term exclude = INSTANCE.$$(exc);
        Term x = Conj.diffAll(INSTANCE.$$(inc), exclude);
        if (excludeNeg)
            x = Conj.diffAll(x, exclude.neg());
        assertEq(expect, x);
    }

    public static Compound $$c(String s) {
        return ((Compound) INSTANCE.$$(s));
    }

    @Deprecated
    static Term conj(FasterList<LongObjectPair<Term>> events) {
        int eventsSize = events.size();
        switch (eventsSize) {
            case 0:
                return True;
            case 1:
                return events.get(0).getTwo();
        }

        ConjBuilder ce = new ConjTree();

        for (LongObjectPair<Term> o : events) {
            if (!ce.add(o.getOne(), o.getTwo())) {
                break;
            }
        }

        return ce.term();
    }


    @Test
    void testSequenceInEternal() {
        assertEq("((#NIGHT &&+1 #DAY1)&&(#DAY2 &&+1 #NIGHT))",
                CONJ.the(DTERNAL,
                        INSTANCE.$$$("(#NIGHT &&+1 #DAY1)"),
                        INSTANCE.$$$("(#DAY2 &&+1 #NIGHT)")
                ));
    }


    @Test
    void testMergedDisjunction1() {
        assertFalse(Conj.eventOf(INSTANCE.$$("(x &&+1 y)"), INSTANCE.$$("(x&&y)")));

        //simplest case: merge one of the sequence
        Term a = INSTANCE.$$("(x &&+1 y)");
        Term b = INSTANCE.$$("(x && y)");
        assertEq(
                "((--,((--,y) &&+1 (--,y)))&&x)",
                Op.DISJ(a, b)
        );
    }

    @Test
    void testMergedDisjunction2() {
        //simplest case: merge one of the sequence
        Term a = INSTANCE.$$("(x &&+10 y)");
        Term b = INSTANCE.$$("(x &&-10 y)");
        assertEq(
                "((--,((--,y) &&+20 (--,y))) &&+10 x)",
                Op.DISJ(a, b)
        );
    }


    @Test
    void testWrappingCommutiveConjunctionX_3() {

        Term xAndRedundantParallel = INSTANCE.$$("(((x &| y) &| z)&&x)");
        assertEquals("(&&,x,y,z)",
                xAndRedundantParallel.toString());


        Term xAndContradictParallel = INSTANCE.$$("(((x &| y) &| z)&&--x)");
        assertEquals(False,
                xAndContradictParallel);


        Term xAndContradictParallelMultiple = INSTANCE.$$("(&&,x,y,((x &| y) &| z))");
        assertEquals("(&&,x,y,z)",
                xAndContradictParallelMultiple.toString());


        Term xAndContradict2 = INSTANCE.$$("((((--,angX) &&+4 x) &&+10244 angX) && --x)");
        assertEquals(False, xAndContradict2);


        Term xAndContradict3 = INSTANCE.$$("((((--,angX) &&+4 x) &&+10244 angX) && angX)");
        assertEquals(False, xAndContradict3);


        Term xParallel = INSTANCE.$$("((((--,angX) &&+4 x) &&+10244 angX) &| y)");
        assertEquals(False, xParallel);


        Term xParallelContradiction4 = INSTANCE.$$("((((--,angX) &&+4 x) &&+10244 angX) &| angX)");
        assertEquals(False, xParallelContradiction4);


        Term x = INSTANCE.$$("((((--,angX) &&+4 x) &&+10244 angX) &| angX)");
        Term y = INSTANCE.$$("(angX &| (((--,angX) &&+4 x) &&+10244 angX))");
        assertEquals(x, y);

    }


    @Test
    public void testXternalInDternal() {
        assertEq(//"((a&&x) &&+- (a&&y))",
                "((x &&+- y)&&a)",
                "((x &&+- y) && a)");
    }

    @Test
    public void testXternalInSequence() {
        assertEq("((x &&+- y) &&+1 a)", "((x &&+- y) &&+1  a)");
        assertEq("(a &&+1 (x &&+- y))", "(a &&+1 (x &&+- y))");
        assertEq("((x &&+- y)&&a)", "((x &&+- y) &|  a)");

        assertEquals(0, INSTANCE.$$("(x &&+- y)").eventRange());
        assertEquals(0, INSTANCE.$$("((y &&+2 z) &&+- x)").eventRange());

        assertEq("(((y &&+2 z) &&+- x) &&+1 a)", "((x &&+- (y &&+2 z)) &&+1  a)");
        assertEq("(a &&+1 ((y &&+2 z) &&+- x))", "(a &&+1 ((y &&+2 z) &&+- x))");
        assertEq("(((w&&x) &&+- (y &&+2 z)) &&+1 a)", "(((x&&w) &&+- (y &&+2 z)) &&+1 a)");

    }


    @Test
    void testConjSeqWtf() {
        Term t = CONJ.the(606,
                INSTANCE.$$$("((tetris(#1,13) &&+42 (tetris(#1,13)&&(tetris-->#2)))&&(--,tetris(#3,#_f)))"),
                INSTANCE.$$$("(tetris-->#2)"));
        assertEq("(((tetris(#1,13) &&+42 (tetris(#1,13)&&(tetris-->#2)))&&(--,tetris(#3,#_f))) &&+606 (tetris-->#2))",
                t);

        Term u = t.anon();
        assertEq("(((_2(#1,_1) &&+42 (_2(#1,_1)&&(_2-->#2)))&&(--,_2(#3,#_f))) &&+606 (_2-->#2))", u);
        assertEquals(t.volume(), u.volume());

    }


    @Test
    void testDisjunctionInnerDTERNALConj2_simple() {

        assertEq("(x&&y)", "(x && (y||--x))");
        assertEq("(x&&y)", "(x && (y||(--x &&+1 --x)))");

        assertEq("(x&&y)", "(x && (y||--(x &&+1 x)))");

        assertEq("((--,y)&&(--,z))", Conj.diffAll(INSTANCE.$$("((x||(--,z))&&(--,y))"), INSTANCE.$$("x")));
        assertEq("((y||z)&&x)", "(x && (y||(--x && z)))"); //and(x, or(y,and(not(x), z)))) = x & y

    }

    @Test
    void testDisjunctionInnerDTERNALConj2() {
        Term x = INSTANCE.$$("((x &&+1 --x) && --y)");
        Term xn = x.neg();
        assertEq("((--,(x &&+1 (--,x)))||y)", xn);

        assertEq(
                //"((||,(--,(x &&+1 (--,x)) ),y)&&x)",
                //"x",
                "(x&&y)",
                CONJ.the(xn, INSTANCE.$$("x"))
        );

    }


    @Test
    void testAnotherComplexInvalidConj() {
        String a0 = "(((--,((--,_2(_1)) &&+710 (--,_2(_1))))&&(_3-->_1)) &&+710 _2(_1))";
        Term a = INSTANCE.$$(a0);
        assertEq(a0, a);
        Term b = INSTANCE.$$("(--,(_2(_1)&&(_3-->_1)))");
        assertEq("((_3-->_1) &&+710 _2(_1))", CONJ.the(0, a, b));

    }


    @Test
    void disjunctionSequence_vs_Eternal_Cancellation() {

        for (long t : new long[]{0, 1, ETERNAL}) {
            ConjBuilder c = new ConjTree();
            c.add(t, INSTANCE.$$("--(x &&+50 x)"));
            c.add(t, INSTANCE.$$("x"));
            Term cc = c.term();
            assertEquals(t == ETERNAL ? False : INSTANCE.$$("(x &&+50 (--,x))"), cc, new Supplier<String>() {
                @Override
                public String get() {
                    return t + " = " + cc;
                }
            });
        }
    }

    @Test
    void xternal_disjunctionSequence_Reduce() {
        ConjBuilder c = new ConjTree();
        c.add(ETERNAL, INSTANCE.$$("--(x &&+- y)"));
        c.add(ETERNAL, INSTANCE.$$("x"));
        assertEq("((--,y)&&x)", c.term());
    }

    @Test
    void disjunctionSequence_vs_Eternal_Cancellation_mix() {
        {
            //disj first:
            ConjBuilder c = new ConjTree();
            c.add(1, INSTANCE.$$("--(x &&+50 x)"));
            c.add(ETERNAL, INSTANCE.$$("x"));
            assertEq(False, c.term());
        }
        {
            //eternal first:
            ConjBuilder c = new ConjTree();
            c.add(ETERNAL, INSTANCE.$$("x"));
            c.add(1, INSTANCE.$$("--(x &&+50 x)"));
            assertEq(False, c.term());
        }


    }


    @Test
    void testSequentialDisjunctionAbsorb3() {

        Term t = CONJ.the(0,
                INSTANCE.$$("(--,((--,R) &&+600 jump))"),
                INSTANCE.$$("(--,L)"),
                INSTANCE.$$("(--,R)"));
        assertEq(
                "(((--,L)&&(--,R)) &&+600 (--,jump))"
                //"(&&,(--,((--,R) &&+600 jump)),(--,L),(--,R))"
                , t);
    }

    @Test
    void testDisj_Factorize_2b() {
        assertEq("((a&&b)||(c&&d))", "((a&&b)||(c&&d))");
        assertEq("(((a&&b)||(c&&d))&&x)", "(||,(&&,x,a,b),(&&,x,c,d))");
    }

    @Test
    void testDisj5() {
        /* (a and x) or (a and y) or (not (a) and z) =
              (¬a ∨ x ∨ y) ∧ (a ∨ z)
              ((||,x,y,(--,a))&&(a||z))
        * */

        assertEq("((||,x,y,(--,a))&&(a||z))", "(||,(a && x),(a && y), (--a&&z))");

        assertEq("((||,x,y,(--,a))&&(a||z))", "((||,x,y,(--,a))&&(a||z))"); //pre-test
    }


    @Test
    void testDisjuncSeq1() {


        //simple case of common event
        Term c = INSTANCE.$$("(||,(a &&+1 b),(a &&+2 b))");
        assertEq("(a &&+1 (--,((--,b) &&+1 (--,b))))", c);
    }

    @Test
    public void factorizeInProductsTest() {
        /* https://github.com/Horazon1985/ExpressionBuilder/blob/master/test/logicalexpression/computationtests/GeneralLogicalTests.java#L109 */
        //LogicalExpression logExpr = LogicalExpression.build("(a|b)&(a|c)&x&(a|d)");
        //LogicalExpression expectedResult = LogicalExpression.build("(a|b&c&d)&x");
        assertEq("(((&&,b,c,d)||a)&&x)", "((( (a||b) && (a||c)) && x) && (a||d))");
    }




    /**
     * these are experimental cases involving contradictory or redundant events in a conjunction of
     * parallel and dternal sub-conjunctions
     * TODO TO BE DECIDED
     */
//    @Disabled
//    private class ConjReductionsTest {


    @Test
    void testConjParaEteReduction2() throws Narsese.NarseseException {
        String o = "(((--,tetris(isRow,2,true))&|tetris(isRowClear,8,true)) ==>-807 (((--,tetris(isRow,2,true))&&tetris(isRowClear,8,true))&|tetris(isRowClear,8,true)))";
        String q = "(((--,tetris(isRow,2,true))&|tetris(isRowClear,8,true)) ==>-807 ((--,tetris(isRow,2,true))&|tetris(isRowClear,8,true)))";
        Term oo = INSTANCE.$(o);
        assertEquals(q, oo.toString());
    }


    @Test
    void testConjNearIdentity() {
        assertEq(IdempotentBool.True, "( (a&&b) ==> (a&|b) )");

        assertEq(
                "((X,x)&|#1)",
                "( ((X,x)&&#1) &| ((X,x)&|#1) )");

        assertEq("((--,((X,x)&&#1))&|(--,((X,x)&|#1)))", "( (--,((X,x)&&#1)) &| (--,((X,x)&|#1)) )");
    }


}
