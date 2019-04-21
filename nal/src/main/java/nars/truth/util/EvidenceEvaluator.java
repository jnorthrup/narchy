package nars.truth.util;

import jcog.math.LongFloatTrapezoidalIntegrator;
import jcog.math.LongInterval;
import nars.Param;

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
        public double y(long when) {
            return evi;
        }

    }

    private static class TemporalPointEvidenceEvaluator extends EvidenceEvaluator {
        final long s;
        final int dur;
        /**
         * max evidence during defined range
         */
        private final double evi;

        TemporalPointEvidenceEvaluator(long w, double evi, int dur) {
            this.dur = dur;
            this.s = w;
            assert (w != LongInterval.ETERNAL);
            this.evi = evi;
        }

        @Override
        public double y(long when) {
            long dt = dt(when);
            return (dt == 0) ?
                    evi : ((dur > 0) ? Param.evi(evi, dt, dur) : 0 /* none */);
        }

        long dt(long when) {
            return Math.abs(when - s);
        }

    }

    static final class TemporalSpanEvidenceEvaluator extends TemporalPointEvidenceEvaluator {
        final long e;

        TemporalSpanEvidenceEvaluator(long s, long e, double evi, int dur) {
            super(s, evi, dur);
            this.e = e;
        }

        @Override
        protected long dt(long when) {
            return LongInterval.minTimeTo(when, s, e);
        }


    }


    public static EvidenceEvaluator of(long s, long e, double evi, int dur) {
        return s == e ?
                new TemporalPointEvidenceEvaluator(s, evi, dur) :
                new TemporalSpanEvidenceEvaluator(s, e, evi, dur);
    }


}
