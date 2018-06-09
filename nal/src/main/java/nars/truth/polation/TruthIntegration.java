package nars.truth.polation;

import jcog.math.Longerval;
import nars.Task;
import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;
import org.jetbrains.annotations.Nullable;

import static nars.time.Tense.ETERNAL;

public class TruthIntegration {

    public static float eviAvg(Task x, long start, long end, int dur) {
        if ((start == end) || start == ETERNAL) {
            return x.evi(start, dur); //point
        } else {
            return eviInteg(x, start, end, dur) / (end - start); //range
        }
    }

    /**
     * convenience method for selecting evidence integration strategy
     */
    public static float eviInteg(Task x, long qStart, long qEnd, long dur) {
        if (qStart == qEnd) {

            return x.evi(qStart, dur);
        } else {
            long tStart = x.start();
            if (tStart == ETERNAL)
                return x.evi() * (qEnd - qStart + 1);







            long tEnd = x.end();



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
                    if (qta != qStart && qta != qEnd) { //quick test to avoid set add
                        pp.add(qta);
                    }
                    long qtb = qt.b;
                    if (qta != qtb) {
                        if (qtb != qStart && qtb != qEnd) { //quick test to avoid set add
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
            return x.eviIntegTrapezoidal(dur, points);


        }
    }
}
