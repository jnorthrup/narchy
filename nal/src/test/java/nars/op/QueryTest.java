package nars.op;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.eval.Evaluation;
import nars.eval.FactualEvaluator;
import nars.term.Term;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static nars.$.$$;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class QueryTest {

    @Test void testFactPos() throws Narsese.NarseseException {
        final NAR n = NARS.shell();
        n.believe("f(x)");

        Set<Term> e = Evaluation.queryAll($$("f(?what)"), n);
        assertEquals("[f(x)]", e.toString());
    }

    @Test void testFactsPos() throws Narsese.NarseseException {
        final NAR n = NARS.shell();
        n.believe("f(x)");
        n.believe("f(y)");

        Set<Term> e = Evaluation.queryAll($$("f(?what)"), n);
        assertEquals("[f(x),f(y)]", e.toString());
    }

    @Test void testFactNeg() throws Narsese.NarseseException {
        final NAR n = NARS.shell();
        n.believe("--f(x)");

        Set<Term> e = Evaluation.queryAll($$("f(?what)"), n);
        assertEquals("[(--,f(x))]", e.toString());
    }

    @Test void testFactImpliedByFact() throws Narsese.NarseseException {
        final NAR n = NARS.shell();
        n.believe("(f(x) ==> g(x))");
        n.believe("f(x)");

        Set<Term> e = Evaluation.queryAll($$("g(?what)"), n);
        assertEquals("[g(x)]", e.toString());
    }

    @Test void testFactImpliedByConj() throws Narsese.NarseseException {
        final NAR n = NARS.shell();
        n.believe("((f(x) && f(y)) ==> g(x,y))");
        n.believe("f(x)");
        n.believe("f(y)");

        Set<Term> e = Evaluation.queryAll($$("g(?1,?2)"), n);
        assertEquals("[g(x,y),g(y,x)]", e.toString());
    }

    static class FunctorBacktrackingTest {
        final NAR n = NARS.shell();

        {
            try {
                /**
                 * from: https://en.wikipedia.org/wiki/Prolog#Execution
                 *
                 */
                n.input(
                        "mother(trude, sally).",
                        "father(tom, sally).",
                        "father(tom, erica).",
                        "father(mike, tom).",

                        "(father($X, $Y) ==> parent($X, $Y)).",
                        "(mother($X, $Y) ==> parent($X, $Y)).",
                        "((parent(#Z, $X) && parent(#Z, $Y)) ==> sibling($X, $Y))."

                        //TODO
                        //"prolog(\"sibling(X, Y)      :- parent_child(Z, X), parent_child(Z, Y)\").",


                );
            } catch (Narsese.NarseseException e) {
                e.printStackTrace();
            }
        }

        @Test
        public void test2() throws Narsese.NarseseException {

            FactualEvaluator e = Evaluation.query("father(?Father, ?Child)", n);
            e.print();
            //"[father(mike,tom), father(tom,sally), father(tom,erica)]",
            assertEquals("{father(tom,sally)=[true], father(tom,erica)=[true], father(mike,tom)=[true]}",
                    e.nodes.toString());
        }

        @Test
        public void test3() throws Narsese.NarseseException {

            n.log();

            //        "[wonder(sibling(sally,erica))]",
            {
                FactualEvaluator e = Evaluation.query("sibling(sally,erica)", n);
                e.print();
            }

            n.believe("mother(trude,erica)"); //becomes true only after this missing information

            {
                FactualEvaluator e = Evaluation.query("sibling(sally,erica)", n);
                e.print();
//            assertEquals(
//                    "[sibling(sally,erica)]",
//                    ee.toString()
//            );

            }

        }

        @Test
        public void test4() throws Narsese.NarseseException {
            FactualEvaluator e = Evaluation.query("sibling(tom,erica)", n);
            e.print();

//        assertEquals(
//                "[wonder(sibling(tom,erica))]", //UNKNOWN, not true or false
//                .toString()
//        );



        /*

        ?- father_child(Father, Child).
            [enumerates all possibilities]
         */

        }
    }
}
