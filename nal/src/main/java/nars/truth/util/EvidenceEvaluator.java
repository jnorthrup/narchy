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

    private static class TemporalPointEvidenceEvaluator extends EvidenceEvaluator {
        final long s;
        final float dur;
        /**
         * max evidence during defined range
         */
        private final double evi;

        /** //assert (w != LongInterval.ETERNAL); */
        TemporalPointEvidenceEvaluator(long w, double evi, float dur) {
            this.dur = dur;
            this.s = w;
            this.evi = evi;
        }

        @Override
        public final double applyAsDouble(long when) {
            long dt = dt(when);
            return (dt == 0) ?
                    evi : ((dur > 0) ? NAL.evi(evi, dt, dur) : 0 /* none */);
        }

        long dt(long when) {
            return Math.abs(when - s);
        }

    }

    static final class TemporalSpanEvidenceEvaluator extends TemporalPointEvidenceEvaluator {
        final long e;

        TemporalSpanEvidenceEvaluator(long s, long e, double evi, float dur) {
            super(s, evi, dur);
            this.e = e;
        }

        @Override
        protected long dt(long when) {
            return LongInterval.minTimeTo(when, s, e);
        }


    }

    public static double of(long s, long e, double evi, float dur, long when) {
        return of(s, e, evi, dur).applyAsDouble(when);
    }

    public static EvidenceEvaluator of(long s, long e, double evi, float dur) {
        return s == e ?
                new TemporalPointEvidenceEvaluator(s, evi, dur) :
                new TemporalSpanEvidenceEvaluator(s, e, evi, dur);
    }


}
