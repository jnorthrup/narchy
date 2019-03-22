package jcog.math;

import jcog.WTF;

/** trapezoidally integrates a function where the x domain is long integers, and the y range is floats */
abstract public class LongFloatTrapezoidalIntegrator {

    private long xPrev = Long.MIN_VALUE;
    private double yPrev = Float.NaN;
    private double sum = Float.NaN;

    abstract public double y(long x);

//    /** pre-sorts the input array */
//    public static double sumSort(LongToDoubleFunction y, long... xNexts) {
//        if (xNexts.length > 1)
//            Arrays.sort(xNexts);
//        return sum(y, xNexts);
//    }

    /** /** points must be ordered */
    public double integrate(long... x) {

        switch (x.length) {
            case 0:
                return 0;
            case 1:
                return y(x[0]); //range=1
            case 2: {
                long a = x[0], b = x[1];
                if (a == b)
                    return y(a);
                else {
                    long range = b-a;
                    if (range < 0)
                        throw new WTF("x must be monotonically increasing");
                    return ((y(a) + y(b)) / 2.0 * (range + 1));
                }
            }
            //TODO 3-ary case?
            default:
                return sample(x).integrate();
        }

    }

    private final LongFloatTrapezoidalIntegrator sample(long... xNexts) {
        long xPrev = Long.MIN_VALUE;
        for (long xNext : xNexts) {
            if (xPrev == xNext)
                continue;
            sample(xNext, y(xPrev = xNext));
        }
        return this;
    }
//
//    /** returns same instance */
//    public final LongFloatTrapezoidalIntegrator sample(LongToDoubleFunction y, long xNext) {
//        sample(xNext, y.applyAsDouble(xNext));
//        return this;
//    }

    private void sample(long xNext, double yNext) {
        if (!Double.isFinite(yNext))
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
    private double integrate() {
        if (sum != sum) {
            //zero or 1 points
            return yPrev;
        }
        return sum;
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
