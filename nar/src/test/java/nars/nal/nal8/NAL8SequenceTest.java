package nars.nal.nal8;

import jcog.data.list.FasterList;
import jcog.random.XoRoShiRo128PlusRandom;
import nars.$;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.term.Term;
import nars.term.util.conj.ConjMatch;
import nars.test.NALTest;
import nars.time.Tense;
import nars.unify.UnifyTransform;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static nars.$.$$;
import static nars.Op.BELIEF;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** test precision of sequence execution (planning) */
public class NAL8SequenceTest extends NALTest {

    public static final int cycles = 50;

    @BeforeEach void init() {
        test.termVolMax(26);
    }

    @Test
    void testSubSequence2_end() {
        
        test
                .input( "(x &&+1 y)!")
                .input( "(z &&+1 (x &&+1 y)).")
                .mustGoal(cycles, "z", 1, 0.81f) //81% for one step
        ;
    }
    @Test
    void testSubSequence2_mid() {

        test
                .termVolMax(14)
                .confMin(0.75f)
                .input( "(x &&+1 y)!")
                .input( "(z &&+1 ((x &&+1 y) &&+1 w)).")
                .mustGoal(cycles, "z", 1, 0.81f) //81% for one step
        ;
    }

    @ParameterizedTest
    @ValueSource(strings = {"&&"})
    void testSubParallelOutcome(String conj) {
        
        test
                .input( "x!")
                .input('(' + conj + ",x,y,z).")
                .mustGoal(cycles, "(y"+conj+"z)", 1, 0.81f) //81% for one step
        ;
    }

    @Test
    void testMidSequenceOutcome() {
        
        test
                .input( "c!")
                .input( "(a &&+1 (b &&+1 (c &&+1 (d &&+1 e)))).")
                .mustGoal(cycles, "(a &&+1 b)", 1, 0.81f) //81% for one step
        ;
    }
    @Test
    void testStartSequenceDTernalComponentOutcome() {
        
        test
                .input( "b!")
                .input( "((a&&b) &&+1 c).")
                .mustGoal(cycles, "a", 1, 0.81f) //81% for one step
        ;
    }
    @Test
    void testMidSequenceDTernalComponentOutcome() {
        
        test
                .input( "c!")
                .input( "(a &&+1 (b &&+1 ((c&&f) &&+1 (d &&+1 e)))).")
                .mustGoal(cycles, "(a &&+1 (b &&+1 f))", 1, 0.81f) //81% for one step
        ;
    }

    @Test
    void testMidSequenceDTernalComponentWithUnification() {
        
        test
                .input( "c(x)!")
                .input( "(a &&+1 (b(#1) &&+1 ((&&,a,b,c(#1),d(x,y)) &&+1 (d &&+1 e)))).")
                .mustGoal(cycles, "(a &&+1 (b(x) &&+1 (&&,a,b,d(x,y))))", 1, 0.81f) //81% for one step
        ;
    }
    @Test
    void testMidSequenceDTernalComponentOutcome_Alternate_Sort() {
        
        test
                .input( "f!")
                .input( "(a &&+1 (b &&+1 ((c&&f) &&+1 (d &&+1 e)))).")
                .mustGoal(cycles, "(a &&+1 (b &&+1 c))", 1, 0.81f) //81% for one step
        ;
    }

    @Disabled @Test //TODO
    void testXternal() {

        test
            .input( "f.")
            .input( "(a &&+- (b &&+1 (c&&f))).")
            .mustBelieve(cycles, "(a &&+- (b &&+1 c))", 1, 0.81f) //81% for one step
        ;
    }

    @Test
    void testNegMidSequenceOutcome() {
        
        test
                .input( "--c!")
                .input( "(a &&+1 (b &&+1 (--c &&+1 (d &&+1 e)))).")
                .mustGoal(cycles, "(a &&+1 b)", 1, 0.81f) //81% for one step
        ;
    }
    @Test
    void testEqualConclusionSequenceOutcome() {
        
        test
                .input( "e!")
                .input( "(a &&+1 (b &&+1 (c &&+1 (d &&+1 e)))).")
                .mustGoal(cycles, "(a &&+1 (b &&+1 (c &&+1 d)))", 1, 0.81f) //81% for one step
        ;
    }

    @Test
    void testUnifyConclusionSequenceOutcome() {
        
        test
                .input( "e(x)!")
                .input( "(a(#1) &&+1 (b &&+1 (c &&+1 (d &&+1 e(#1))))).")
                .mustGoal(cycles, "(a(x) &&+1 (b &&+1 (c &&+1 d)))", 1, 0.81f) //81% for one step
        ;
    }

    @Test
    void testBeliefDeduction_MidSequenceDTernalComponent() {
        
        test
                .input( "c.")
                .input( "(a &&+1 (b &&+1 ((c&&f) &&+1 (d &&+1 e)))).")
                .mustBelieve(cycles, "(f &&+1 (d &&+1 e))", 1, 0.81f) //81% for one step
        ;
    }
    @Test
    void testBeliefDeduction_MidSequenceDTernalComponentWithUnification() {
        
        test
                .input( "c(x).")
                .input( "(a &&+1 (b(#1) &&+1 ((&&,a,b,c(#1),d(x,#1)) &&+1 (d &&+1 e(#1))))).")
                .mustBelieve(cycles, "((&&,a,b,d(x,x)) &&+1 (d &&+1 e(x)))", 1, 0.81f) //81% for one step
        ;
    }

    @Test
    void beliefDeduction_MidSequence_Conj() {

        test
                .input( "(a &&+1 ((b(x)&&c) &&+1 ((c(#1) && d(x,#1)) &&+1 (d &&+1 e(#1))))).")
                .input( "(b(x)&&c).")
                .mustBelieve(cycles, "(((d(x,#1)&&c(#1)) &&+1 d) &&+1 e(#1))", 1, 0.81f) //81% for one step
        ;
    }

    @Test
    void beliefDeduction_MidSequence_Disj() {

        test
                .input( "(a &&+1 ((b(x)||c) &&+1 (c(#1) &&+1 (d &&+1 e)))).")
                .input( "c.")
                .mustBelieve(cycles, "((c(#1) &&+1 d) &&+1 e)", 1, 0.81f) //81% for one step
        ;
    }
    @Test
    void testGoalDeduction_MidSequenceDTernalComponentWithUnification() {

        String g = "(a &&+1 ((b(#1)&&c) &&+1 ((&&,c(#1),d(x,#1)) &&+1 d)))";
        String b = "c(x)";
        test
            .believe(g)
            .believe( b )
            .mustBelieve(cycles, "((&&,d(x,x)) &&+1 d)", 1, 0.81f) //81% for one step
        ;
    }

    @Test
    void testGoalDeduction_MidSequenceDTernalComponentWithUnification2() {
        {

            Term seq = $$("( ( ( (d(x,#1) &&+1 e(#1)) &&+1 (b(#1)&&c))  &&+1 c(#1)) &&+1 f)");
            Term cmd = $$("(b(x) &&+1 c(x))");
            Term result = $$("f");
            assertEquals(result, ConjMatch.beforeOrAfter(seq, cmd, false, false, true,
                    new UnifyTransform(new XoRoShiRo128PlusRandom(1)),
                    10));
        }

        Term seq = $$("((d(x,#1) &&+1 e(#1)) &&+1 ((b(#1)&&c) &&+1 c(#1)))");
        Term cmd = $$("(b(x) &&+1 c(x))");
        Term result = $$("((d(x,x) &&+1 e(x)) &&+1 c)");
        assertEquals(result, ConjMatch.beforeOrAfter(seq, cmd, true, false, false,
                new UnifyTransform(new XoRoShiRo128PlusRandom(1)),
                10));

        test.termVolMax(24);
        test
                .believe( seq.toString() )
                .goal( cmd.toString() )
                .mustGoal(cycles, result.toString(), 1, 0.81f) //81% for one step
        //.mustGoal(cycles, "(&&,a,b,d(x,x))", 1, 0.81f) //81% for one step
        ;
    }

    @Test
    void testGoalDeduction_ParallelWithDepVar() {
        test
                .input( "(x(#1,#2) && y(#1,#2)).")
                .input( "x(1,1)!")
                .mustGoal(cycles, "y(1,1)", 1, 0.81f) //81% for one step
        ;
    }
    @Test
    void testGoalDeduction_Neg_ParallelWithDepVar() {
        test
                .input( "(--x(#1,#2) && y(#1,#2)).")
                .input( "--x(1,1)!")
                .mustGoal(cycles, "y(1,1)", 1, 0.81f) //81% for one step
        ;
    }

    @Test
    void testGoalDeduction_ParallelWithDepVar_and_Arithmetic() {
        test
                .termVolMax(16)
                .input( "(&&, x(#1,#2), add(#1,#2,#3), y(#3)).")
                .input( "x(1,1)!")
                .mustGoal(cycles, "y(2)", 1, 0.81f) //81% for one step
        ;
    }
    @Test
    void testGoalDeduction_ParallelWithDepVar_and_Specific_Arithmetic() {
        test
                .termVolMax(17)
                .input( "(&&, x(#1,#2), y(#1,#2), --equal(#1,#2)).")
                .input( "x(1,1).")
                .input( "x(1,2).")
                .mustBelieve(cycles, "y(1,2)", 1, 0.81f) //81% for one step
                .mustNotOutput(cycles, "y(1,1)", BELIEF) //81% for one step
        ;
    }


    @Disabled
    @Test void one() throws Narsese.NarseseException {

        String sequence = "(((f(a) &&+2 f(b)) &&+2 f(c)) &&+2 done)";
        String goal = "done";
        List<Term> log = new FasterList();

        NAR n = NARS.tmp();
        n.termVolMax.set(20);
        n.time.dur(4);

        n.addOp1("f", (x, nar)->{
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

    @Test void taskEventConjBeforeCorrectTime() {
        /*
        $0.0 (trackXY-->left)! 172456⋈172471 %.10;.01% {172470: m;2~ú;2À7}
            $.13 ((--,(trackXY-->left))&&(--,(trackXY-->near)))! %1.0;.01% {172464: m}
            $.02 ((--,(trackXY-->left)) &&+64 ((--,(trackXY-->left))&&(--,(trackXY-->near)))). 172392⋈172407 %.90;.81% {172470: 2~ú;2À7}
        */
        test
            .input("x!")//eternal
            .input("(y &&+10 x). |") //temporal
            .mustGoal(cycles, "y", 1, 0.81f, t->t>=0 /*-10*/);
    }
    @Test void taskEventConjBeforeCorrectTime2() {

        test
            .input("(--x&&z)!")//eternal
            .input("(y &&+10 (--x&&z)). |") //temporal
            .mustGoal(cycles, "y", 1, 0.81f, t->t>=0 /*-10*/);
    }
    @Test void testGoalUnionDeduction() {
        test
            .input("(x||y)!")//eternal
            .input("--x. |") //temporal
            .mustGoal(cycles, "y", 1, 0.81f,  0);
    }
}
