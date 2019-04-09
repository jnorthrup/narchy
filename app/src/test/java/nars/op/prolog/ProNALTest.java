package nars.op.prolog;

import alice.tuprolog.InvalidTheoryException;
import alice.tuprolog.Theory;
import nars.NAR;
import nars.NARS;
import nars.term.Functor;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled
public class ProNALTest {


    @Test public void test1() throws InvalidTheoryException, IOException, URISyntaxException {





        Theory t = Theory.resource(
            "../../../resources/prolog/furniture.pl"
        );

        NAR n = NARS.tmp(6);
        n.questPriDefault.set(1f);
        n.beliefPriDefault.pri(0.5f);

        Set<String> answers = new TreeSet();
        for (nars.term.Term xx : PrologToNAL.N(t)) {
            if (Functor.isFunc(xx)) {

            }
//            if (Functor.ifFunc(xx, (xt)->xt.equals(PrologToNAL.QUESTION_GOAL) ? xt : null)!=null) {
//                Term qTerm = Operator.args(xx).sub(0).normalize();
//
//                n.question(qTerm, ETERNAL,(q, a) -> {
//                    if (answers.addAt(a.target().toString())) {
//                        System.err.println(q + " " + a);
//                        System.err.println(a.proof());
//                    } /*else {
//                        System.err.println("dup");
//                    }*/
//                });
//            } else {
//                n.believe(xx.normalize());
//            }
        }
        n.run(2500);


        assertTrue(answers.contains("(colour(wood,brown)&&made_of(your_chair,wood))"));




        /*
        [0] *** ANSWER=goal(s(s(s(s(0)))))
        TOTAL ANSWERS=1
        */

    }
}
