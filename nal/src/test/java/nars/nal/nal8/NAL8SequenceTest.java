package nars.nal.nal8;

import jcog.data.list.FasterList;
import nars.$;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.term.Term;
import nars.test.NALTest;
import nars.time.Tense;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

import static nars.$.$$;

/** test precision of sequence execution (planning) */
public class NAL8SequenceTest extends NALTest {

    public static final int cycles = 50;

    @Test
    void testSubSequenceOutcome() {
        test.confTolerance(0.01f);
        test
                .input( "(x &&+1 y)!")
                .input( "(z &&+1 (x &&+1 y)).")
                .mustGoal(cycles, "z", 1, 0.81f) //81% for one step
        ;
    }

    @Test
    void testMidSequenceOutcome() {
        test.confTolerance(0.01f);
        test
                .input( "c!")
                .input( "(a &&+1 (b &&+1 (c &&+1 (d &&+1 e)))).")
                .mustGoal(cycles, "(a &&+1 b)", 1, 0.81f) //81% for one step
        ;
    }
    @Test
    void testMidSequenceDTernalComponentOutcome() {
        test.confTolerance(0.01f);
        test
                .input( "c!")
                .input( "(a &&+1 (b &&+1 ((c&|f) &&+1 (d &&+1 e)))).")
                .mustGoal(cycles, "(a &&+1 (b &&+1 f))", 1, 0.81f) //81% for one step
        ;
    }

    @Test
    void testMidSequenceDTernalComponentWithUnification() {
        test.confTolerance(0.01f);
        test
                .input( "c(x)!")
                .input( "(a &&+1 (b(#1) &&+1 ((&|,a,b,c(#1),d(x,y)) &&+1 (d &&+1 e)))).")
                .mustGoal(cycles, "(a &&+1 (b(x) &&+1 (&|,a,b,d(x,y))))", 1, 0.81f) //81% for one step
        ;
    }
    @Test
    void testMidSequenceDTernalComponentOutcome_Alternate_Sort() {
        test.confTolerance(0.01f);
        test
                .input( "f!")
                .input( "(a &&+1 (b &&+1 ((c&|f) &&+1 (d &&+1 e)))).")
                .mustGoal(cycles, "(a &&+1 (b &&+1 c))", 1, 0.81f) //81% for one step
        ;
    }

    @Test
    void testNegMidSequenceOutcome() {
        test.confTolerance(0.01f);
        test
                .input( "--c!")
                .input( "(a &&+1 (b &&+1 (--c &&+1 (d &&+1 e)))).")
                .mustGoal(cycles, "(a &&+1 b)", 1, 0.81f) //81% for one step
        ;
    }
    @Test
    void testEqualConclusionSequenceOutcome() {
        test.confTolerance(0.01f);
        test
                .input( "e!")
                .input( "(a &&+1 (b &&+1 (c &&+1 (d &&+1 e)))).")
                .mustGoal(cycles, "(a &&+1 (b &&+1 (c &&+1 d)))", 1, 0.81f) //81% for one step
        ;
    }

    @Test
    void testUnifyConclusionSequenceOutcome() {
        test.confTolerance(0.01f);
        test
                .input( "e(x)!")
                .input( "(a(#1) &&+1 (b &&+1 (c &&+1 (d &&+1 e(#1))))).")
                .mustGoal(cycles, "(a(x) &&+1 (b &&+1 (c &&+1 d)))", 1, 0.81f) //81% for one step
        ;
    }

    @Disabled
    @Test void test1() throws Narsese.NarseseException {

        String sequence = "(((f(a) &&+2 f(b)) &&+2 f(c)) &&+2 done)";
        String goal = "done";
        List<Term> log = new FasterList();

        NAR n = NARS.tmp();
        n.termVolumeMax.set(20);
        n.time.dur(4);

        n.onOp1("f", (x,nar)->{
            System.err.println(x);

            nar.want($.func("f", x).neg(), Tense.Present, 1f); //quench

            if (!log.isEmpty()) {
                Term prev = ((FasterList<Term>) log).getLast();
                if (prev.equals(x))
                    return; //same
                nar.believe($.func("f", prev).neg(), nar.time()); //TODO truthlet quench?
            }

            log.add(x);

            nar.believe($.func("f", x), nar.time());

            n.want($$(goal), Tense.Present, 1f); //reinforce
        });

        n.believe(sequence);
        //n.want($$(goal), Tense.Eternal /* Present */, 1f);
        n.want($$(goal), Tense.Present, 1f);
        n.run(1400);
    }



}
