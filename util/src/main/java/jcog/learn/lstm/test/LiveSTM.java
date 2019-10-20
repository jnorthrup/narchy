package jcog.learn.lstm.test;

import jcog.learn.lstm.AbstractTraining;
import jcog.learn.lstm.ExpectedVsActual;
import jcog.learn.lstm.SimpleLSTM;
import jcog.random.XorShift128PlusRandom;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.util.MathArrays;

import java.util.Random;
import java.util.function.Consumer;

/**
 * Created by me on 7/9/16.
 */
public abstract class LiveSTM extends AbstractTraining {

    public final SimpleLSTM agent;

    public boolean train = true;
    DescriptiveStatistics errorHistory = new DescriptiveStatistics();

    public LiveSTM(int inputs, int outputs, int cellBlocks) {
        this(new XorShift128PlusRandom(1), inputs, outputs, cellBlocks);
    }

    public LiveSTM(Random random, int inputs, int outputs, int cellBlocks) {
        super(random, inputs, outputs);

        this.agent = lstm(cellBlocks);

        int ERROR_WINDOW_SIZE = 8;
        errorHistory.setWindowSize(ERROR_WINDOW_SIZE);
    }

    @Override
    public String toString() {
        return agent.toString();
    }

    public double next() {


        ExpectedVsActual inter = observe();


        double dist;
        if (inter.expected == null) {

            inter.predicted = agent.predict(inter.actual);

            dist = Float.NaN;

        } else {

            float learningRate = 0.1f;
            double[] predicted;
			predicted = validation_mode ? agent.predict(inter.actual) : agent.learn(inter.actual, inter.expected, learningRate);






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
    protected abstract ExpectedVsActual observe();

    @Override
    protected void interact(Consumer<ExpectedVsActual> each) {
        throw new UnsupportedOperationException();
    }
}
