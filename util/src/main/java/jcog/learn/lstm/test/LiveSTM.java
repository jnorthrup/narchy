package jcog.learn.lstm.test;

import jcog.learn.lstm.AbstractTraining;
import jcog.learn.lstm.Interaction;
import jcog.learn.lstm.SimpleLSTM;
import jcog.random.XorShift128PlusRandom;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.util.MathArrays;

import java.util.Random;
import java.util.function.Consumer;

/**
 * Created by me on 7/9/16.
 */
abstract public class LiveSTM extends AbstractTraining {

    public final SimpleLSTM agent;

    @Deprecated
    private final int ERROR_WINDOW_SIZE = 8;

    public boolean train = true;
    DescriptiveStatistics errorHistory = new DescriptiveStatistics();
    private final float learningRate = 0.1f;

    public LiveSTM(int inputs, int outputs, int cellBlocks) {
        this(new XorShift128PlusRandom(1), inputs, outputs, cellBlocks);
    }

    public LiveSTM(Random random, int inputs, int outputs, int cellBlocks) {
        super(random, inputs, outputs);

        this.agent = lstm(cellBlocks);

        errorHistory.setWindowSize(ERROR_WINDOW_SIZE);
    }

    @Override
    public String toString() {
        return agent.toString();
    }

    public double next() {


        Interaction inter = observe();


        double[] predicted;

        double dist;
        if (inter.expected == null) {

            inter.predicted = agent.predict(inter.actual);

            dist = Float.NaN;

        } else {

            if (validation_mode)
                predicted = agent.predict(inter.actual);
            else
                predicted = agent.learn(inter.actual, inter.expected, learningRate);






            inter.predicted = predicted;

            dist = MathArrays.distance1(inter.expected, predicted); 

        }

        if (inter.forget > 0)
            agent.forget(inter.forget);

        errorHistory.addValue(dist);

        return errorHistory.getMean();

    }

    /**
     * the content of the returned Interaction determines the following modes:
     * * expected = null:     prediction only
     * * expected = non-null: learn (optional validation mode)
     * <p>
     * the input and output arrays are not modified or retained, so you may re-use them
     */
    protected abstract Interaction observe();

    @Override
    protected void interact(Consumer<Interaction> each) {
        throw new UnsupportedOperationException();
    }
}
