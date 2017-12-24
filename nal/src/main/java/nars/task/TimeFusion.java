package nars.task;

import jcog.Util;
import jcog.math.Interval;
import nars.task.util.TaskRegion;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static nars.time.Tense.ETERNAL;

public final class TimeFusion {

    public final float factor;
    public final long unionStart;
    public final long unionEnd;

    public TimeFusion(long as, long ae, long bs, long be) {
        this(new Interval(as, ae), new Interval(bs, be));
        assert(as <= ae);
        assert(bs <= be);
    }

    public TimeFusion(Interval... x) {

        assert(x.length > 1);

        Interval uu = x[0];
        for (int n = 1, xLength = x.length; n < xLength; n++) {
            uu = uu.union(x[n]);
        }
        this.unionStart = uu.a;
        this.unionEnd = uu.b;

        long uLen = uu.length();

        long lenSum = Util.sum(Interval::length, x);
        int n = x.length;
        this.factor = ((float)(n + lenSum)) / (n + uLen * n); //ratio of how much the union is filled with respect to the number of tasks being overlapped


    }

    @Nullable
    public static TimeFusion the(List<? extends TaskRegion> e) {
        int n = e.size();
        assert (n > 1);
        Interval[] ii = new Interval[n];
        int eternals = 0;
        for (int j = 0; j < n; j++) {
            TaskRegion ee = e.get(j);
            long s = ee.start();
            if (s == ETERNAL) {
                eternals++;
                //leave a blank spot we will fill it in after
                continue;
            } else {
                //TODO see if s == ETERNAL
                ii[j] = new Interval(s, ee.end());
            }
        }
        if (eternals == n) {
            return null; //all eternal, no discount need apply
        } else if (eternals > 0) {
            long max = Long.MIN_VALUE, min = Long.MAX_VALUE;
            for (Interval i : ii) {
                if (i!=null) {
                    if (i.a < min) min = i.a;
                    if (i.b > max) max = i.b;
                }
            }
            Interval entire = new Interval(min, max);
            for (int j = 0, iiLength = ii.length; j < iiLength; j++) {
                Interval i = ii[j];
                if (i == null)
                    ii[j] = entire;
            }
        }

        return new TimeFusion(ii);
    }


//        Interval ii = x[0];
//        for (int n = 1, xLength = x.length; n < xLength; n++) {
//            ii = ii.intersection(x[n]);
//            if (ii == null)
//                break;
//        }
//
//        this.factor = factor(strict, x, uLen, ii != null ? ii.length() : 0);

//    static float factor(float strict, Interval[] ii, long union, long intersection) {
//        assert(ii.length > 1);
//
//        float f;
////        if (intersection > 0) {
////            //partial intersection, weaken the union
////            f = Util.lerp((1f + intersection) / (1f + union), strict, 1f);
////        } else {
////            //no intersection
////            if (strict == 0) {
////                f = 0;
////            } else {
//                long lenSum = Util.sum(Interval::length, ii);
//                int n = ii.length;
//                f = ((float)(n + lenSum)) / (n + union * n); //ratio of how much the union is filled with respect to the number of tasks being overlapped
//
//
////                long lenMin = Util.min(Interval::length, ii);
////                long separation = union - lenSum;
////                if (separation >= 0) {
////                    float separationRatio = ((float) separation) / (1 + lenMin);
////                    f = Util.lerp(1f / (1f + separationRatio), 0, strict);
////                } else {
//
////                }
//
//                //
//                //
//                //            float factor = 1f;
//                //            if (u > s) {
//                //
//                //                /** account for how much the merge stretches the truth beyond the range of the inputs */
//                //                long separation = u - s;
//                //                if (separation > 0) {
//                //                    //int hd = nar.dur();
//                //                    int minterval = Math.min(al, bl);// + hd; //+hd to pad the attention surrounding point0like events
//                //                    if (separation < minterval) {
//                //                        factor = 1f - separation / ((float) minterval);
//                //                    } else {
//                //                        factor = 0; //too separate
//                //                    }
//                //
//                //
//                ////                    factor = 1f - (separation / (separation + (float) u));
//                //
//                //                }
//                //            }
//                //
//                //            this.factor = factor;
//
////            }
////        }
//        return f;
//    }

}
