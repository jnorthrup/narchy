package nars.term;

import nars.Narsese;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.$.$;
import static nars.$.$$;
import static nars.Op.False;
import static nars.Op.True;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** these are experimental cases involving contradictory or redundant events in a conjunction of
 * parallel and dternal sub-conjunctions
 * TODO TO BE DECIDED */
@Disabled
class ConjReductionsTest {

    @Test
    void testConjParaEteReductionInvalid() {
        assertEquals(False,
                $$("(((--,a)&&b)&|(--,b)))")
        );
    }
    @Test
    void testConjParaEteReductionInvalid2() {
        assertEquals(False,
                $$("(((--,a)&&(--,b))&|b))")
        );
    }
    @Test
    void testConjParaEteReduction2simpler() throws Narsese.NarseseException {
        String o = "(((--,x)&|y) ==>+1 (((--,x)&&y)&|y))";
        String q = "(((--,x)&|y) ==>+1 ((--,x)&|y))";
        Term oo = $(o);
        assertEquals(q, oo.toString());
    }
    @Test
    void testConjParaEteReduction2() throws Narsese.NarseseException {
        String o = "(((--,tetris(isRow,2,true))&|tetris(isRowClear,8,true)) ==>-807 (((--,tetris(isRow,2,true))&&tetris(isRowClear,8,true))&|tetris(isRowClear,8,true)))";
        String q = "(((--,tetris(isRow,2,true))&|tetris(isRowClear,8,true)) ==>-807 ((--,tetris(isRow,2,true))&|tetris(isRowClear,8,true)))";
        Term oo = $(o);
        assertEquals(q, oo.toString());
    }

    @Test
    void testConjParaEteReduction() throws Narsese.NarseseException {
        String o = "(((--,a)&&b)&|b))";
        String q = "((--,a)&|b)";
        Term oo = $(o);
        assertEquals(q, oo.toString());
    }

    @Test
    void testConjEteParaReduction() throws Narsese.NarseseException {
        String o = "(((--,a)&|b)&&b))";
        String q = "((--,a)&|b)";
        Term oo = $(o);
        assertEquals(q, oo.toString());
    }
    @Test
    void testConjParallelOverrideEternal() {

        TermReductionsTest.assertReduction(
                "(a&|b)",
                "( (a&&b) &| (a&|b) )");

    }
    @Test
    void testConjNearIdentity() {
        TermReductionsTest.assertReduction(True, "( (a&&b) ==> (a&|b) )");

        TermReductionsTest.assertReduction(
                "((X,x)&|#1)",
                "( ((X,x)&&#1) &| ((X,x)&|#1) )");

        TermReductionsTest.assertReduction("((--,((X,x)&&#1))&|(--,((X,x)&|#1)))", "( (--,((X,x)&&#1)) &| (--,((X,x)&|#1)) )");
    }

}
