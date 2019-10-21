package nars.truth.util;

import jcog.math.LongFloatTrapezoidalIntegrator;
import jcog.math.LongInterval;
import nars.NAL;

/**
 * used for accelerating evidence queries involving a batch of time points
 */
public abstract class EvidenceEvaluator extends LongFloatTrapezoidalIntegrator   {

    public static TemporalDurSpanEvidenceEvaluator of(long s, long e, float dur) {
//        if (dur < Float.MIN_NORMAL) {
//            return s == e ?
//                new TemporalRawPointEvidenceEvaluator(s) :
//                new TemporalRawSpanEvidenceEvaluator(s, e);
//        } else {
//            return s == e ?
//                new TemporalDurPointEvidenceEvaluator(s, dur) :
          return      new TemporalDurSpanEvidenceEvaluator(s, e, dur);
//        }
    }

//    public static final class EternalEvidenceEvaluator extends EvidenceEvaluator {
//
//        private EternalEvidenceEvaluator() {
//        }
//
//        @Override
//        public double applyAsDouble(long when) {
//            return 1;
//        }
//
//    }

    private abstract static class TemporalPointEvidenceEvaluator extends EvidenceEvaluator {
        final long s;


        TemporalPointEvidenceEvaluator(long s) {
            this.s = s;
        }

    }

    /** dur=0 */
    private static class TemporalRawPointEvidenceEvaluator extends TemporalPointEvidenceEvaluator {

        TemporalRawPointEvidenceEvaluator(long w) {
            super(w);
        }

        public long dt(long when) {
            return Math.abs(when - s);
        }

        @Override
        public double applyAsDouble(long when) {
            long dt = dt(when);
            return (dt == 0) ? 1 : 0;
        }

    }

    private static class TemporalDurPointEvidenceEvaluator extends TemporalRawPointEvidenceEvaluator {
        final float dur;

        /** //assert (w != LongInterval.ETERNAL); */
        TemporalDurPointEvidenceEvaluator(long w, float dur) {
            super(w);
            this.dur = dur;
        }

        @Override
        public final double applyAsDouble(long when) {
            long dt = dt(when);
            return (dt == 0) ?
                1
                : dur > Float.MIN_NORMAL ? NAL.evi(1, dt, dur) : 0;
        }

    }
//    private static class TemporalSubjPointEvidenceEvaluator extends TemporalPointEvidenceEvaluator {
//        final long now;
//
//        /** //assert (w != LongInterval.ETERNAL); */
//        TemporalSubjPointEvidenceEvaluator(long w, double evi, long now) {
//            super(w, evi);
//            this.now = now;
//        }
//
//        protected long end() {
//            return s;
//        }
//
//        @Override
//        public final double applyAsDouble(long when) {
//            return NAL.eviRelative(s, end(), evi, when, now);
//        }
//    }
//
//    static final class TemporalSubjSpanEvidenceEvaluator extends TemporalSubjPointEvidenceEvaluator {
//        final long e;
//
//        TemporalSubjSpanEvidenceEvaluator(long s, long e, double evi, long now) {
//            super(s, evi, now);
//            this.e = e;
//        }
//
//        @Override
//        protected long end() {
//            return e;
//        }
//    }

    static final class TemporalRawSpanEvidenceEvaluator extends TemporalRawPointEvidenceEvaluator {
        final long e;

        TemporalRawSpanEvidenceEvaluator(long s, long e) {
            super(s);
            this.e = e;
        }

        @Override
        public long dt(long when) {
            return LongInterval.minTimeTo(when, s, e);
        }
    }

    public static final class TemporalDurSpanEvidenceEvaluator extends TemporalDurPointEvidenceEvaluator {
        final long e;

        TemporalDurSpanEvidenceEvaluator(long s, long e, float dur) {
            super(s, dur);
            this.e = e;
        }

        @Override
        public long dt(long when) {
            return LongInterval.minTimeTo(when, s, e);
        }

    }



//    /** subjective */
//    public static EvidenceEvaluator of(long s, long e, double evi, long now) {
//        return s == e ?
//            new TemporalSubjPointEvidenceEvaluator(s, evi, now) :
//            new TemporalSubjSpanEvidenceEvaluator(s, e, evi, now);
//    }

}
