package nars.task;

import jcog.data.iterator.ArrayIterator;
import jcog.data.set.MetalLongSet;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.task.util.TaskRegion;
import nars.time.Tense;
import nars.truth.PreciseTruth;
import nars.truth.Stamp;
import nars.truth.Truth;
import nars.truth.Truthed;
import nars.truth.polation.Projection;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

import static nars.truth.func.TruthFunctions.c2wSafe;

/**
 * Truth/Task Revision & Projection (Revection)
 */
public enum Revision {;

    @Nullable
    public static Truth revise(/*@NotNull*/ Truthed a, /*@NotNull*/ Truthed b, float factor, float minEvi) {

        float ae = a.evi();
        float be = b.evi();
        float w = ae + be;
        float e = w * factor;

        return e <= minEvi ?
                null :
                PreciseTruth.byEvi(
                        (ae * a.freq() + be * b.freq()) / w,
                        e
                );
    }


    public static Truth revise(/*@NotNull*/ Truthed a, /*@NotNull*/ Truthed b) {
        return revise(a, b, 1f, 0f);
    }


    //    /** merge occurrence */
//    public static long merge(long at, long bt, float aProp, NAR nar) {
//        long dt;
//        long diff = Math.abs(at - bt);
//        if (diff == 1) {
//            return choose(at, bt, aProp, nar.random());
//        }
//        if ((float) diff /nar.dur() <= nar.intermpolationRangeLimit.floatValue()) {
//            //merge if within a some number of durations
//            dt = Util.lerp(aProp, bt, at);
//        } else {
//            dt = ETERNAL;
//        }
//        return dt;
//    }

    //    static Term choose(Term a, Term b, float aProp, /*@NotNull*/ Random rng) {
//        return (rng.nextFloat() < aProp) ? a : b;
//    }
//    static int choose(int a, int b, float aProp, /*@NotNull*/ Random rng) {
//        return rng.nextFloat() < aProp ? a : b;
//    }
//
//    static long choose(long a, long b, float aProp, /*@NotNull*/ Random rng) {
//        return rng.nextFloat() < aProp ? a : b;
//    }

//    /*@NotNull*/
//    public static Term[] choose(/*@NotNull*/ Term[] a, Term[] b, float aBalance, /*@NotNull*/ Random rng) {
//        int l = a.length;
//        Term[] x = new Term[l];
//        for (int i = 0; i < l; i++) {
//            x[i] = choose(a[i], b[i], aBalance, rng);
//        }
//        return x;
//    }

    /** might be useful in lazily prioritizing or parallelizing somehow */
    public static Task merge(Supplier<TaskRegion> x, Supplier<TaskRegion> y, NAR nar) {
        return merge(x::get, y::get, nar);
    }

    /**
     * 2-ary merge with quick overlap filter
     */
    public static Task merge(TaskRegion x, TaskRegion y, NAR nar) {

        return Stamp.overlaps((Task) x, (Task) y) ? null : merge(nar, x, y);

    }


    /**
     * assumes none of the tasks are eternal
     * <p>
     * warning: output task will have zero priority and input tasks will not be affected
     * this is so a merge construction can be attempted without actually being budgeted
     * <p>
     * also cause merge is deferred in the same way
     */
    @Nullable
    static Task merge(NAR nar, TaskRegion... tasks) {

        assert (tasks.length > 1);

        long[] u = Tense.merge(ArrayIterator.iterable(tasks));
        if (u == null)
            return null;

        Projection T = nar.projection(u[0], u[1], 0).add(tasks);

        MetalLongSet stamp = T.filterCyclic(true, 2);
        if (stamp == null)
            return null;

        assert(T.size()>=2);

        Truth truth = T.truth(c2wSafe(nar.confMin.floatValue()), nar);
        if (truth == null)
            return null;

        Truth cTruth = truth.dither(nar);
        if (cTruth == null)
            return null;

        byte punc = T.punc();
        return Task.tryTask(T.term, punc, cTruth, (c, tr) ->
                new UnevaluatedTask(c, punc,
                        tr,
                        nar.time(), T.start(), T.end(),
                        Stamp.sample(Param.STAMP_CAPACITY, stamp /* TODO account for relative evidence contributions */, nar.random())
                )
        );
    }


    //    public static Task mergeOrChoose(@Nullable Task x, @Nullable Task y, long start, long end, Predicate<Task> filter, NAR nar) {
//        if (x == null && y == null)
//            return null;
//
//        if (filter != null) {
//            if (x != null && !filter.test(x))
//                x = null;
//            if (y != null && !filter.test(y))
//                y = null;
//        }
//
//        if (y == null)
//            return x;
//
//        if (x == null)
//            return y;
//
//        if (x.equals(y))
//            return x;
//
//
//        Top<Task> top = new Top<>(t -> TruthIntegration.eviInteg(t, 1));
//
//        if (x.target().equals(y.target()) && !Stamp.overlapsAny(x, y)) {
//
//            Task xy = merge(nar, nar.dur(), start, end, true, x, y);
//            if (xy != null && (filter == null || filter.test(xy)))
//                top.accept(xy);
//        }
//        top.accept(x);
//        top.accept(y);
//
//        return top.the;
//    }
}



























































































































































































































































































































































































































































































































































































































































































































































































































