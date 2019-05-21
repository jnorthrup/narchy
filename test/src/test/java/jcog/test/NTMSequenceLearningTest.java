package jcog.test;

import jcog.Util;
import jcog.learn.ntm.NTM;
import jcog.learn.ntm.run.SequenceGenerator;
import jcog.learn.ntm.run.TrainingSequence;
import jcog.test.predict.SequenceLearner;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by me on 2/17/17.
 */
class NTMSequenceLearningTest {

    @ParameterizedTest
    @ValueSource(ints = { 1, 2, 3, 4 })
    void testSimpleSequence4(int vectorSize) {
        testSimpleSequence(vectorSize, vectorSize * 3);
    }

    static void testSimpleSequence(int vectorSize, int length) {
        SequenceLearner s = new SequenceLearner(
                vectorSize, vectorSize, vectorSize, Math.max(1, vectorSize/2), vectorSize * 2) {
            @Override
            protected TrainingSequence nextTrainingSequence() {
                return SequenceGenerator.generateSequenceXOR(length, vectorSize);
            }

            @Override
            public void onTrained(int sequenceNum, TrainingSequence sequence, NTM[] output, long trainTimeNS, double avgError) {

                double[][] ideal = sequence.ideal;
                int slen = ideal.length;

//        if (printSequences) {
//            for (int t = 0; t < slen; t++) {
//                double[] actual = output[t].getOutput();
//                System.out.println("\t" + sequenceNum + '#' + t + ":\t" + toNiceString(ideal[t]) + " =?= " + toNiceString(actual));
//            }
//        }

                if ((sequenceNum+1) % statisticsWindow == 0) {
                    System.out.format("@ %d :       avgErr: %f       time(s): %f", i,
                            Util.mean(errors), Util.mean(times)/1.0e9);
                    System.out.println();
                }
            }
        };

        double startError = s.run();
//        assertTrue(startError > 0.1f);
        for (int i = 0; i < 8000; i++)
            s.run();

        double endError = s.run();
        assertTrue(endError < 0.01f);
    }


}