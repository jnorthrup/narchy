package nars.truth.proj;


import jcog.Util;
import jcog.WTF;
import jcog.math.LongInterval;
import nars.Task;
import nars.truth.func.TruthFunctions;
import nars.truth.util.EvidenceEvaluator;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static nars.time.Tense.ETERNAL;

public enum TruthIntegration {
	;


	public static double evi(Task t) {
		return eviAbsolute(t, t.start(), t.end(), 0, false);
	}


	/**
	 * convenience method for selecting evidence integration strategy
	 * interval is: [qStart, qEnd], ie: qStart: inclusive qEnd: inclusive
	 * if qStart==qEnd then it is a point sample
	 */
	public static double eviAbsolute(Task t, long qStart, long qEnd, float dur, boolean eternalize) {

		//assert (qStart != ETERNAL && qStart <= qEnd);

        long tStart = t.start();
		double factor;
		if (qStart == qEnd) {
			//point
			if (tStart == ETERNAL)
				factor = 1;
			else {
				if (qStart == LongInterval.ETERNAL) {
					if (!eternalize)
						throw new WTF();
					factor = 0; //fully eternalize
				} else
					factor = EvidenceEvaluator.of(tStart, t.end(), dur).applyAsDouble(qStart);
			}
		} else {
			//range
			//eternal task
			factor = tStart == ETERNAL ? qEnd - qStart + 1 : eviIntegrate(qStart, qEnd, tStart, t.end(), dur);
		}

        double e = t.evi();
		if (!eternalize) {
			return e * factor;
		} else {
            double ee = TruthFunctions.eternalize(e);
			return (ee * (qEnd - qStart + 1)) + ((e-ee) * factor);
		}
	}

//	public static double evi(Task t, long qStart, long qEnd, long now) {
//
//		assert (qStart != ETERNAL && qStart <= qEnd);
//
//		if (qStart == qEnd) {
//			return t.eviRelative(qStart, now); //point
//		} else {
//			double evi = t.evi();
//			long tStart = t.start();
//			//temporal task
//			return tStart == ETERNAL ?
//				evi * (qEnd - qStart + 1) :
//				eviIntegrate(evi, now, qStart, qEnd, tStart, t.end());
//		}
//	}
	/**
	 * allows ranking task by projected evidence strength to a target query region, but if temporal, the value is not the actual integrated evidence value but a monotonic approximation
	 */
	public static double eviFast(Task t, long qStart, long qEnd) {
        long tStart = t.start();
        long qRange = qEnd - qStart + 1;
		qRange *= 2; //expansion bubble to rank extra evidence beyond the query range while ensuring fair comprison between temporals and eternals
		return t.evi() * (tStart == ETERNAL ?
			qRange //qRange
			:
			(Math.min(qRange, 1 + t.end() - tStart))) /
				(1.0 + Util.sqr(t.minTimeTo(qStart, qEnd)/((double)qRange)));
			//(Math.min(qRange, 1 + t.end() - tStart)) / (1.0 + t.minTimeTo(qStart, qEnd)));
			//((1 + t.end() - tStart)) / (1.0 + t.meanTimeTo(qStart, qEnd)));
			//Math.min(range, t.range()) / (1 + t.minTimeTo(qStart, qEnd)));
	}

	/** for ranking relative relevance of tasks with respect to a time point */
	public static double eviFast(Task t, long now) {
        long s = t.start();  //assert(s!=ETERNAL);
        long e = t.end();
        double dist = (now >= s && now <= e) ? 0 : Util.mean((double) Math.abs(now - s), Math.abs(now - e));
        long range = 1 + e - s;
		return range * t.evi() / (1 + dist);

		//return (1+e-s) * t.evi() / (1 + Math.max(Math.abs(now - s), Math.abs(now - e))); //penalize long tasks even if they surround now evenly
		///return t.range() * t.evi() / (1 + t.maxTimeTo(now));
		//return NAL.evi(t.range() * t.evi(), t.meanTimeTo(now), 1.0f);
		//return t.range() * t.evi() / (1 + t.meanTimeTo(now));
		//return t.range() * t.evi() / (1 + t.minTimeTo(now));
		//return t.range() * t.evi() / (1 + t.maxTimeTo(now));
		//return Math.sqrt(t.range()) * t.evi() / (1 + t.maxTimeTo(now));
	}

//	private static double eviIntegrate(double evi, long now, long qs, long qe, long ts, long te) {
//		return eviIntegrate(qs, qe, ts, te, EvidenceEvaluator.of(ts, te, evi, now));
//	}

	private static double eviIntegrate(long qs, long qe, long ts, long te, float dur) {
        EvidenceEvaluator e = EvidenceEvaluator.of(ts, te, dur);
		if (max(qs, ts) > min(qe, te)) {
		   //DISJOINT - entirely before, or after //!LongInterval.intersectsRaw(ts, te, qs, qe)) {
		   return e.integrate2(qs, qe);
	   } else if (ts <= qs && te >= qe) {
		   //task equals or contains question
		   return e.integrate2(qs, qe);
	   } else if (qs >= ts /*&& qe > te*/) {
		   //question starts during and ends after task
		   //return e.integrateN(qs, te, Math.min(te + 1, qe), (te + qe) / 2, qe);
		   //return e.integrateN(qs, te, (te + qe) / 2, qe);
		   return e.integrate4(qs, te, Math.min(te+1, qe), qe);
	   } else if (/*qs < ts &&*/ qe <= te) {
		   //question starts before task and ends during task
		   return e.integrate4(qs, Math.max(qs, ts-1), ts, qe);
	   } else {
//			assert(qs <= ts && qe >= te);

		   //question surrounds task
			return e.integrateN(
			   qs, Math.max(qs, ts - 1),
			   ts, te,
			   Math.min(te + 1, qe), qe);
	   }
	}

}
