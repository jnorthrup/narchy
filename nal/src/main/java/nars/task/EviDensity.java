package nars.task;

import jcog.math.Interval;
import nars.Task;
import nars.task.util.TaskRegion;

/** iterative calculator of evidential density
 *      evidensity: average evidence of a set of evidential components integrated through their unioned time-span
 */
public final class EviDensity {

    public long unionStart;
    public long unionEnd;

    float sumEviIntegrals = Float.NaN, sumEviAvg = Float.NaN;


    public EviDensity() {
    }

    public EviDensity(TaskRegion... x) {
        this();
        assert (x.length > 1);
        for (TaskRegion xx : x) {
            if (xx!=null)
                add(xx);
        }
    }

    public EviDensity clone() {
        EviDensity clone = new EviDensity();
        clone.unionStart = unionStart;
        clone.unionEnd = unionEnd;
        clone.sumEviAvg = sumEviAvg;
        clone.sumEviIntegrals = sumEviIntegrals;
        return clone;
    }

    public void add(TaskRegion x) {
        Task xt = x.task();
        add(x.start(), x.end(), xt.evi(), xt.eviInteg());
    }

    public void add(long xStart, long xEnd, float evi) {
        add(xStart, xEnd, evi, evi * (1 + (xEnd - xStart)));
    }

    public void add(long xStart, long xEnd, float evi, float eviInteg) {
        if (sumEviIntegrals != sumEviIntegrals) {
            //first add
            unionStart = xStart;
            unionEnd = xEnd;
            sumEviAvg = sumEviIntegrals = 0;
        } else {
            Interval uu = new Interval(unionStart, unionEnd).union(xStart, xEnd);
            this.unionStart = uu.a;
            this.unionEnd = uu.b;
        }

        sumEviAvg += evi;
        sumEviIntegrals += eviInteg;
    }

    /** compute the evidence averaged across the union */
    public float density() {
        long unionRange = 1 + (unionEnd - unionStart);
        return sumEviIntegrals / unionRange;
    }

    /** ratio of density to sum of averages */
    public float factor() {
        long unionRange = 1 + (unionEnd - unionStart);
        return sumEviIntegrals / (unionRange * sumEviAvg);
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
