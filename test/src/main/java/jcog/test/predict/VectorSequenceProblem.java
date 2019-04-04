package jcog.test.predict;

import jcog.Util;
import jcog.learn.ntm.NTM;
import jcog.learn.ntm.run.SequenceGenerator;
import jcog.learn.ntm.run.TrainingSequence;

/** TODO abstract further */
public class VectorSequenceProblem extends SequenceLearner {

    /** print every frame in all sequences, in the order they are trained */
    boolean printSequences;


    public VectorSequenceProblem(int vectorSize) {
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
                System.out.println("\t" + sequenceNum + '#' + t + ":\t" + toNiceString(ideal[t]) + " =?= " + toNiceString(actual));
            }
        }

        if ((sequenceNum+1) % statisticsWindow == 0) {
            System.out.format("@ %d :       avgErr: %f       time(s): %f", i,
                    Util.mean(errors), Util.mean(times)/1.0e9);
            System.out.println();
        }

    }



}
