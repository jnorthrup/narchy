package nars.truth.util;

import jcog.math.LongFloatTrapezoidalIntegrator;
import jcog.math.LongInterval;
import nars.NAL;

/**
 * used for accelerating evidence queries involving a batch of time points
 */
public abstract class EvidenceEvaluator extends LongFloatTrapezoidalIntegrator   {

    public static final class EternalEvidenceEvaluator extends EvidenceEvaluator {
        private final double evi;

        private EternalEvidenceEvaluator(double evi) {
            this.evi = evi;
        }

        @Override
        public double applyAsDouble(long when) {
            return evi;
        }

    }

    abstract private static class TemporalPointEvidenceEvaluator extends EvidenceEvaluator {
        final long s;
        /**
         * max evidence during defined range
         */
        public final double evi;

        TemporalPointEvidenceEvaluator(long s, double evi) {
            this.s = s;
            this.evi = evi;
        }

    }

    /** dur=0 */
    private static class TemporalRawPointEvidenceEvaluator extends TemporalPointEvidenceEvaluator {

        TemporalRawPointEvidenceEvaluator(long w, double evi) {
            super(w, evi);
        }

        public long dt(long when) {
            return Math.abs(when - s);
        }

        @Override
        public double applyAsDouble(long when) {
            long dt = dt(when);
            return (dt == 0) ? evi : 0;
        }

    }

    private static class TemporalDurPointEvidenceEvaluator extends TemporalRawPointEvidenceEvaluator {
        final float dur;

        /** //assert (w != LongInterval.ETERNAL); */
        TemporalDurPointEvidenceEvaluator(long w, double evi, float dur) {
            super(w, evi);
            this.dur = dur;
        }

        @Override
        public final double applyAsDouble(long when) {
            long dt = dt(when);
            return (dt == 0) ?
                    evi : NAL.evi(evi, dt, dur);
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

        TemporalRawSpanEvidenceEvaluator(long s, long e, double evi) {
            super(s, evi);
            this.e = e;
        }

        @Override
        public long dt(long when) {
            return LongInterval.minTimeTo(when, s, e);
        }
    }

    static final class TemporalDurSpanEvidenceEvaluator extends TemporalDurPointEvidenceEvaluator {
        final long e;

        TemporalDurSpanEvidenceEvaluator(long s, long e, double evi, float dur) {
            super(s, evi, dur);
            this.e = e;
        }

        @Override
        public long dt(long when) {
            return LongInterval.minTimeTo(when, s, e);
        }

    }


    public static double of(long s, long e, double evi, float dur, long when) {
        return of(s, e, evi, dur).applyAsDouble(when);
    }

    public static EvidenceEvaluator of(long s, long e, double evi, float dur) {
        if (dur < Float.MIN_NORMAL) {
            return s == e ?
                    new TemporalRawPointEvidenceEvaluator(s, evi) :
                    new TemporalRawSpanEvidenceEvaluator(s, e, evi);
        } else {
            return s == e ?
                    new TemporalDurPointEvidenceEvaluator(s, evi, dur) :
                    new TemporalDurSpanEvidenceEvaluator(s, e, evi, dur);
        }
    }

//    /** subjective */
//    public static EvidenceEvaluator of(long s, long e, double evi, long now) {
//        return s == e ?
//            new TemporalSubjPointEvidenceEvaluator(s, evi, now) :
//            new TemporalSubjSpanEvidenceEvaluator(s, e, evi, now);
//    }

}
