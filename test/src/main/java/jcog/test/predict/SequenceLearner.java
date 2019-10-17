package jcog.test.predict;

import jcog.learn.ntm.NTM;
import jcog.learn.ntm.learn.BPTTTeacher;
import jcog.learn.ntm.learn.RMSPropWeightUpdater;
import jcog.learn.ntm.learn.RandomWeightInitializer;
import jcog.learn.ntm.memory.address.Head;
import jcog.learn.ntm.run.TrainingSequence;
import jcog.random.XorShift128PlusRandom;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Random;

/**
 * remove abstract, removing dependency on NTM
 */
public abstract class SequenceLearner {

    static final double log2 = Math.log(2.0);
    static final NumberFormat twoDigits = new DecimalFormat("0.00");
    public final NTM machine;
    protected final int vectorSize;
    protected final int statisticsWindow = 16;
    final int seed = 1;
    protected final Random rand = new XorShift128PlusRandom(seed);
    private final BPTTTeacher teacher;
    protected double[] errors = new double[statisticsWindow];
    protected double[] times = new double[statisticsWindow];
    protected int i;

    public SequenceLearner(int vectorSize, int memoryWidth, int memoryHeight, int numHeads, int controllerSize) {

        this.vectorSize = vectorSize;
        this.machine = new NTM(
                vectorSize,
                vectorSize,
                controllerSize,
                numHeads,
                memoryHeight,
                memoryWidth,
                new RandomWeightInitializer(rand));

        Arrays.fill(errors, 1.0);

        int headUnitSize = Head.getUnitSize(memoryWidth);

        final int outputSize = machine.outputSize();
        final int inputSize = machine.inputSize();

        int weightsCount = (numHeads * memoryHeight) + (memoryHeight * memoryWidth) + (controllerSize * numHeads * memoryWidth) + (controllerSize * inputSize) + (controllerSize) + (outputSize * (controllerSize + 1)) + (numHeads * headUnitSize * (controllerSize + 1));
//        System.out.println("# Weights: "  + weightsCount);

        teacher = new BPTTTeacher(machine,
                new RMSPropWeightUpdater(weightsCount,
                        0.05, 0.05, 0.02, 0.001));

    }

    /**
     * shift all items in an array down 1 index, leaving the last element ready for a new item
     */
    public static void pop(double[] x) {
        System.arraycopy(x, 0, x, 1, x.length - 1);

        /*for (int i = x.length-2; i >= 0; i--) {
            x[i+1] = x[i];
        }*/
    }

    public static void pop(Object[] x) {
        System.arraycopy(x, 0, x, 1, x.length - 1);
        /*for (int i = x.length-2; i >= 0; i--) {
            x[i+1] = x[i];
        }*/
    }

    public static void push(double[] x, double v) {
        x[0] = v;
    }

    public static void popPush(double[] x, double v) {
        pop(x);
        push(x, v);
    }

    private static double calculateLogLoss(double[][] knownOutput, NTM[] machines) {
        double totalLoss = 0.0;
        int okt = knownOutput.length - ((knownOutput.length - 2) / 2);
        for (int t = 0; t < knownOutput.length; t++) {

            if (t < okt) continue;

            final double[] ideal = knownOutput[t];
            final double[] actual = machines[t].getOutput();


            double rowLoss = 0;
            for (int i = 0; i < ideal.length; i++) {

                final double expected = ideal[i];
                final double real = actual[i];


                rowLoss += (expected * (Math.log(real) / log2)) + ((1.0 - expected) * (Math.log(1.0 - real) / log2));


            }
            totalLoss += rowLoss;
        }
        return -totalLoss;
    }

    private static double calculateAbsoluteError(double[][] knownOutput, NTM[] machines) {
        double totalLoss = 0.0;
        int okt = knownOutput.length - ((knownOutput.length - 2) / 2);
        for (int t = 0; t < knownOutput.length; t++) {

            if (t < okt) continue;

            final double[] knownOutputT = knownOutput[t];
            final double[] actual = machines[t].getOutput();


            double rowLoss = 0;
            for (int i = 0; i < knownOutputT.length; i++) {

                final double expected = knownOutputT[i];
                final double real = actual[i];
                final double diff = Math.abs(expected - real);

                rowLoss += diff;
            }
            totalLoss += rowLoss;
        }
        return totalLoss;
    }

    public static StringBuffer toNiceString(double[] x) {
        StringBuffer sb = new StringBuffer(x.length * 5 + 4);
        sb.append("< ");
        for (double v : x) {
            sb.append(twoDigits.format(v)).append(' ');
        }
        sb.append('>');
        return sb;
    }

    /**
     * train the next sequence
     */
    public double run() {

        TrainingSequence sequence = nextTrainingSequence();

        long timeBefore = System.nanoTime();
        NTM[] output = teacher.train(sequence.input, sequence.ideal);
        long trainTimeNS = System.nanoTime() - timeBefore;


        double error = calculateLogLoss(sequence.ideal, output);
        double averageError = error / (
                sequence.ideal.length * sequence.ideal[0].length);


        popPush(errors, averageError);
        popPush(times, trainTimeNS);

        onTrained(i, sequence, output, trainTimeNS, averageError);

        i++;

        return averageError;
    }

    protected abstract TrainingSequence nextTrainingSequence();

    public void onTrained(int sequenceNum, TrainingSequence sequence, NTM[] output, long trainTimeNS, double avgError) {


    }


}


