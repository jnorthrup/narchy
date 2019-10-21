package nars.nal.nal6;

import nars.*;
import nars.term.Term;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static nars.Op.BELIEF;
import static org.junit.jupiter.api.Assertions.*;


@Disabled
class QueryVariableTest {

    @Test
    void testNoVariableAnswer() throws Narsese.NarseseException {
        testQuestionAnswer("<a --> b>", "<a --> b>");
    }

    @Test
    void testQueryVariableAnswerUnified() throws Narsese.NarseseException {

        testQuestionAnswer("<?x --> b>", "<a --> b>");
    }

    @Test
    void testQueryVariableAnswerUnified2() throws Narsese.NarseseException {
        testQuestionAnswer("<?x --> (a&b)>", "<c --> (a&b)>");
    }


    @Test
    void testQueryVariableMatchesDepVar() throws Narsese.NarseseException {
        testQuestionAnswer("<?x --> (a&b)>", "<#c --> (a&b)>");
    }


    @Test
    void testQueryVariableMatchesIndepVar() throws Narsese.NarseseException {
        testQuestionAnswer("(?x ==> y(?x))", "($x ==> y($x))");
    }
    @Test
    void testQueryVariableMatchesIndepVarXternal() throws Narsese.NarseseException {
        testQuestionAnswer("(?x ==>+- y(?x))", "($x ==>+1 y($x))");
    }

    @Test
    void testQueryVariableMatchesTemporally() throws Narsese.NarseseException {
        testQuestionAnswer("(?x &&+- y)", "(x &&+1 y)");
    }

    @Test
    void testQueryVariableMatchesTemporally2() throws Narsese.NarseseException {
        testQuestionAnswer("(e ==> (?x &&+- y))", "(e ==> (x &&+1 y))");
    }

    @Test
    void testQuery1() throws Narsese.NarseseException {
        testQueryAnswered(4, 64);
    }


    private static void testQuestionAnswer(String question, String belief) throws Narsese.NarseseException {

        AtomicBoolean valid = new AtomicBoolean();

        NAR nar = NARS.shell();

        Term beliefTerm = $.$(belief);
        assertNotNull(beliefTerm);




        nar.onTask(new Consumer<Task>() {
            @Override
            public void accept(Task a) {
//            if (a.isBelief() && a.term().unify(question, new UnifyAny())) {
//                valid.set(true);
//                //q.delete();
//            }
            }
        });
        nar.question(question);

        nar.believe(beliefTerm, 1f, 0.9f);

        int time = 32;
        nar.run(time);
        assertTrue(valid.get());
        

    }


    private static void testQueryAnswered(int cyclesBeforeQuestion, int cyclesAfterQuestion) throws Narsese.NarseseException {

        AtomicBoolean b = new AtomicBoolean(false);

        String question = cyclesBeforeQuestion == 0 ?
                "(a --> b)" /* unknown solution to be derived */ :
                "(b --> a)" /* existing solution, to test finding existing solutions */;

        NAR n = NARS.tmp(1);

        n.input("(a <-> b). %1.0;0.5%",
                "(b --> a). %1.0;0.5%");
        n.run(cyclesBeforeQuestion);

        n.onTask(new Consumer<Task>() {
            @Override
            public void accept(Task a) {
                if (a.punc() == BELIEF && a.term().equals(question)) {
                    assertEquals('.', a.punc());
                    if (!a.isDeleted())
                        b.set(true);
                }
            }
        });
        n.question(question);

        n.stopIf(b::get);
        n.run(cyclesAfterQuestion);

        assertTrue(b.get());

    }







































}

