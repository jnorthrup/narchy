package jcog.learn.ntm;

import jcog.learn.ntm.run.SequenceGenerator;
import jcog.learn.ntm.run.SequenceLearner;
import jcog.learn.ntm.run.TrainingSequence;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by me on 2/17/17.
 */
class NTMSequenceLearningTest {

    @Test
    void testSimpleSequence() throws Exception {
        SequenceLearner s = new RunSequenceLearner(8);
        double startError = s.run();
        assertTrue(startError > 0.1f);
        for (int i = 0; i < 8000; i++) {
            s.run();
        }

        double endError = s.run();
        assertTrue(endError < 0.01f);
    }


    static class RunSequenceLearner extends SequenceLearner {

        /** print every frame in all sequences, in the order they are trained */
        boolean printSequences;


        RunSequenceLearner(int vectorSize) {
            super(vectorSize, 8, 32, 2, 16);
        }

        @Override
        protected TrainingSequence nextTrainingSequence() {
            return SequenceGenerator.generateSequenceXOR(vectorSize);
            
            
        }

        @Override
        public void onTrained(int sequenceNum, TrainingSequence sequence, NTM[] output, long trainTimeNS, double avgError) {

            double[][] ideal = sequence.ideal;
            int slen = ideal.length;

            if (printSequences) {
                for (int t = 0; t < slen; t++) {
                    double[] actual = output[t].getOutput();
                    System.out.println("\t" + sequenceNum + "#" + t + ":\t" + toNiceString(ideal[t]) + " =?= " + toNiceString(actual));
                }
            }

            if ((sequenceNum+1) % statisticsWindow == 0) {
                System.out.format("@ %d :       avgErr: %f       time(s): %f", i,
                        mean(errors), mean(times)/1.0e9);
                System.out.println();
            }

        }



    }

}