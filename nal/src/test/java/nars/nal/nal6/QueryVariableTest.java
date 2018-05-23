package nars.nal.nal6;

import nars.$;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.term.Term;
import nars.time.Tense;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static nars.time.Tense.ETERNAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class QueryVariableTest {

    @Test
    public void testNoVariableAnswer() throws Narsese.NarseseException {
        testQuestionAnswer("<a --> b>", "<a --> b>");
    }

    @Test
    public void testQueryVariableAnswerUnified() throws Narsese.NarseseException {

        testQuestionAnswer("<a --> b>", "<?x --> b>");
    }

    @Test
    public void testQueryVariableAnswerUnified2() throws Narsese.NarseseException {
        testQuestionAnswer("<c --> (a&b)>", "<?x --> (a&b)>");
    }

    @Test
    public void testQueryVariableMatchesDepVar() throws Narsese.NarseseException {
        testQuestionAnswer("<#c --> (a&b)>", "<?x --> (a&b)>");
    }

    @Test
    public void testQueryVariableMatchesIndepVar() throws Narsese.NarseseException {
        testQuestionAnswer("($x ==> y($x))", "(?x ==> y(?x))");
    }

    @Test
    public void testQueryVariableMatchesTemporally() throws Narsese.NarseseException {
        testQuestionAnswer("(x &&+1 y)", "(?x && y)");
    }

    @Test
    public void testQueryVariableMatchesTemporally2() throws Narsese.NarseseException {
        testQuestionAnswer("(e ==> (x &&+1 y))", "(e ==> (?x && y))");
    }

    @Test
    public void testQuery2() throws Narsese.NarseseException {
        testQueryAnswered(32, 512);
    }

    @Test
    public void testQuery1() throws Narsese.NarseseException {
        testQueryAnswered(1, 512);
    }

    void testQuestionAnswer(@NotNull String beliefString, @NotNull String question) throws Narsese.NarseseException {

        int time = 512;

        AtomicBoolean valid = new AtomicBoolean();

        NAR nar = NARS.tmp();

        Term beliefTerm = $.$(beliefString);
        assertNotNull(beliefTerm);

//        nar.believe(beliefTerm, 1f, 0.9f);
//        assertEquals(1, nar.tasks().count());

        //nar.log();
        nar.question(question, Tense.ETERNAL, (q, a) -> {
            //if (a.term().equals(beliefTerm)) {
            valid.set(true);
            q.delete();
            //}
        });

        nar.believe(beliefTerm, 1f, 0.9f);

        nar.run(time);
        assertTrue(valid.get());
        //assertTrue(2 <= nar.tasks().count());

    }


    void testQueryAnswered(int cyclesBeforeQuestion, int cyclesAfterQuestion) throws Narsese.NarseseException {

        AtomicBoolean b = new AtomicBoolean(false);

        String question = cyclesBeforeQuestion == 0 ?
                "<a --> b>" /* unknown solution to be derived */ :
                "<b --> a>" /* existing solution, to test finding existing solutions */;


        //this.activeTasks = activeTasks;
        NAR n = NARS.tmpEternal();


//        n.log();
        n.input("<a <-> b>. %1.0;0.5%",
                "<b --> a>. %1.0;0.5%");
        n.run(cyclesBeforeQuestion);


        n.question(question, ETERNAL, (q, a) -> {
            assertEquals('?', q.punc());
            assertEquals('.', a.punc());
            if (!a.isDeleted())
                b.set(true);
        });

        n.stopIf(b::get);
        n.run(cyclesAfterQuestion);

        assertTrue(b.get());

    }


//    /** simple test for solutions to query variable questions */
//    @Test public void testQueryVariableSolution() throws InvalidInputException {
//
//        Global.DEBUG = true;
//
//        /*
//        int time1 = 5;
//        int time2 = 15;
//        int time3 = 5;
//        */
//
//        int time1 = 55;
//        int time2 = 115;
//        int time3 = 115;
//
//        //TextOutput.out(n);
//        //new TraceWriter(n, System.out);
//
//        NAR nar = nar();
//
//        nar.frame(time1);
//        String term = "<a --> b>";
//        nar.believe(term);
//        nar.frame(time2);
//
//        //should not output 0.81
//        nar.memory.eventDerived.on( d-> {
//            if (d.isJudgment() && d.getTerm().toString().equals(term)) {
//                assertFalse(d + " should not have been derived", Util.isEqual(d.getConfidence(), 0.81f, 0.01f));
//            }
//        } );
//
//        nar.ask("<?x --> b>");
//
//        nar.frame(time3);
//    }

}

