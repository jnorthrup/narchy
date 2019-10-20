package nars.op;

import jcog.math.FloatSupplier;
import nars.term.Compound;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.function.IntConsumer;

/**
 * Created by me on 6/27/16.
 */
public abstract class RelativeSignalClassifier  {
    protected final DescriptiveStatistics history;
    private final FloatSupplier input;
    private final IntConsumer output;

    public RelativeSignalClassifier(FloatSupplier input, int windowSize, IntConsumer classified) {
        this.history = new DescriptiveStatistics(windowSize);
        this.input = input;
        this.output = classified;
    }

    public void run() {
        float h = input();

        float dMean = (float)(h - history.getMean());
        double varianceThresh = history.getVariance();
        history.addValue(h);

        
        int y;

        Compound e;
        if (dMean > varianceThresh) {
            y = +1;
        } else if (dMean < -varianceThresh) {
            y = -1;
        } else {
            y = 0;
        }

        output.accept(y);

    }

    public float input() {
        return input.asFloat();
    }


}
