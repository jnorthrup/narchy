package nars.truth.polation;


import jcog.math.Longerval;
import nars.Task;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList;

import java.util.Arrays;

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
        long range = (qEnd - qStart + 1);
        if (tStart == ETERNAL) {
            return t.evi() * range;
        }

        long tEnd = t.end();
        if (
                (qStart >= tStart && qEnd <= tEnd)  //contained
                ||
                (qStart <= tStart && qEnd >= tEnd) //disjoint
            )
        {
            //simple 1-component trapezoid
            return t.eviIntegTrapezoidal(dur, qStart, qEnd);
        }


        //two remaining cases:
        //  a) intersects the task and hangs before or after it
        //  b) contains the entire task

        
        long[] qt = Longerval.intersectionArray(qStart, qEnd, tStart, tEnd);
        //piecewise trapezoid.  TODO optimize this
        {

            TempLongArrayList pp = new TempLongArrayList(/*(mid ? 1 : 0) + (qt == null ? 2 : 4)*/6);

            pp.add(qStart);

//            if (mid)
//                pp.add((qStart + qEnd) / 2L); //mid

            if (qt != null) {
                //inner points
                long qta = qt[0];
                if (qta > qStart && qta < qEnd) //quick test to avoid set add
                    pp.add(qta);
                /*else */{
                    //pp.add((qta + ((qStart + qEnd) / 2L)) / 2L);
                    long before = qta - Math.max(1, dur);
                    if (before > qStart)
                        pp.add(before); //right before qta
                }

                long qtb = qt[1];
                if (qta != qtb && qtb > qStart && qtb < qEnd)  //quick test to avoid set add
                    pp.add(qtb);
                /*else*/ {
                    //pp.add((qtb + ((qStart + qEnd) / 2L)) / 2L);
                    long after = qtb + Math.max(1, dur);
                    if (after < qEnd)
                        pp.add(after); //right after qtb
                }
            }


            pp.add(qEnd);

            return t.eviIntegTrapezoidal(dur, pp.toSortedArray());
        }

        //return x.eviIntegRectMid(dur, points);


    }

    private static final class TempLongArrayList extends LongArrayList {

        public TempLongArrayList(int cap) {
            items = new long[cap];
        }

        @Override
        public boolean add(long newItem) {
            int size = this.size;
            if (size > 0 && items[size-1] == newItem)
                return true; //equal to the last value
            return super.add(newItem);
        }

        @Override
        public long[] toArray() {
            int size = this.size;
            if (size == 0)
                return ArrayUtils.EMPTY_LONG_ARRAY;

            long[] x = items;
            if (x.length == size)
                return x;

            return Arrays.copyOf(x, size);
        }

        /** the input is likely already sorted so do a few extra comparisons to avoid a sort() */
        @Override public long[] toSortedArray() {
            long[] array = this.toArray();
            switch (array.length) {
                case 0:
                case 1:
                    return array;
                case 2:
                    if (array[0] > array[1]) {
                        long x = array[0];
                        array[0] = array[1];
                        array[1] = x;
                    }
                    return array;
                case 3:
                    if (array[0] <= array[1] && array[1] <= array[2])
                        return array;
                    break;
                case 4:
                    if (array[0] <= array[1] && array[1] <= array[2] && array[2] <= array[3])
                        return array;
                    break;
                default:
                    break;
            }
            //Arrays.sort(array);
            ArrayUtils.sort(array, (l)->-l);
            return array;
        }
    }
}
