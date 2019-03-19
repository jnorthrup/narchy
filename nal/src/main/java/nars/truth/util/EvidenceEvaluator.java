package nars.truth.util;

import jcog.math.LongInterval;
import nars.Param;
import nars.Task;
import org.eclipse.collections.api.block.function.primitive.LongToDoubleFunction;

import static nars.time.Tense.ETERNAL;

/**
 * used for accelerating evidence queries involving a batch of time points
 */
public interface EvidenceEvaluator {


    double evi(long when, int dur);

    default /* final */ double[] evi(int dur, long... when) {
        return evi(dur, 0, when.length, when);
    }

    default double[] evi(int dur, int arrayFrom, int arrayTo, long[] when) {
        int n = arrayTo-arrayFrom;
        double[] e = new double[n];
        for (int i = 0; i < n; i++) {
            e[i] = evi(when[i + arrayFrom], dur);
        }
        return e;
    }

    default LongToDoubleFunction eviFn(int dur) {
        return w->evi(w,dur);
    }

    final class EternalEvidenceEvaluator implements EvidenceEvaluator {
        private final double evi;

        private EternalEvidenceEvaluator(double evi) {
            this.evi = evi;
        }

        @Override
        public double evi(long when, int dur) {
            return evi;
        }

    }

    class TemporalPointEvidenceEvaluator implements EvidenceEvaluator {
        public final long s;

        /**
         * max evidence during defined range
         */
        private final double ee;

        protected long dt(long when) {
            return Math.abs(when - s);
        }

        protected TemporalPointEvidenceEvaluator(long s, double ee) {
            this.s = s;
            assert (s != LongInterval.ETERNAL);
            this.ee = ee;
        }

        @Override
        public final double evi(long when, int dur) {
            long dt = dt(when);
            return (dt == 0) ?
                    ee : ((dur != 0) ? Param.evi(ee, dt, dur) : 0);
        }
    }

    final class TemporalSpanEvidenceEvaluator extends TemporalPointEvidenceEvaluator {
        public final long e;


        public TemporalSpanEvidenceEvaluator(long s, long e, double ee) {
            super(s, ee);
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


    static EvidenceEvaluator the(Task t) {
        long s = t.start();
        double ee = t.evi();
        if (s == ETERNAL)
            return new EternalEvidenceEvaluator(ee);

        long e = t.end();
        if (s == e)
            return new TemporalPointEvidenceEvaluator(s, ee);
        else
            return new TemporalSpanEvidenceEvaluator(s, e, ee);
    }

}
