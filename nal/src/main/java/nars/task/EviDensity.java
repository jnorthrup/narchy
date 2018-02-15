package nars.task;

import jcog.math.Interval;
import nars.Task;
import nars.task.util.TaskRegion;

/** iterative calculator of evidential density
 *      evidensity: average evidence of a set of evidential components integrated through their unioned time-span
 */
public final class EviDensity {

    public float factor;
    public long unionStart;
    public long unionEnd;

//    /** HACK TODO more accurate - call after union fusion calculated  */
//    public float factor(float freqDifference) {
//        factor *= (1f - freqDifference);
//        return factor;
//    }

    public EviDensity(TaskRegion... x) {

        assert (x.length > 1);

        Interval uu = new Interval(x[0].start(), x[0].end());
        for (int i = 1, xLength = x.length; i < xLength; i++) {
            TaskRegion xi = x[i];
            if (xi == null) continue;
            uu = uu.union(xi.start(), xi.end());
        }

        this.unionStart = uu.a;
        this.unionEnd = uu.b;

        long unionRange = Math.max(1,uu.length());

        /** integrate the evidence*time curve of each task component to calculate the effective
         * evidence volume, and evidence density (which becomes an evidence reduction factor if < 1) */
        float v = 0, totalEvi = 0;
        for (int i = 0; i < x.length; i++) {
            TaskRegion xxi = x[i];
            if (xxi == null) continue;

            Task xi = xxi.task();


            //TODO use a better approximation
            float xe = xi.evi();
            totalEvi += xe;
            v += xe * Math.max(xi.range(), 1);
        }
        float density = v / unionRange;

        this.factor = density / totalEvi;


    }

//    @Nullable
//    public static TimeFusion the(List<? extends TaskRegion> e) {
//        int n = e.size();
//        assert (n > 1);
//        Interval[] ii = new Interval[n];
//        int eternals = 0;
//        for (int j = 0; j < n; j++) {
//            TaskRegion ee = e.get(j);
//            long s = ee.start();
//            if (s == ETERNAL) {
//                eternals++;
//                //leave a blank spot we will fill it in after
//            } else {
//                //TODO see if s == ETERNAL
//                ii[j] = new Interval(s, ee.end());
//            }
//        }
//        if (eternals == n) {
//            return null; //all eternal, no discount need apply
//        } else if (eternals > 0) {
//            long max = Long.MIN_VALUE, min = Long.MAX_VALUE;
//            for (Interval i : ii) {
//                if (i != null) {
//                    if (i.a < min) min = i.a;
//                    if (i.b > max) max = i.b;
//                }
//            }
//            Interval entire = new Interval(min, max);
//            for (int j = 0, iiLength = ii.length; j < iiLength; j++) {
//                Interval i = ii[j];
//                if (i == null)
//                    ii[j] = entire;
//            }
//        }
//
//        return new TimeFusion(ii);
//    }

//    public static float eviEternalize(float evi, float concEviFactor) {
//        if (concEviFactor != 1) {
//            float eviEternal = Truth.eternalize(evi);
//            evi = eviEternal + Util.lerp(concEviFactor, 0, evi - eviEternal);
//        }
//        return evi;
//    }


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
