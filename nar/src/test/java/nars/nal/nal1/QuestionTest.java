package nars.nal.nal1;

import nars.*;
import nars.task.util.TaskStatistics;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.test.TestNAR;
import nars.test.impl.DeductiveMeshTest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.DoubleSummaryStatistics;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.IntFunction;

import static nars.$.$$$;
import static nars.term.Functor.f;
import static nars.term.util.TermTest.assertEq;
import static nars.time.Tense.ETERNAL;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by me on 5/24/16.
 */
class QuestionTest {

    private final int withinCycles = 212;

    @Test
    void whQuestionUnifyQueryVar() throws Narsese.NarseseException {
        testQuestionAnswer(withinCycles, "<bird --> swimmer>", "<?x --> swimmer>", "<bird --> swimmer>");
    }

    @Test
    void yesNoQuestion() throws Narsese.NarseseException {
        testQuestionAnswer(withinCycles, "<bird --> swimmer>", "<bird --> swimmer>", "<bird --> swimmer>");
    }

    @Test
    void testTemporalExact() throws Narsese.NarseseException {
        testQuestionAnswer(withinCycles,
                "((a &&+1 b) &&+1 c)",
                "((a &&+1 b) &&+1 c)",
                "((a &&+1 b) &&+1 c)");
    }

    /**
     * question to answer matching
     */
    private void testQuestionAnswer(int cycles, @NotNull String belief, @NotNull String question, @NotNull String expectedSolution) throws Narsese.NarseseException {
        AtomicInteger ok = new AtomicInteger(0);


        Term expectedSolutionTerm = $.$(expectedSolution);

        NAR nar = NARS.tmp(1);


        nar
                .believe(belief, 1.0f, 0.9f)
                .question(question, ETERNAL, (q, a) -> {
                    if (a.punc() == '.' && a.term().equals(expectedSolutionTerm))
                        ok.incrementAndGet();
                });


        nar.run(cycles);


        assertTrue(ok.get() > 0);


    }


    /**
     * tests whether the use of a question guides inference as measured by the speed to reach a specific conclusion
     */
    @Test
    void questionDrivesInference() {

        final int[] dims = {3, 2};
        final int timelimit = 2400;

        TaskStatistics withTasks = new TaskStatistics();
        TaskStatistics withoutTasks = new TaskStatistics();
        DoubleSummaryStatistics withTime = new DoubleSummaryStatistics();
        DoubleSummaryStatistics withOutTime = new DoubleSummaryStatistics();

        IntFunction<NAR> narProvider = (seed) -> {
            NAR d = NARS.tmp(1);
            //d.random().setSeed(seed);
            d.termVolumeMax.set(8);
            d.freqResolution.set(0.1f);
            return d;
        };

        BiFunction<Integer, Integer, TestNAR> testProvider = (seed, variation) -> {
            NAR n = narProvider.apply(seed);
            TestNAR t = new TestNAR(n);
            switch (variation) {
                case 0:
                    new DeductiveMeshTest(t, dims, timelimit);
                    break;
                case 1:
                    new DeductiveMeshTest(t, dims, timelimit) {
                        @Override
                        public void ask(@NotNull TestNAR n, Term term) {

                        }
                    };
                    break;
            }
            return t;
        };

        for (int i = 0; i < 1 /* seed doesnt do anything right now 10 */; i++) {
            int seed = i + 1;

            TestNAR withQuestion = testProvider.apply(seed, 0);
            withQuestion.test();
            withTime.accept(withQuestion.time());
            withTasks.add(withQuestion.nar);

            TestNAR withoutQuestion = testProvider.apply(seed, 1);
            withoutQuestion.test();
            withOutTime.accept(withoutQuestion.time());
            withoutTasks.add(withoutQuestion.nar);
        }

        withTasks.print();
        withoutTasks.print();

        assertNotEquals(withTime, withOutTime);
        System.out.println("with: " + withTime);
        System.out.println("withOut: " + withOutTime);


    }


    @Test
    @Disabled
    void testMathBackchain() throws Narsese.NarseseException {
        NAR n = NARS.tmp(1);


        n.add(f("odd", a -> {
            if (a.subs() == 1 && a.sub(0).op() == Op.ATOM) {
                try {
                    return $.intValue(a.sub(0)) % 2 == 0 ? Bool.False : Bool.True;
                } catch (NumberFormatException ignored) {

                }
            }
            return null;
        }));
        n.termVolumeMax.set(24);
        n.input(
                "({1,2,3,4} --> number).",
                "((({#x} --> number) && odd(#x)) ==> ({#x} --> ODD)).",
                "((({#x} --> number) && --odd(#x)) ==> ({#x} --> EVEN)).",
                "({#x} --> ODD)?",
                "({#x} --> EVEN)?"


        );
        n.run(2500);

    }

    @Disabled
    @Test
    void testDeriveQuestionOrdinary() {
        new TestNAR(NARS.tmp(1))
                .ask("((S | P) --> M)")
                .believe("(S --> M)")
                .mustQuestion(512, "(P --> M)").test();
    }

    @Disabled
    @Test
    void testDeriveQuestOrdinary() {
        new TestNAR(NARS.tmp(1))
                .quest("((S | P) --> M)")
                .believe("(S --> M)")
                .mustQuest(256, "(P --> M)").test();
    }

    @Disabled
    @Test
    void testExplicitEternalizationViaQuestion() {
        new TestNAR(NARS.tmp(1))
                .inputAt(1, "x. | %1.00;0.90%")
                .inputAt(4, "x. | %0.50;0.90%")
                .inputAt(7, "x. | %0.00;0.90%")
                .inputAt(8, "$1.0 x?")
                .mustBelieve(64, "x", 0.5f, 0.73f /*ETERNAL*/)
                .mustBelieve(64, "x", 0.5f, 0.9f, t -> t == 4 /*temporal*/)
                .test();
    }

    @Disabled
    @Test
    void testExplicitEternalizationViaQuestionDynamic() {
        new TestNAR(NARS.tmp(1))
                .inputAt(1, "x. | %1.00;0.90%")
                .inputAt(4, "y. | %1.00;0.90%")
                .inputAt(1, "$1.0 (x &&+3 y)? |")
                .inputAt(1, "$1.0 (x &&+3 y)?")


                .mustBelieve(64, "(x &&+3 y)", 1f, 0.45f, t -> t == ETERNAL)
                .mustBelieve(64, "(x &&+3 y)", 1f, 0.81f, t -> t == 1)
                .test();
    }

    @Test
    void testDepVarInIndepImpl() {
        new TestNAR(NARS.tmp(6))
                .input("f(#x).")
                .input("(f($x) ==> g($x)).")
                .mustBelieve(64, "g(#1)", 1f, 0.81f)
                .test();
    }
    @Test
    void testDepVarInIndepImpl2() {
        new TestNAR(NARS.tmp(6))
                .input("f(#x,y).")
                .input("(f($x,y) ==> g($x,y)).")
                .mustBelieve(64, "g(#1,y)", 1f, 0.81f)
                .test();
    }
    @Test
    void testDepVarInIndepImpl3() {
        assertTrue(!$$$("f(#2,#1)").isNormalized());
        assertEq("f(#1,#2)", $$$("f(#2,#1)").normalize());
        NAR n = NARS.tmp(6);
//        n.onTask(t->{
//            if (t.term().toString().equals("f(#2,#1)"))
//                throw new WTF();
//        });
        new TestNAR(n)
                .input("f(#x,#y).")
                .input("(f($x,#y) ==> g($x,#y)).")
                .mustBelieve(64, "g(#1,#2)", 1f, 0.81f)
                .test();
    }

}
