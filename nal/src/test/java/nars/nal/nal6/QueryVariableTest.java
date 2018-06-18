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
import static org.junit.jupiter.api.Assertions.*;


class QueryVariableTest {

    @Test
    void testNoVariableAnswer() throws Narsese.NarseseException {
        testQuestionAnswer("<a --> b>", "<a --> b>");
    }

    @Test
    void testQueryVariableAnswerUnified() throws Narsese.NarseseException {

        testQuestionAnswer("<a --> b>", "<?x --> b>");
    }

    @Test
    void testQueryVariableAnswerUnified2() throws Narsese.NarseseException {
        testQuestionAnswer("<c --> (a&b)>", "<?x --> (a&b)>");
    }

    @Test
    void testQueryVariableMatchesDepVar() throws Narsese.NarseseException {
        testQuestionAnswer("<#c --> (a&b)>", "<?x --> (a&b)>");
    }

    @Test
    void testQueryVariableMatchesIndepVar() throws Narsese.NarseseException {
        testQuestionAnswer("($x ==> y($x))", "(?x ==> y(?x))");
    }

    @Test
    void testQueryVariableMatchesTemporally() throws Narsese.NarseseException {
        testQuestionAnswer("(x &&+1 y)", "(?x && y)");
    }

    @Test
    void testQueryVariableMatchesTemporally2() throws Narsese.NarseseException {
        testQuestionAnswer("(e ==> (x &&+1 y))", "(e ==> (?x && y))");
    }

    @Test
    void testQuery2() throws Narsese.NarseseException {
        testQueryAnswered(32, 512);
    }

    @Test
    void testQuery1() throws Narsese.NarseseException {
        testQueryAnswered(1, 512);
    }

    private void testQuestionAnswer(@NotNull String beliefString, @NotNull String question) throws Narsese.NarseseException {

        int time = 512;

        AtomicBoolean valid = new AtomicBoolean();

        NAR nar = NARS.tmp();

        Term beliefTerm = $.$(beliefString);
        assertNotNull(beliefTerm);




        
        nar.question(question, Tense.ETERNAL, (q, a) -> {
            
            valid.set(true);
            q.delete();
            
        });

        nar.believe(beliefTerm, 1f, 0.9f);

        nar.run(time);
        assertTrue(valid.get());
        

    }


    private void testQueryAnswered(int cyclesBeforeQuestion, int cyclesAfterQuestion) throws Narsese.NarseseException {

        AtomicBoolean b = new AtomicBoolean(false);

        String question = cyclesBeforeQuestion == 0 ?
                "<a --> b>" /* unknown solution to be derived */ :
                "<b --> a>" /* existing solution, to test finding existing solutions */;


        
        NAR n = NARS.tmpEternal();



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







































}

