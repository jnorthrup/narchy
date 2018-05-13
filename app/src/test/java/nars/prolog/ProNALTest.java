package nars.prolog;

import alice.tuprolog.InvalidTheoryException;
import alice.tuprolog.Theory;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.Op;
import nars.concept.Operator;
import nars.op.prolog.PrologToNAL;
import nars.term.Term;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.TreeSet;

import static nars.time.Tense.ETERNAL;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ProNALTest {


    @Test public void test1() throws InvalidTheoryException, Narsese.NarseseException, IOException, URISyntaxException {
//        Theory t = Theory.string(
//            "add(0,X,X).",
//            "add(s(X),Y,s(Z)):-add(X,Y,Z).\n",
//            "goal(R):-add(s(s(0)),s(s(0)),R)."
//        );
        Theory t = Theory.resource(
            "../../../resources/prolog/furniture.pl"
        );

        NAR n = NARS.tmp(6);
        n.questPriDefault.set(1f);
        n.beliefPriDefault.set(0.5f);
        //n.log();
        Set<String> answers = new TreeSet();
        for (nars.term.Term xx : PrologToNAL.N(t)) {
            if (Op.functor(xx, (xt)->xt.equals(PrologToNAL.QUESTION_GOAL) ? xt : null)!=null) {
                Term qTerm = Operator.args(xx).sub(0).normalize();
                //n.question(q);
                n.question(qTerm, ETERNAL,(q, a) -> {
                    if (answers.add(a.term().toString())) {
                        System.err.println(q + " " + a);
                        System.err.println(a.proof());
                    } /*else {
                        System.err.println("dup");
                    }*/
                });
            } else {
                n.believe(xx.normalize());
            }
        }
        n.run(2500);

        //?- made_of(your_chair,X), colour(X,Colour).
        assertTrue(answers.contains("(colour(wood,brown)&&made_of(your_chair,wood))"));

//        n.concepts().forEach(c -> {
//           c.print();
//        });
        /*
        [0] *** ANSWER=goal(s(s(s(s(0)))))
        TOTAL ANSWERS=1
        */

    }
}
