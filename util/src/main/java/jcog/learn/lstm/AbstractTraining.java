package jcog.learn.lstm;


import jcog.Util;

import java.util.Random;
import java.util.function.Consumer;

public abstract class AbstractTraining {

    public AbstractTraining(Random random, int inputs, int outputs) {
        this.random = random;
        this.inputs = inputs;
        this.outputs = outputs;
    }

    public SimpleLSTM lstm(int cell_blocks) {
        return new SimpleLSTM(random, inputs, outputs, cell_blocks);
    }

    @Deprecated public double scoreSupervised(SimpleLSTM agent, float learningRate)  {

        double[] fit = {(double) 0};
        double[] max_fit = {(double) 0};

        this.interact(inter -> {
            if (inter.forget > (float) 0)
                agent.forget(inter.forget);

            if (inter.expected == null) {
                agent.predict(inter.actual);
            }
            else {
                double[] actual_output;

                if (validation_mode)
                    actual_output = agent.predict(inter.actual);
                else
                    actual_output = agent.learn(inter.actual, inter.expected, learningRate);

                if (Util.argmax(actual_output) == Util.argmax(inter.expected))
                    fit[0]++;

                max_fit[0]++;
            }
        });

        return fit[0] / max_fit[0];
    }
































    protected final Random random;
    protected int batches; 
    protected boolean validation_mode;

    @Deprecated protected abstract void interact(Consumer<ExpectedVsActual> each);

    public final int inputs;
    public final int outputs;
}
