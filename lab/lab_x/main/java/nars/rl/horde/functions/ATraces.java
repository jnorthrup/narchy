package nars.rl.horde.functions;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.OpenMapRealVector;
import org.apache.commons.math3.linear.RealVector;

/**
 * Accumulating traces
 */
public class ATraces implements Traces {
    private static final long serialVersionUID = 6241887723527497111L;
    public static final OpenMapRealVector DefaultPrototype = new OpenMapRealVector(0);
    public static final double DefaultThreshold = 1e-8;

    protected final double threshold;
    protected final RealVector prototype;

    protected RealVector vector;


    public ATraces() {
        this(DefaultPrototype);
    }

    public ATraces(RealVector prototype) {
        this(prototype, DefaultThreshold);
    }

    public ATraces(RealVector prototype, double threshold) {
        this(prototype, threshold, 0);
    }

    protected ATraces(RealVector prototype, double threshold, int size) {
        this.prototype = prototype;
        vector = prototype.copy(); 
        this.threshold = threshold;
    }

    @Override
    public ATraces newTraces(int size) {
        return new ATraces(prototype, threshold, size);
    }

    @Override
    public void update(double lambda, RealVector phi) {
        updateVector(lambda, phi);
        adjustUpdate();
        if (clearRequired(phi, lambda))
            clearBelowThreshold();
        
    }

    protected void adjustUpdate() {
    }

    protected void updateVector(double lambda, RealVector phi) {
        if (vector.getDimension()!=phi.getDimension())
            vector = phi.copy();
        else {
            vector.mapMultiplyToSelf(lambda);
            vector.combineToSelf(1, 1, phi);
        }
    }

    private boolean clearRequired(RealVector phi, double lambda) {
        if (threshold == 0)
            return false;
        if (vector instanceof ArrayRealVector)
            return false;
        return true;
    }

    protected void clearBelowThreshold() {
        throw new RuntimeException("clearBelowThreshold: not implemented yet");
    }






































    @Override
    public RealVector vect() {
        return vector;
    }

    @Override
    public void clear() {
        vector = new OpenMapRealVector();
    }


    public RealVector prototype() {
        return prototype;
    }
}
