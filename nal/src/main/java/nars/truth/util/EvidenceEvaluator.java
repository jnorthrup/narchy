package nars.truth.util;

import jcog.math.LongFloatTrapezoidalIntegrator;
import jcog.math.LongInterval;
import nars.Param;
import nars.Task;

import java.util.function.LongToDoubleFunction;

import static nars.time.Tense.ETERNAL;

/**
 * used for accelerating evidence queries involving a batch of time points
 */
public abstract class EvidenceEvaluator implements LongToDoubleFunction /* time to evidence */  {


    public final double evi(long when) {
        return applyAsDouble(when);
    }

    public final double[] evi(long... when) {
        return evi(0, when.length, when);
    }

    public double[] evi(int arrayFrom, int arrayTo, long[] when) {
        int n = arrayTo-arrayFrom;
        double[] e = new double[n];
        for (int i = 0; i < n; i++) {
            e[i] = applyAsDouble(when[i + arrayFrom]);
        }
        return e;
    }

    /** points must be ordered */
    public double integrate(long... points) {
        return LongFloatTrapezoidalIntegrator.sum(this, points);
    }

    static final class EternalEvidenceEvaluator extends EvidenceEvaluator {
        private final double evi;

        private EternalEvidenceEvaluator(double evi) {
            this.evi = evi;
        }

        @Override
        public double applyAsDouble(long when) {
            return evi;
        }

    }

    static class TemporalPointEvidenceEvaluator extends EvidenceEvaluator {
        public final long s;
        public final int dur;
        /**
         * max evidence during defined range
         */
        private final double evi;

        protected long dt(long when) {
            return Math.abs(when - s);
        }

        protected TemporalPointEvidenceEvaluator(long w, double evi, int dur) {
            this.dur = dur;
            this.s = w;
            assert (w != LongInterval.ETERNAL);
            this.evi = evi;
        }

        @Override
        public double applyAsDouble(long when) {
            long dt = dt(when);
            return (dt == 0) ?
                    evi : ((dur > 0) ? Param.evi(evi, dt, dur) : 0 /* none */);
        }
    }

    static final class TemporalSpanEvidenceEvaluator extends TemporalPointEvidenceEvaluator {
        public final long e;


        public TemporalSpanEvidenceEvaluator(long s, long e, double evi, int dur) {
            super(s, evi, dur);
            this.e = e;
        }

        @Override
        protected long dt(long when) {
            return LongInterval.minTimeTo(when, s, e);
        }


//                int n = when.length;
//                float[] e = new float[n];
//                long ts = start(), te = end(); //cache these here to avoid repeat access through LongInterval.minTimeTo
        //long wPre = Long.MIN_VALUE;
        //for (int i = 0; i < n; i++) {
//                    long w = when[i]; assert(w!=ETERNAL && w!=TIMELESS);
//                    assert(wPre <= w);

//                    if (i <= 1 || w != wPre) {
//                        long dt = LongInterval.minTimeOutside(when, s, e);
//                        return (dt == 0) ?
//                                ee : ((dur != 0) ? Param.evi(ee, dt, dur) : 0);
        //wPre = w;

//                    } else {
//                        e[i] = e[i-1]; //copy a repeat value
//                    }

//                }

//                return e;

    }


    public static EvidenceEvaluator the(Task t, int dur) {
        long s = t.start();
        double ee = t.evi();
        if (s == ETERNAL)
            return new EternalEvidenceEvaluator(ee);

        long e = t.end();
        if (s == e)
            return new TemporalPointEvidenceEvaluator(s, ee, dur);
        else
            return new TemporalSpanEvidenceEvaluator(s, e, ee, dur);
    }

}
