package jcog.math;

import jcog.WTF;
import org.eclipse.collections.api.block.function.primitive.LongToFloatFunction;

import java.util.Arrays;

/** trapezoidally integrates a function where the x domain is long integers, and the y range is floats */
public class LongFloatTrapezoidalIntegrator {

    private long xPrev = Long.MIN_VALUE;
    private float yPrev = Float.NaN;
    private double sum = Float.NaN;


    /** pre-sorts the input array */
    public final static float sumSort(LongToFloatFunction y, long... xNexts) {
        if (xNexts.length > 1)
            Arrays.sort(xNexts);
        return sum(y, xNexts);
    }

    public final static float sum(LongToFloatFunction each, long... x) {

        //return new LongFloatTrapezoidalIntegrator().sample(each, x).sum();

        switch (x.length) {
            case 0:
                return 0;
            case 1:
                return each.valueOf(x[0]);
            case 2: {
                long a = x[0], b = x[1];
                if (a == b)
                    return each.valueOf(a);
                else {
                    long range = b-a;
                    if (range < 0)
                        throw new WTF("x must be monotonically increasing");
                    return (float)((each.valueOf(a) + each.valueOf(b)) / 2.0 * (range + 1));
                }
            }
            //TODO 3-ary case?
            default:
                return new LongFloatTrapezoidalIntegrator().sample(each, x).sum();
        }

    }

    public final LongFloatTrapezoidalIntegrator sample(LongToFloatFunction y, long... xNexts) {
        long xPrev = Long.MIN_VALUE;
        for (long xNext : xNexts) {
            if (xPrev == xNext)
                continue;
            sample(xNext, y.valueOf(xPrev = xNext));
        }
        return this;
    }

    /** returns same instance */
    public final LongFloatTrapezoidalIntegrator sample(LongToFloatFunction y, long xNext) {
        sample(xNext, y.valueOf(xNext));
        return this;
    }

    public void sample(long xNext, float yNext) {
        if (!Float.isFinite(yNext))
            throw new WTF("y must be finite");

        boolean first = yPrev!=yPrev;

            //first value
        if (!first) {
            //subsequent value
            if (xNext == xPrev)
                return; //no effect, same point. assume same y-value
            if (xNext < xPrev)
                throw new WTF("x must be monotonically increasing");
        }

        if (!first) {
            //accumulate sum
            if (sum!=sum)
                sum = 0; //initialize first summation
            long dt = xNext - xPrev;
            sum += (yNext + yPrev) / 2.0 * (dt + 1);
        }

        this.xPrev = xNext; this.yPrev = yNext;
    }

    /** returns Float.Nan if empty */
    public float sum() {
        if (sum != sum) {
            //zero or 1 points
            return xPrev;
        }
        return (float) sum;
    }
//        /**
//         //     * https:
//         //     * long[] points needs to be sorted, unique, and not contain any ETERNALs
//         //     * <p>
//         //     * TODO still needs improvement, tested
//         //     */
//    private static float eviIntegTrapezoidal(Task t, int dur, long[] when) {
//
//        int n = when.length;
//        if (n == 1)
//            return t.evi(when[0], dur);
//
//        //assert (n > 1);
//        //assert(superSampling == 0 || superSampling == 1);
//
//        float[] ee = t.eviEvaluator().evi(dur, when);
//        float e = 0;
//        long a = when[0];
//        float eviPrev = ee[0];
//
//        for (int i = 1; i < n; i++) {
//            long b = when[i];
//            long dt = b - a;
//            if (dt == 0)
//                continue;
//            assert (dt > 0);
//
//            float eviNext = ee[i];
//            e += (eviNext + eviPrev) / 2 * (dt + 1);
//
//            eviPrev = eviNext;
//            a = b;
//        }
//
//        return e;
//    }
}
