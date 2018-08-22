package nars.op;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.eval.Evaluation;
import nars.term.Term;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FunctorBacktrackingTest {

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

    @Test  public void test2() {

        assertEquals(
                "[father(mike,tom), father(tom,sally), father(tom,erica)]",
                Evaluation.query("father(?Father, ?Child)", n).toString()
        );
    }
    @Test  public void test3() throws Narsese.NarseseException {

        assertEquals(
                "[wonder(sibling(sally,erica))]",
                Evaluation.query("sibling(sally,erica)", n).toString()
        );
        n.believe("mother(trude,erica)");


        //becomes true now
        Set<Term> ee = Evaluation.query("sibling(sally,erica)", n);
        assertEquals(
                "[sibling(sally,erica)]",
                ee.toString()
        );

    }

    @Test  public void test4() {

        assertEquals(
                "[wonder(sibling(tom,erica))]", //UNKNOWN, not true or false
                Evaluation.query("sibling(tom,erica)",  n).toString()
        );



        /*

        ?- father_child(Father, Child).
            [enumerates all possibilities]
         */

    }

}
