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
    protected static final int statisticsWindow = 16;
    static final int seed = 1;
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

        var headUnitSize = Head.getUnitSize(memoryWidth);

        var outputSize = machine.outputSize();
        var inputSize = machine.inputSize();

        var weightsCount = (numHeads * memoryHeight) + (memoryHeight * memoryWidth) + (controllerSize * numHeads * memoryWidth) + (controllerSize * inputSize) + (controllerSize) + (outputSize * (controllerSize + 1)) + (numHeads * headUnitSize * (controllerSize + 1));
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
        var totalLoss = 0.0;
        var okt = knownOutput.length - ((knownOutput.length - 2) / 2);
        for (var t = 0; t < knownOutput.length; t++) {

            if (t < okt) continue;

            var ideal = knownOutput[t];
            var actual = machines[t].getOutput();


            double rowLoss = 0;
            for (var i = 0; i < ideal.length; i++) {

                var expected = ideal[i];
                var real = actual[i];


                rowLoss += (expected * (Math.log(real) / log2)) + ((1.0 - expected) * (Math.log(1.0 - real) / log2));


            }
            totalLoss += rowLoss;
        }
        return -totalLoss;
    }

    private static double calculateAbsoluteError(double[][] knownOutput, NTM[] machines) {
        var totalLoss = 0.0;
        var okt = knownOutput.length - ((knownOutput.length - 2) / 2);
        for (var t = 0; t < knownOutput.length; t++) {

            if (t < okt) continue;

            var knownOutputT = knownOutput[t];
            var actual = machines[t].getOutput();


            double rowLoss = 0;
            for (var i = 0; i < knownOutputT.length; i++) {

                var expected = knownOutputT[i];
                var real = actual[i];
                var diff = Math.abs(expected - real);

                rowLoss += diff;
            }
            totalLoss += rowLoss;
        }
        return totalLoss;
    }

    public static StringBuffer toNiceString(double[] x) {
        var sb = new StringBuffer(x.length * 5 + 4);
        sb.append("< ");
        for (var v : x) {
            sb.append(twoDigits.format(v)).append(' ');
        }
        sb.append('>');
        return sb;
    }

    /**
     * train the next sequence
     */
    public double run() {

        var sequence = nextTrainingSequence();

        var timeBefore = System.nanoTime();
        var output = teacher.train(sequence.input, sequence.ideal);
        var trainTimeNS = System.nanoTime() - timeBefore;


        var error = calculateLogLoss(sequence.ideal, output);
        var averageError = error / (
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


