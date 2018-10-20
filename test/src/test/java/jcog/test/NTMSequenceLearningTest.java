package jcog.test;

import jcog.test.predict.SequenceLearner;
import jcog.test.predict.VectorSequenceProblem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by me on 2/17/17.
 */
class NTMSequenceLearningTest {

    @Test
    void testSimpleSequence() {
        SequenceLearner s = new VectorSequenceProblem(8);
        double startError = s.run();
        assertTrue(startError > 0.1f);
        for (int i = 0; i < 8000; i++) {
            s.run();
        }

        double endError = s.run();
        assertTrue(endError < 0.01f);
    }


}