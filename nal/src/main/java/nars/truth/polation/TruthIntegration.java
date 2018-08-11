package nars.truth.polation;

import jcog.math.Longerval;
import nars.Task;
import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;
import org.jetbrains.annotations.Nullable;

import static nars.time.Tense.ETERNAL;

public class TruthIntegration {

    public static float eviAvg(Task t, int dur) {
        return eviAvg(t, t.start(), t.end(), dur);
    }

    public static float eviAvg(Task t, long start, long end, int dur) {
        if (start == end) {
            return t.evi(start, dur); //point sample
        } else {
            return eviInteg(t, start, end, dur) / (1+end-start);
        }
    }

    public static float eviInteg(Task t, long dur) {
        return eviInteg(t, t.start(), t.end(), dur);
    }

    /**
     * convenience method for selecting evidence integration strategy
     */
    public static float eviInteg(Task t, long qStart, long qEnd, long dur) {
        if (qStart == qEnd) {

            return t.evi(qStart, dur);
        } else {
            long tStart = t.start();
            if (tStart == ETERNAL)
                return t.evi() * (qEnd - qStart + 1);







            long tEnd = t.end();



            long[] points;
            if (qStart!=tStart || qEnd!=tEnd) {
                LongHashSet pp = new LongHashSet(4);
                pp.add(qStart);
                pp.add(qEnd);
                if (qStart+1!=qEnd)
                    pp.add((qStart+qEnd)/2L); //mid

                @Nullable Longerval qt = Longerval.intersect(qStart, qEnd, tStart, tEnd);
                if (qt != null) {

                    //inner points
                    long qta = qt.a;
                    if (qta > qStart && qta < qEnd) { //quick test to avoid set add
                        pp.add(qta);
                    }
                    long qtb = qt.b;
                    if (qta != qtb) {
                        if (qtb > qStart && qtb < qEnd) { //quick test to avoid set add
                            pp.add(qtb);
                        }
                    }

                }
                points = pp.toSortedArray();
            } else {
                //quick points array determination
                long d = qEnd - qStart;
                if (d == 0) {
                    points = new long[] { qStart };
                } else if (d == 1) {
                    points = new long[] { qStart, qEnd };
//                } else if (d <= dur) {
//                    points = new long[] { qStart, (qStart + qEnd)/2L, qEnd };
                } else {
                    //with midpoint supersample
                    points = new long[] { qStart, (qStart + qEnd)/2L, qEnd };
                }
            }





            //return x.eviIntegRectMid(dur, points);
            return t.eviIntegTrapezoidal(dur, points);


        }
    }
}
