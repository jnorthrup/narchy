package jcog.meter.event;

import jcog.util.AtomicFloat;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;

import static java.lang.Float.floatToIntBits;

/**
 * count and sum are not kept perfectly synchronized on value addition.
 * but each commit they are.
 * the count is incremented prior to the sum so any result will be at worst an underestimate.
 */
public class AtomicFloatGuage extends AtomicFloat implements FloatProcedure {

    private volatile int count = 0;

    public final String id;
    float sum = 0, mean = 0;

    public AtomicFloatGuage(String id) {
        super(0f);
        this.id = id;
    }

    public float getSum() {
        return sum;
    }
    public float getMean() {
        return mean;
    }

    static final int ZERO = floatToIntBits(0);

    /** records current values and clears for a new cycle */
    public void commit() {
        int c = this.count;
        if (c > 0) {
            this.mean = (this.sum = Float.intBitsToFloat(getAndUpdate((v) -> {
                count = 0;
                return ZERO;
            }))) / c;
        } else {
            this.mean = Float.NaN;
        }
    }

    public void accept(float v) {
        count++;
        addAndGet(v);
    }

    @Override
    public final void value(float v) {
        accept(v);
    }
}
