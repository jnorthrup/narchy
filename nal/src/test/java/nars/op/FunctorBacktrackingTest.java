package nars.op;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.term.Evaluation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FunctorBacktrackingTest {
    /**
     * from: https://en.wikipedia.org/wiki/Prolog#Execution
     *
     */
    @Test  public void test1() throws Narsese.NarseseException {

        NAR n = NARS.shell();
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


        assertEquals(
                "TODO",
                Evaluation.solveAll("father(?Father, ?Child)",  n).toString()
        );

        assertEquals(
                "[sibling(sally,erica)]",
                Evaluation.solveAll("sibling(sally,erica)",  n).toString()
        );
        assertEquals(
                "", //UNKNOWN, not true or false
                Evaluation.solveAll("sibling(tom,erica)",  n).toString()
        );



        /*

        ?- father_child(Father, Child).
            [enumerates all possibilities]
         */

    }
}
