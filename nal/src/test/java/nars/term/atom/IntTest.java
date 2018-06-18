package nars.term.atom;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterators;
import nars.*;
import nars.term.Term;
import nars.test.TestNAR;
import nars.time.Tense;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Iterator;

import static nars.$.$;
import static nars.Op.Null;
import static nars.Op.SECTi;
import static nars.term.atom.Int.range;
import static nars.term.atom.Int.the;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class IntTest {

    private static Term differenceSafe(/*@NotNull*/ Term a, Term b) {
        Op o = a.op();
        assert (b.op() == o);
        return Op.differenceSet(o, a, b);
    }

    @Disabled
    @Test
    void testVariableIntroduction() throws Narsese.NarseseException {
        NAR n = NARS.shell();
        n.log();
        n.input(" ((3,x) ==>+1 (4,y)).");
        

        n.run(10);
    }

    @Test
    void testIntRange1() throws Narsese.NarseseException {
        Atomic ii = range(0, 2);
        assertEquals("0..2", ii.toString());

        NAR n = NARS.tmp();
        n.log();
        n.believe("(f(1) <-> 5)");
        n.believe($.sim($.func("f", ii), $.varDep(1)));
        n.run(10);
    }

    @Test
    void testDifferRangeInt() {
        assertEquals(range(1,2), differenceSafe(range(0,2), the(0)));
        assertEquals(range(0,2), differenceSafe(range(0,2), the(3))); 
        assertEquals("(0&2)", differenceSafe(range(0,2), the(1)).toString());
    }

    @Test
    void testIntIntersectionReduction() {
        
        

        
        assertEquals(
                range(0, 1),
                Op.SECTi.the(the(0), the(1))
        );
        NAR n = NARS.tmp();
        n.log();
        n.believe($.inh($.the(0), $.the("x")));
        n.believe($.inh($.the(1), $.the("x")));
        n.run(10);
    }








    @Test
    void testIntInProductIntersectionReduction() {

        
        assertEquals(
                
                
                "(0..1,0..2)",
                Op.SECTi.the($.p(0, 1), $.p(the(1), range(0,2))).toString()
        );

        NAR n = NARS.tmp();
        n.log();
        n.believe($.inh($.p(0, 1), $.the("x")));
        n.believe($.inh($.p(the(1), range(0,2)), $.the("x")));
        n.run(10);
    }

    @Test
    void testMultidimUnroll() throws Narsese.NarseseException {
        Term a = SECTi.the($("(1,1)"), $("(1,2)"));
        assertEquals("(1,1..2)", a.toString());
        assertEquals("[(1,1), (1,2)]", unroll(a));
    }

    private static String unroll(Term a) {
        Iterator<Term> unroll = Int.unroll(a);
        assertNotNull(unroll);
        return Arrays.toString(Iterators.toArray(unroll, Term.class));
    }

    @Test
    void testRecursiveUnroll() {
        assertEquals("TODO",
                unroll(
                    
                    $.p(Int.range(0,1), $.the("c"), Int.the(0),
                            $.p(Int.the(2), $.the("b"), Int.range(1,2),
                                    $.p(Int.range(0,1), $.the("a"), Int.the(0))))
        ));
    }

    @Disabled @Test
    void testRangeUnification() {
        TestNAR n = new TestNAR(NARS.tmp());
        
        n.nar.believe(
                $.inh(range(0, 2), $.the("x")),
                Tense.Present
        );
        n.nar.believe(
                $.impl(
                    $.inh(Int.the(1), $.varIndep(1)),
                    $.inh($.varIndep(1), $.the("z"))
                ),
                Tense.Present
        );
        n.mustBelieve(128,"(x-->z)", 1f, 0.81f, 0);
        n.test();
    }

    @Test
    void testInvalidDifference() throws Narsese.NarseseException {
        assertEquals(Null, $("(((happy~(0,0))~(0,0))-->tetris)"));
        assertEquals(Null, $("(((happy-(0,0))-(0,0))-->tetris)"));
    }

    @Test
    void testIntAndNonInts() throws Narsese.NarseseException {
        assertEquals("[1..2]", Arrays.toString(
                Int.intersect($("1"),$("2"))
        ));
        assertEquals("[1, 3]", Arrays.toString(
                Int.intersect($("1"),$("3"))
        ));
        assertEquals("[x, 1..2]", Arrays.toString(
                Int.intersect($("1"),$("2"),$("x"))
        ));
        assertEquals("[x, 8, 5..6, 1..2]", Arrays.toString(
                Int.intersect($("1"),$("2"),$("x"),$("5"),$("6"),$("8"))
        ));

        assertEquals("[(y-->x), (1..2-->x)]", Arrays.toString(
                Int.intersect($("(1-->x)"),$("(2-->x)"),$("(y-->x)"))
        ));
    }

    @Test
    void testIntInttersectProd() {
        assertEquals("[(1,1..2)]", Arrays.toString(
                Int.intersect($.p(1, 1), $.p(1, 2))
        ));
    }
    @Test
    void testIntInttersectProdSplit1() {
        assertEquals("[(1,1..2), (3,3)]", Arrays.toString(
                Int.intersect($.p(1, 1), $.p(1, 2), $.p(3, 3))
        ));
        assertEquals("[(1,1..2), (3,3), x]", Arrays.toString(
                Int.intersect($.p(1, 1), $.p(1, 2), $.p(3, 3), $.the("x"))
        ));
    }
    @Test
    void testIntInttersectProdSplit2() {
        assertEquals("[(1,1..2), (3..4,3)]", Arrays.toString(
                Int.intersect($.p(1,1), $.p(1,2), $.p(3,3), $.p(4,3))
        ));
    }

    @Test
    void testNonRangeableIntersection() throws Narsese.NarseseException {
        Term[] r = Int.intersect(
                $("(isRow,(6,true),true)"), $("(isRow,(7,true),true)"));
        String rangeable = Arrays.toString(r);
        assertEquals("[(isRow,(6..7,true),true)]", rangeable);
        assertEquals("(isRow,(6,true),true),(isRow,(7,true),true)", Joiner.on(',').join(Int.unroll(r[0])));

        String nonrangeable = Arrays.toString(Int.intersect(
                $("(isRow,(6,true),true)"), $("(isRow,(7,false),true)")));
        assertEquals("[(isRow,(6,true),true), (isRow,(7,false),true)]", nonrangeable);

    }


    @Test
    void testIntersectionRange() {
        assertEquals("(8|4..5)", Op.SECTi.the(Int.the(4), Int.the(8), Int.range(4, 5)).toString());
        assertEquals("(8&4..5)", Op.SECTe.the(Int.the(4), Int.the(8), Int.range(4, 5)).toString());
        
    }

}






























































































































































