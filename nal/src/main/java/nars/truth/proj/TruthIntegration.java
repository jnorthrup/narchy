package nars.truth.proj;


import jcog.math.LongInterval;
import nars.Task;
import nars.truth.util.EvidenceEvaluator;

import static nars.time.Tense.ETERNAL;

public class TruthIntegration {

    public static double eviAvg(Task t, int dur) {
        return eviAvg(t, t.start(), t.end(), dur);
    }

    public static double eviAvg(Task t, long start, long end, float dur) {
        long range = start == ETERNAL ? 1 : 1 + (end - start);
        return evi(t, start, end, dur) / range;
    }

    public static double evi(Task t) {
        return evi(t, 0);
    }

    private static double evi(Task t, int dur) {
        return evi(t, t.start(), t.end(), dur);
    }



    /**
     * convenience method for selecting evidence integration strategy
     * interval is: [qStart, qEnd], ie: qStart: inclusive qEnd: inclusive
     * if qStart==qEnd then it is a point sample
     */
    public static double evi(Task t, long qStart, long qEnd, float dur) {

        assert(qStart != ETERNAL && qStart <= qEnd);

        if (qStart == qEnd) {
            return t.evi(qStart, dur); //point
        } else {
            //range
            long tStart = t.start();
            double evi = t.evi();
            if (tStart == ETERNAL) {
                //eternal task
                long range = (qEnd - qStart + 1);
                return evi * range;
            } else {
                return eviIntegrate(evi, dur, qStart, qEnd, tStart, t.end()); //temporal task
            }
        }
    }

    private static double eviIntegrate(double evi, float dur, long qStart, long qEnd, long tStart, long tEnd) {

        EvidenceEvaluator ee = EvidenceEvaluator.of(tStart, tEnd, evi, dur);

        if (tStart <= qStart && tEnd >= qEnd) {

            //task contains question
            return ee.integrate2(qStart, qEnd);

            //return eviInteg(t, dur, qStart, qEnd);
            //return (qEnd - qStart) * t.evi(); //fast, assumes task evi is uniform between the end-points:
         } else if (LongInterval.intersectsRaw(tStart, tEnd, qStart, qEnd)) {

            if (qStart <= tStart && qEnd >= tEnd) {
                //question contains task

//                if (qStart == tStart) {
//                    //HACK collapse point
//                    return LongFloatTrapezoidalIntegrator.sumSort(ee,
//                           qStart,
//                            tStart, tEnd,   //internal to task.  supersample not necessary unless task is not uniform
//                            Math.min(tEnd + 1, qEnd),//task edge supersample
//                            (tEnd + qEnd) / 2, //supersample
//                            qEnd);
//
//                } else {
                //supersample
                //task rising edge supersample
                //internal to task.  supersample not necessary unless task is not uniform
                //task falling edge supersample
                //supersample
                return ee.integrateN(qStart, Math.min(qStart, (qStart + tStart) / 2), Math.max(qStart, tStart - 1), tStart, tEnd, Math.min(tEnd + 1, qEnd), Math.max(qEnd, (tEnd + qEnd) / 2), qEnd);
                //                }

            } else {
                //MESSY INTERSECTION
                if (qStart <= tStart && qEnd >= tEnd) {
                    //ends before the task
                    //supersample
                    //task rising edge supersample
                    return ee.integrateN(qStart, (qStart+tStart)/2, Math.max(qStart, tStart - 1), tStart, qEnd);

                } else if (qStart >= tStart && qEnd <= tEnd) {
                    //starts before the task and ends in it
                    //supersample
                    //task rising edge supersample
                    return ee.integrateN(qStart, (qStart+tStart)/2, Math.max(qStart, tStart - 1), tStart, qEnd);
                } else if (qStart >= tStart && qStart <= tEnd && qEnd >= tEnd) {
                    //tstart, qstart, qend
                    //finishes after the task
                    //task falling edge supersample
                    //supersample
                    return ee.integrateN(qStart, tEnd, Math.min(tEnd + 1, qEnd), Math.max(qEnd, (tEnd+qEnd)/2), qEnd);
                } else { //if (tStart >= qStart && qEnd <= tEnd) {
                    assert(tStart >= qStart && qEnd <= tEnd);
                    //qStart, tstart, qend
                    //supersample
                    return ee.integrateN(qStart, (qStart+tStart)/2, tStart, qEnd);
                } /*else
                    throw new WTF();*/


            }
        } else {
            //DISJOINT

            //entirely before, or after
            //return eviInteg(t, dur, qStart, qEnd);

            //supersample
            return ee.integrate3(qStart, (qStart + qEnd) / 2, qEnd);
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
