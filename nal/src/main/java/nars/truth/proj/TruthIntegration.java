package nars.truth.proj;


import jcog.math.LongInterval;
import nars.Task;
import nars.truth.func.TruthFunctions;
import nars.truth.util.EvidenceEvaluator;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static nars.time.Tense.ETERNAL;

public class TruthIntegration {



	public static double eviAvg(Task t, long start, long end, float dur, boolean eternalize) {
		assert(start!=ETERNAL);
		long range = 1 + (end - start);
		return eviAbsolute(t, start, end, dur, eternalize) / range;
	}

	public static double evi(Task t) {
		return eviAbsolute(t, t.start(), t.end(), 0, false);
	}


	/**
	 * convenience method for selecting evidence integration strategy
	 * interval is: [qStart, qEnd], ie: qStart: inclusive qEnd: inclusive
	 * if qStart==qEnd then it is a point sample
	 */
	static double eviAbsolute(Task t, long qStart, long qEnd, float dur, boolean eternalize) {

		//assert (qStart != ETERNAL && qStart <= qEnd);

		long tStart = t.start();
		double factor;
		if (qStart == qEnd) {
			//point
			if (tStart == ETERNAL)
				//result = new EvidenceEvaluator.EternalEvidenceEvaluator(ee);
				factor = 1;
			else {
				assert(qStart != LongInterval.ETERNAL);
				factor = EvidenceEvaluator.of(tStart, t.end(), dur).applyAsDouble(qStart);
			}
		} else {
			//range
			if (tStart == ETERNAL) {
				//eternal task
				factor = (qEnd - qStart + 1);
			} else {
				factor = eviIntegrate(qStart, qEnd, tStart, t.end(), dur);
			}
		}

		double e = t.evi();
		if (!eternalize) {
			return e * factor;
		} else {
			double ee = TruthFunctions.eternalize(e);
			return (ee * (qEnd - qStart + 1)) + (e-ee) * factor;
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
		long range = (qEnd - qStart + 1);
		long tStart = t.start();
		double tEvi = t.evi();
		return tStart == ETERNAL ?
			tEvi * range
			:
			tEvi * Math.min(range, t.range()) / (1 + t.minTimeTo(qStart, qEnd));
	}

	/** for ranking relative relevance of tasks with respect to a time point */
	public static double eviFast(Task t, long now) {
		return t.range() * t.evi() / (1 + Math.max(now - t.start(), now - t.end())); //penalize long tasks even if they surround now evenly
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
	   } else if (qs >= ts && qe > te) {
		   //question starts during and ends after task
		   //return e.integrateN(qs, te, Math.min(te + 1, qe), (te + qe) / 2, qe);
		   //return e.integrateN(qs, te, (te + qe) / 2, qe);
		   return e.integrateN(qs, te, Math.min(te+1, qe), qe);
	   } else if (qs < ts && qe <= te) {
		   //question starts before task and ends during task
		   return e.integrateN(qs, Math.max(qs, ts-1), ts, qe);
	   } else {
//			assert(qs <= ts && qe >= te);

		   //question surrounds task
		   return e.integrateN(
			   qs, Math.max(qs, ts - 1),
			   ts, te,
			   Math.min(te + 1, qe), qe);
	   }
	}

//    private static float eviInteg(Task t, int dur, long... when) {
//        if (when.length == 0)
//            return when[0];
//
//        //assumes when[] is sorted
//        if (Param.DEBUG)
//            assert(ArrayUtils.isSorted(when));
//        //Arrays.sort(when);
//
//        EvidenceEvaluator ee = t.eviEvaluator();
//        return LongFloatTrapezoidalIntegrator.sum(when, w->ee.evi(w,dur));
////        }
////        return eviIntegTrapezoidal(t, dur,
////                Param.TRUTH_INTEGRATION_SUPERSAMPLING==1 ? supersample1(when) : when);
//    }
//
//    /** also deduplicates */
//    @Deprecated private static long[] supersample1(long[] whenSorted) {
//        int n = whenSorted.length;
//        long range = whenSorted[n - 1] - whenSorted[0];
//        if (range <= whenSorted.length)
//            return whenSorted; //no net change from start to end
//
//        //TODO special 2-element case
//
//        LongArrayList l = new LongArrayList(whenSorted);
//        LongListIterator ll = l.listIterator();
//        long prev = ll.nextLong();
//        boolean changed = false;
//        while (ll.hasNext()) {
//            long next = ll.nextLong();
//            long delta = next - prev;
//            if (delta == 0)
//                ll.remove();
//            else if (delta >= 2) {
//                long mid = prev + delta/2;
//                ll.set(mid);
//                ll.add(next);
//                prev = next;
//                changed = true;
//            }
//
//        }
//
//        return changed ? l.toLongArray() : whenSorted;
//    }


//    static float eviIntegTrapezoidal(Task t, int dur, long a, long b) {
//        float[] eab = t.eviBatch(dur, a, b);
//        float ea = eab[0], eb = eab[1];
//        return (ea + eb) / 2 * (b - a + 1);
//    }
//
//    static float eviIntegTrapezoidal(Task t, int dur, long a, long b, long c) {
//        float[] e = t.eviBatch(dur, a, b, c);
//        float ea = e[0], eb = e[1], ec = e[2];
//        return ((ea + eb) / 2 * (b - a + 1)) + ((eb + ec) / 2 * (c - b + 1));
//    }
//
//    static float eviIntegTrapezoidal(Task t, int dur, long a, long b, long c, long d) {
//        float[] e = t.eviBatch(dur, a, b, c, d);
//        float ea = e[0], eb = e[1], ec = e[2], ed = e[3];
//        return ((ea + eb) / 2 * (b - a + 1)) + ((eb + ec) / 2 * (c - b + 1)) + ((ec + ed) / 2 * (d - c + 1));
//    }

//


//    private static final class TempLongArrayList extends LongArrayList {
//
//        public TempLongArrayList(int cap) {
//            items = new long[cap];
//        }
//
//        @Override
//        public boolean addAt(long newItem) {
//            int size = this.size;
//            if (size > 0 && items[size-1] == newItem)
//                return true; //equal to the last value
//            return super.addAt(newItem);
//        }
//
//        @Override
//        public long[] toArray() {
//            int size = this.size;
//            if (size == 0)
//                return ArrayUtils.EMPTY_LONG_ARRAY;
//
//            long[] x = items;
//            if (x.length == size)
//                return x;
//
//            return Arrays.copyOf(x, size);
//        }
//
//        /** the input is likely already sorted so do a few extra comparisons to avoid a sort() */
//        @Override public long[] toSortedArray() {
//            long[] array = this.toArray();
//            switch (array.length) {
//                case 0:
//                case 1:
//                    return array;
//                case 2:
//                    if (array[0] > array[1]) {
//                        long x = array[0];
//                        array[0] = array[1];
//                        array[1] = x;
//                    }
//                    return array;
//                case 3:
//                    if (array[0] <= array[1] && array[1] <= array[2])
//                        return array;
//                    break;
//                case 4:
//                    if (array[0] <= array[1] && array[1] <= array[2] && array[2] <= array[3])
//                        return array;
//                    break;
//                default:
//                    break;
//            }
//            //Arrays.sort(array);
//            ArrayUtils.sort(array, (l)->-l);
//            return array;
//        }
//    }
}
