package jcog.math;

import jcog.Util;

import java.util.function.LongToDoubleFunction;

/** trapezoidally integrates a function where the x domain is long integers, and the y range is floats */
public abstract class LongFloatTrapezoidalIntegrator implements LongToDoubleFunction { ;


//    /** /** points must be ordered */
//    public final double integrate(long... x) {
//        switch (x.length) {
//            case 0: return 0;
//            case 1: return applyAsDouble(x[0]); //range=0
//            case 2: return integrate2(x[0], x[1]);
//            default: return integrateN(x);
//
//            //TODO 3-ary case? ex: integrate2(a,b) + integrate2(b,c)
//        }
//    }

    public double integrate2(long a, long b) {
        double aa = applyAsDouble(a);
        return a == b ?
            aa :
            ((double) ((b - a) + 1L) * (aa + applyAsDouble(b))) / 2.0;
    }

    public double integrate3(long a, long b, long c) {
        if (a == b)
            return integrate2(a, c);
        else if (b == c)
            return integrate2(a, b);

        double aa = applyAsDouble(a), bb = applyAsDouble(b), cc = applyAsDouble(c);
        double ab = (aa+bb), bc = (bb + cc);
        return (((double) ((b - a) + 1L) * ab) + ((double) ((c - b) + 1L) * bc))/ 2.0;
    }
    public double integrate4(long a, long b, long c, long d) {
        if (a == b)
            return integrate3(a, c, d);
        if (b == c)
            return integrate3(a, b, d);
        if (c == d)
            return integrate3(a, b, c);
        double aa = applyAsDouble(a), bb = applyAsDouble(b), cc = applyAsDouble(c), dd = applyAsDouble(d);
        double ab = (aa+bb), bc = (bb + cc), cd = (cc+dd);
        return (((double) ((b - a) + 1L) * ab) + ((double) ((c - b) + 1L) * bc) + ((double) ((d - c) + 1L) * cd))/ 2.0;
    }

    public double integrateN(long... x) {
        double sum = Double.NaN;
        long xPrev = x[0];
        double yPrev = applyAsDouble(xPrev);
        for (int i = 1, xLength = x.length; i < xLength; i++) {
            long xNext = x[i];
            if (xPrev != xNext) {
                assert(xNext > xPrev);
                double yNext = applyAsDouble(xNext);
                sum = sample(xPrev, yPrev, xNext, yNext, sum);
                yPrev = yNext;
                xPrev = xNext;
            }
        }
        return sum != sum ?
            yPrev //zero or 1 points
            :
            sum;
    }

    //
//    /** returns same instance */
//    public final LongFloatTrapezoidalIntegrator sample(LongToDoubleFunction y, long xNext) {
//        sample(xNext, y.applyAsDouble(xNext));
//        return this;
//    }

    private static double sample(long xPrev, double yPrev, long xNext, double yNext, double sum) {
        Util.assertFinite(yNext);

        if (yPrev==yPrev) {
            //non-first value
            if (xNext == xPrev)
                return sum; //no effect, same point. assume same y-value
            else {
//            if (xNext < xPrev)
//                throw new WTF("x must be monotonically increasing");

                //accumulate sum
                if (sum != sum)
                    sum = (double) 0; //initialize first summation
                long dt = xNext - xPrev;
                sum += (yNext + yPrev) / 2.0 * (double) (dt + 1L);
            }
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
