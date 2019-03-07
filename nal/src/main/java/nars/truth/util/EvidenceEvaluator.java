package nars.truth.util;

import jcog.math.LongInterval;
import nars.Param;
import nars.Task;

import static nars.time.Tense.ETERNAL;

/**
 * used for accelerating evidence queries involving a batch of time points
 */
public interface EvidenceEvaluator {


    float evi(long when, int dur);

    default /* final */ float[] evi(int dur, long... when) {
        return evi(dur, 0, when.length, when);
    }
    default float[] evi(int dur, int arrayFrom, int arrayTo, long[] when) {
        int n = arrayTo-arrayFrom;
        float[] e = new float[n];
        for (int i = 0; i < n; i++) {
            e[i] = evi(when[i + arrayFrom], dur);
        }
        return e;
    }

    final class EternalEvidenceEvaluator implements EvidenceEvaluator {
        private final float evi;

        private EternalEvidenceEvaluator(float evi) {
            this.evi = evi;
        }

        @Override
        public float evi(long when, int dur) {
            return evi;
        }

    }

    class TemporalPointEvidenceEvaluator implements EvidenceEvaluator {
        public final long s;

        /**
         * max evidence during defined range
         */
        private final float ee;

        protected long dt(long when) {
            return Math.abs(when - s);
        }

        protected TemporalPointEvidenceEvaluator(long s, float ee) {
            this.s = s;
            assert (s != LongInterval.ETERNAL);
            this.ee = ee;
        }

        @Override
        public final float evi(long when, int dur) {
            long dt = dt(when);
            return (dt == 0) ?
                    ee : ((dur != 0) ? Param.evi(ee, dt, dur) : 0);
        }
    }

    class TemporalSpanEvidenceEvaluator extends TemporalPointEvidenceEvaluator {
        public final long e;


        public TemporalSpanEvidenceEvaluator(long s, long e, float ee) {
            super(s, ee);
            this.e = e;
        }

        @Override
        protected long dt(long when) {
            return LongInterval.minTimeOutside(when, s, e);
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
        float ee = t.evi();
        if (s == ETERNAL)
            return new EternalEvidenceEvaluator(ee);

        long e = t.end();
        if (s == e)
            return new TemporalPointEvidenceEvaluator(s, ee);
        else
            return new TemporalSpanEvidenceEvaluator(s, e, ee);
    }

}
