package nars.truth.polation;


import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongListIterator;
import jcog.WTF;
import jcog.math.Longerval;
import jcog.util.ArrayUtils;
import nars.Param;
import nars.Task;

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

        if (!(qStart != ETERNAL && qStart <= qEnd))
            throw new WTF();

        if (qStart == qEnd) {
            return t.evi(qStart, dur);
        }

        long tStart = t.start();
        if (tStart == ETERNAL) {
            long range = (qEnd - qStart + 1);
            return t.evi() * range;
        }

        long tEnd = t.end();

        //possible optimization, needs tested:
//        if (dur == 0) {
//            //trim the question to the task because dur=0 means no residual evidence is measured outside of the task's bounds
//            qStart = Math.max(qStart, tStart);
//            qEnd = Math.min(qEnd, tEnd);
//            if (qStart >= qEnd)
//                return t.evi(qStart, dur); //reduced to a point
//        }

        if (Longerval.intersects(tStart, tEnd, qStart, qEnd)) {
            if (qStart <= tStart && qEnd >= tEnd) {
                //question contains task
                return eviInteg(t, dur,
                        qStart,
                        tStart, tEnd,
                        qEnd);

            } else if (tStart <= qStart && tEnd >= qEnd) {
                //task contains question
                return eviInteg(t, dur, qStart, qEnd); //assumes task evi is uniform
            } else {

                if (qStart <= tStart) {

                    //before and during
                    return eviInteg(t, dur, qStart, tStart, qEnd);


                } else { //if (qEnd >= tEnd) {

                    assert(qEnd >= tEnd);

                    //during and after
                    return eviInteg(t, dur, qStart, tEnd, qEnd);

                }
            }
        } else {
            //disjoint


            //entirely before, or after
            return eviInteg(t, dur, qStart, qEnd);
        }
    }

    private static float eviInteg(Task t, int dur, long... when) {
        //assumes when[] is sorted
        if (Param.DEBUG)
            assert(ArrayUtils.isSorted(when));
        //Arrays.sort(when);
        return eviIntegTrapezoidal(t, dur, Param.TRUTH_INTEGRATION_SUPERSAMPLING==1 ? supersample1(when) : when);
    }

    /** also deduplicates */
    private static long[] supersample1(long[] whenSorted) {
        int n = whenSorted.length;
        long range = whenSorted[n - 1] - whenSorted[0];
        if (range <= whenSorted.length)
            return whenSorted; //no net change from start to end

        //TODO special 2-element case

        LongArrayList l = new LongArrayList(whenSorted);
        LongListIterator ll = l.listIterator();
        long prev = ll.nextLong();
        boolean changed = false;
        while (ll.hasNext()) {
            long next = ll.nextLong();
            long delta = next - prev;
            if (delta == 0)
                ll.remove();
            else if (delta >= 2) {
                long mid = prev + delta/2;
                ll.set(mid);
                ll.add(next);
                prev = next;
                changed = true;
            }

        }

        return changed ? l.toLongArray() : whenSorted;
    }


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

    /**
     * https:
     * long[] points needs to be sorted, unique, and not contain any ETERNALs
     * <p>
     * TODO still needs improvement, tested
     */
    private static float eviIntegTrapezoidal(Task t, int dur, long[] when) {

        int n = when.length;
        if (n == 1)
            return t.evi(when[0], dur);

        //assert (n > 1);
        //assert(superSampling == 0 || superSampling == 1);

        float[] ee = t.eviBatch(dur, when);
        float e = 0;
        long a = when[0];
        float eviPrev = ee[0];

        for (int i = 1; i < n; i++) {
            long b = when[i];
            long dt = b - a;
            if (dt == 0)
                continue;
            assert (dt > 0);

            float eviNext = ee[i];
            e += (eviNext + eviPrev) / 2 * (dt + 1);

            eviPrev = eviNext;
            a = b;
        }

        return e;
    }

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
