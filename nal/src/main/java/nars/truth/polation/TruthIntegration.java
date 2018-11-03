package nars.truth.polation;


import nars.Task;

import static nars.time.Tense.ETERNAL;

public class TruthIntegration {

    public static float eviAvg(Task t, int dur) {
        return eviAvg(t, t.start(), t.end(), dur);
    }

    public static float eviAvg(Task t, long start, long end, int dur) {
        long range = start==ETERNAL ? 1 : 1 + (end - start);
        return evi(t, start, end, dur) / range;
    }

    public static float evi(Task t) {
        return evi(t, 0);
    }

    public static float evi(Task t, long dur) {
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
    public static float evi(Task t, long qStart, long qEnd, long dur) {
        if (qStart == ETERNAL) {
            return t.isEternal() ? t.evi() : t.eviEternalized();
        }
        if (qStart == qEnd) {
            return t.evi(qStart, dur);
        }

        long tStart = t.start();
        if (tStart == ETERNAL) {
            long range = (qEnd - qStart + 1);
            return t.evi() * range;
        }

        long tEnd = t.end();
        if (dur == 0) {
            //trim the question to the task because dur=0 means no residual evidence is measured outside of the task's bounds
            qStart = Math.max(qStart, tStart);
            qEnd = Math.min(qEnd, tEnd);
            if (qEnd < qStart)
                return 0; //no intersection
            if (qStart == qEnd)
                return t.evi(qStart, dur); //point
        }

        if (
                (qEnd <= tStart) //disjoint before
                ||
                (qStart >= tEnd) //disjoint after
                ||
                (qStart >= tStart && qEnd <= tEnd)  //contained
            ) {
            //task contains question
            if (qStart <= qEnd)
                return t.eviIntegTrapezoidal(dur, qStart, qEnd);
            else
                return t.eviIntegTrapezoidal(dur, qEnd, qStart); //??
        }

        if (qStart <= tStart && qEnd >= tEnd) {
            //question contains task
            return t.eviIntegTrapezoidal(dur, qStart, tStart, tEnd, qEnd);
        }

        //remaining cases:
        //  intersects the task and includes time
        //          a) before
        //          OR
        //          b) after

        if (qStart <= tStart) {
            return t.eviIntegTrapezoidal(dur, qStart, tStart, qEnd);
        } else {
            return t.eviIntegTrapezoidal(dur, qStart, tEnd, qEnd);
        }

    }

//    private static final class TempLongArrayList extends LongArrayList {
//
//        public TempLongArrayList(int cap) {
//            items = new long[cap];
//        }
//
//        @Override
//        public boolean add(long newItem) {
//            int size = this.size;
//            if (size > 0 && items[size-1] == newItem)
//                return true; //equal to the last value
//            return super.add(newItem);
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
