package nars.op.prolog;

import alice.tuprolog.InvalidTheoryException;
import alice.tuprolog.Theory;
import com.google.common.base.Joiner;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PrologToNALTest {

    @Test public void testRuleImpl() throws InvalidTheoryException, IOException, URISyntaxException {

        String expected = "add(0,?X,?X) , (add($X,$Y,$Z)==>add(s($X),$Y,s($Z))) , (add(s(s(0)),s(s(0)),$R)==>goal($R))";
        Iterable<alice.tuprolog.Term> input = Theory.resource(
            "../../../resources/prolog/add.pl"
        );
        assertTranslated(expected, input);
    }

    @Test public void testConjCondition2() throws InvalidTheoryException {

        String expected = "(($X&&$Y)==>conj($X,$Y))";
        Iterable<alice.tuprolog.Term> input = Theory.string(
            "conj(X,Y) :- X, Y."
        );
        assertTranslated(expected, input);
    }
    @Test public void testConjCondition3() throws InvalidTheoryException {

        String expected = "((&&,$X,$Y,$Z)==>conj($X,$Y,$Z))";
        Iterable<alice.tuprolog.Term> input = Theory.string(
            "conj(X,Y,Z) :- X, Y, Z."
        );
        assertTranslated(expected, input);
    }

   @Test public void testQuestionGoal() throws Exception {

        String expected = "\"?-\"((isa(your_chair,?X)&&ako(?X,seat)))";
        Iterable<alice.tuprolog.Term> input = Theory.string(
            "?- isa(your_chair,X), ako(X,seat)."
        );
        assertTranslated(expected, input);
    }

    static void assertTranslated(String expected, Iterable<alice.tuprolog.Term> input) {
        String actual = Joiner.on(" , ").join(PrologToNAL.N(input));
        assertEquals(expected, actual, ()->input + "\n\tshould produce:\n\t" + expected);
    }

}
