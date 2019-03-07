package nars.truth.polation;


import jcog.WTF;
import jcog.math.LongFloatTrapezoidalIntegrator;
import jcog.math.Longerval;
import nars.Task;
import org.eclipse.collections.api.block.function.primitive.LongToFloatFunction;

import static nars.time.Tense.ETERNAL;

public class TruthIntegration {

    public static float eviAvg(Task t, int dur) {
        return eviAvg(t, t.start(), t.end(), dur);
    }

    public static float eviAvg(Task t, long start, long end, int dur) {
        long range = start == ETERNAL ? 1 : 1 + (end - start);
        return evi(t, start, end, dur) / range;
    }

    public static float evi(Task t) {
        return evi(t, 0);
    }

    public static float evi(Task t, int dur) {
        return evi(t, t.start(), t.end(), dur);
    }

    public static float evi(Task t, long[] startEnd, int dur) {
        return evi(t, startEnd[0], startEnd[1], dur);
    }

    /**
     * convenience method for selecting evidence integration strategy
     * interval is: [qStart, qEnd], ie: qStart: inclusive qEnd: inclusive
     * if qStart==qEnd then it is a point sample
     */
    public static float evi(Task t, long qStart, long qEnd, int dur) {

        assert(qStart != ETERNAL && qStart <= qEnd);

        if (qStart == qEnd) {
            return t.evi(qStart, dur);
        }

        long tStart = t.start();
        if (tStart == ETERNAL) {
            long range = (qEnd - qStart + 1);
            return t.evi() * range;
        }

        long tEnd = t.end();

        LongToFloatFunction ee = t.eviEvaluator().eviFn(dur);
        //TODO: ee.integrate(...)

        //possible optimization, needs tested:
//        if (dur == 0) {
//            //trim the question to the task because dur=0 means no residual evidence is measured outside of the task's bounds
//            qStart = Math.max(qStart, tStart);
//            qEnd = Math.min(qEnd, tEnd);
//            if (qStart >= qEnd)
//                return t.evi(qStart, dur); //reduced to a point
//        }


        if (tStart <= qStart && tEnd >= qEnd) {

            //task contains question
            return LongFloatTrapezoidalIntegrator.sum(ee, qStart, qEnd);

            //return eviInteg(t, dur, qStart, qEnd);
            //return (qEnd - qStart) * t.evi(); //fast, assumes task evi is uniform between the end-points:

        }

        if (Longerval.intersects(tStart, tEnd, qStart, qEnd)) {
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
                    return LongFloatTrapezoidalIntegrator.sum(ee,
                            qStart,
                            Math.min(qStart, (qStart + tStart) / 2), //supersample
                            Math.max(qStart, tStart - 1), //task rising edge supersample
                            tStart, tEnd,   //internal to task.  supersample not necessary unless task is not uniform
                            Math.min(tEnd + 1, qEnd),//task falling edge supersample
                            Math.max(qEnd, (tEnd + qEnd) / 2), //supersample
                            qEnd);
//                }

            } else {
                //MESSY INTERSECTION
                if (qStart <= tStart && qEnd >= tEnd) {
                    //ends before the task
                    return LongFloatTrapezoidalIntegrator.sum(ee,
                    qStart,
                            (qStart+tStart)/2, //supersample
                            Math.max(qStart, tStart - 1), //task rising edge supersample
                            tStart,
                            qEnd
                    );

                } else if (qStart >= tStart && qEnd <= tEnd) {
                    assert(qEnd <= tEnd);
                    //starts before the task and ends in it
                    return LongFloatTrapezoidalIntegrator.sum(ee,
                            qStart,
                            (qStart+tStart)/2, //supersample
                            Math.max(qStart, tStart - 1), //task rising edge supersample
                            tStart,
                            qEnd
                    );
                } else if (qStart >= tStart && qStart <= tEnd && qEnd >= tEnd) {
                    //tstart, qstart, qend
                    //finishes after the task
                    return LongFloatTrapezoidalIntegrator.sum(ee,
                            qStart,
                            tEnd,
                            Math.min(tEnd + 1, qEnd),//task falling edge supersample
                            Math.max(qEnd, (tEnd+qEnd)/2), //supersample
                            qEnd
                    );
                } else if (tStart >= qStart && qEnd <= tEnd) {
                    //qStart, tstart, qend
                    return LongFloatTrapezoidalIntegrator.sum(ee,
                            qStart,
                            (qStart+tStart)/2, //supersample
                            tStart,
                            qEnd
                    );
                } else
                    throw new WTF();


            }
        } else {
            //DISJOINT

            //entirely before, or after
            //return eviInteg(t, dur, qStart, qEnd);

            return LongFloatTrapezoidalIntegrator.sum(ee,
                    qStart,
                    (qStart + qEnd) / 2,   //supersample
                    qEnd);
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
