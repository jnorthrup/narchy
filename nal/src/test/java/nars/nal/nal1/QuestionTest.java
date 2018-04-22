package nars.nal.nal1;

import nars.*;
import nars.task.util.TaskStatistics;
import nars.term.Term;
import nars.test.DeductiveMeshTest;
import nars.test.TestNAR;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.DoubleSummaryStatistics;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.IntFunction;

import static nars.util.time.Tense.ETERNAL;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by me on 5/24/16.
 */
public class QuestionTest {

    final int withinCycles = 212;

    @Test
    public void whQuestionUnifyQueryVar() throws Narsese.NarseseException {
        testQuestionAnswer(withinCycles, "<bird --> swimmer>", "<?x --> swimmer>", "<bird --> swimmer>");
    }

    @Test
    public void yesNoQuestion() throws Narsese.NarseseException {
        testQuestionAnswer(withinCycles, "<bird --> swimmer>", "<bird --> swimmer>", "<bird --> swimmer>");
    }

    @Test
    public void testTemporalExact() throws Narsese.NarseseException {
        testQuestionAnswer(withinCycles,
                "((a &&+1 b) &&+1 c)",
                "((a &&+1 b) &&+1 c)",
                "((a &&+1 b) &&+1 c)");
    }

    /** question to answer matching */
    public void testQuestionAnswer(int cycles, @NotNull String belief, @NotNull String question, @NotNull String expectedSolution) throws Narsese.NarseseException {
        AtomicInteger ok = new AtomicInteger(0);


        Term expectedSolutionTerm = $.$(expectedSolution);

        NAR nar = NARS.tmp(1);
        nar.log();

        nar
                .believe(belief, 1.0f, 0.9f)
                .question(question, ETERNAL,(q, a) -> {
                    if (a.punc() == '.' && a.term().equals(expectedSolutionTerm))
                        ok.incrementAndGet();
                });


        nar.run(cycles);

        //nar.conceptsActive().forEach(System.out::println);

        assertTrue( ok.get() > 0);

//           .onAnswer(question, a -> { //.en("What is a type of swimmer?")
//
//                System.out.println(nar.time() + ": " + question + " " + a);
//                //test for a few task conditions, everything except for evidence
//                if (a.punc() == expectedTask.punc())
//                    if (a.term().equals(expectedTask.term())) {
//                        if (Objects.equals(a.truth(), expectedTask.truth()))
//                            solved.set(true);
//                }
//
//            }).run(cycles);


    }


//    @Test public void testQuestionHandler() throws Narsese.NarseseException {
//        NAR nar = NARS.shell();
//
//        final int[] s = {0};
//        new TaskMatch("add(%1, %2, #x)", nar) {
//
//            @Override public boolean test(@NotNull Task task) { return task.isQuestOrQuestion(); }
//
//            @Override
//            protected void accept(Task task, Map<Term, Term> xy) {
//                System.out.println(task + " " + xy);
//                s[0] = xy.size();
//            }
//        };
//
//        nar.ask($.$("add(1, 2, #x)"));
//
//        assertEquals(3, s[0]);
//
//    }

//    @Test public void testOperationHandler() throws Narsese.NarseseException {
//        NAR nar = NARS.shell();
//
//        final int[] s = {0};
//        StringBuilder match = new StringBuilder();
//        new OperationTaskMatch( $.$("add(%1, %2, #x)"), nar) {
//
//            @Override public boolean test(@NotNull Task task) { return task.isQuestOrQuestion(); }
//
//            @Override
//            protected void onMatch(Term[] args) {
//                match.append(Arrays.toString(args)).append(' ');
//            }
//        };
//
//        nar.ask($.$("add(1, 2, #x)"));
//
//        assertTrue(match.toString().contains("[1, 2, #1026]"));
//
//        nar.ask($.$("add(1, #x)"));
//        nar.ask($.$("(#x --> add)"));
//
//        assertFalse(match.toString().contains("[1, #1026]"));
//    }

    /** tests whether the use of a question guides inference as measured by the speed to reach a specific conclusion */
    @Test public void questionDrivesInference() {

        final int[] dims = {3, 2};
        final int timelimit = 2400;

        TaskStatistics withTasks = new TaskStatistics();
        TaskStatistics withoutTasks = new TaskStatistics();
        DoubleSummaryStatistics withTime = new DoubleSummaryStatistics();
        DoubleSummaryStatistics withOutTime = new DoubleSummaryStatistics();

        IntFunction<NAR> narProvider = (seed) -> {
            NAR d = NARS.tmp(1);
            d.random().setSeed(seed);
            d.termVolumeMax.set(16);
            d.freqResolution.set(0.1f);
            return d;
        };

        BiFunction<Integer,Integer,TestNAR> testProvider = (seed, variation) -> {
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
                            //disabled
                        }
                    };
                    break;
            }
            return t;
        };

        for (int i = 0; i < 10; i++) {
            int seed = i + 1;

            TestNAR withQuestion = testProvider.apply(seed, 0);
            withQuestion.test(true);
            withTime.accept(withQuestion.time());
            withTasks.add(withQuestion.nar);

            TestNAR withoutQuestion = testProvider.apply(seed, 1);
            withoutQuestion.test(true);
            withOutTime.accept(withoutQuestion.time());
            withoutTasks.add(withoutQuestion.nar);
        }

        withTasks.print();
        withoutTasks.print();

        assertNotEquals(withTime, withOutTime);
        System.out.println("with: " + withTime);
        System.out.println("withOut: " + withOutTime);


//        assertTrue(withTime.getSum() < withOutTime.getSum());
//        assertTrue(withTime.getSum() < 2 * withOutTime.getSum()); //less than half, considering that a search "diameter" becomes a "radius" by providing the answer end-point
    }


    @Test @Disabled
    public void testMathBackchain() throws Narsese.NarseseException {
        NAR n = NARS.tmp();
        n.log();



        n.on("odd", a->{
            if (a.subs() == 1 && a.sub(0).op()== Op.ATOM) {
                try {
                    return $.intValue(a.sub(0)) % 2 == 0 ? Op.False : Op.True;
                } catch (NumberFormatException ignored) {

                }
            }
            return null; //$.f("odd", a[0]); //vars, etc.
        });
        n.termVolumeMax.set(24);
        n.input(
            "({1,2,3,4} --> number).",
            "((({#x} --> number) && odd(#x)) ==> ({#x} --> ODD)).",
            "((({#x} --> number) && --odd(#x)) ==> ({#x} --> EVEN)).",
            "({#x} --> ODD)?",
            "({#x} --> EVEN)?"
//            "(1 --> ODD)?",
//            "(1 --> EVEN)?",
//            "(2 --> ODD)?",
//            "(2 --> EVEN)?"
        );
        n.run(2500);

    }

    @Disabled @Test
    public void testDeriveQuestionOrdinary() throws Narsese.NarseseException {
        new TestNAR(NARS.tmp()) //requires NAL3 single premise
                .ask("((S | P) --> M)")
                .believe("(S --> M)")
                .mustQuestion(512, "(P --> M)").test();
    }
    @Disabled @Test
    public void testDeriveQuestOrdinary() throws Narsese.NarseseException {
        new TestNAR(NARS.tmp()) //requires NAL3 single premise
                .quest("((S | P) --> M)")
                .believe("(S --> M)")
                .mustQuest(256, "(P --> M)").test();
    }

    @Test public void testExplicitEternalizationViaQuestion() {
        new TestNAR(NARS.tmp())
                .inputAt(1, "x. :|: %1.00;0.90%")
                .inputAt(4, "x. :|: %0.50;0.90%")
                .inputAt(7, "x. :|: %0.00;0.90%")
                .inputAt(8, "$1.0 x?") //eternal question that triggers eternalization (the answer)
                .mustBelieve(64, "x", 0.5f, 0.73f /*ETERNAL*/)
                .test();
    }

    @Test public void testExplicitEternalizationViaQuestionDynamic() {
        new TestNAR(NARS.tmp())
                .inputAt(1, "x. :|: %1.00;0.90%")
                .inputAt(4, "y. :|: %1.00;0.90%")
                .inputAt(1, "$1.0 (x &&+3 y)? :|:") //temporal
                .inputAt(1, "$1.0 (x &&+3 y)?") //eternal
                //should produce 2 different answers, one temporal and one eternal. both calculated via the dynamic conjunction model
                //.mustBelieve(16, "(x &&+3 y)", 1f, 0.45f, t -> t == ETERNAL)
                .mustBelieve(64, "(x &&+3 y)", 1f, 0.45f, t -> t == ETERNAL)
                .mustBelieve(64, "(x &&+3 y)", 1f, 0.81f, t -> t == 1)
                .test();
    }

//    @Test public void testSaneBudgeting() {
//
//        String c = "((parent($X,$Y) && parent($Y,$Z)) ==> grandparent($X,$Z))";
//        new Default(1000, 8, 1, 3)
//            .logSummaryGT(System.out, 0.1f)
//            .eachFrame(nn->{
//                Concept cc = nn.concept(c);
//                if (cc!=null) {
//                    cc.print(System.out, false, false, true, false);
//                }
//            })
//            .input(c + ".", "")
//            .run(100);
//
//    }

//    @Test public void testPrologLike1() {
//
//        new Default(1000, 8, 1, 3)
//            .logSummaryGT(System.out, 0.1f)
//            .input(
//                "((parent($X,$Y) && parent($Y,$Z)) ==> grandparent($X,$Z)).",
//                "parent(c, p).",
//                "parent(p, g).",
//                "grandparent(p, #g)?"
//            )
//            .run(800);
//
//    }
}
