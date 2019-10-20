package jcog.math;

/**
 *
 * This filter removes every acceleration that happens slowly or
 * steadily (like e.g. gravity). Remember: It _passes_ acceleration
 * with a big variety.
 *
 * @author Benjamin 'BePo' Poppinga
 *
 * TODO integrate this with Tensors
 */
public class VectorFilter {

    private final double factor;
    private double[] prevAcc;

    public VectorFilter() {
        super();
        this.factor = 0.1;
        this.reset();
    }

    public VectorFilter(double factor) {
        super();
        this.factor = factor;
        this.reset();
    }

    public void reset() {
        this.prevAcc = new double[] {0.0, 0.0, 0.0};
    }

    public double[] highpass(double[] vector) {
        prevAcc[0] = vector[0] * this.factor + this.prevAcc[0] * (1.0 - this.factor);
        prevAcc[1] = vector[1] * this.factor + this.prevAcc[1] * (1.0 - this.factor);
        prevAcc[2] = vector[2] * this.factor + this.prevAcc[2] * (1.0 - this.factor);

        var retVal = new double[3];
        retVal[0] = vector[0] - prevAcc[0];
        retVal[1] = vector[1] - prevAcc[1];
        retVal[2] = vector[2] - prevAcc[2];
        
        return retVal;
    }


    public double[] lowpass(double[] vector) {
        var retVal = new double[3];
        retVal[0] = vector[0] * this.factor + this.prevAcc[0] * (1.0 - this.factor);
        retVal[1] = vector[1] * this.factor + this.prevAcc[1] * (1.0 - this.factor);
        retVal[2] = vector[2] * this.factor + this.prevAcc[2] * (1.0 - this.factor);
        this.prevAcc = retVal;
        return retVal;
    }
}