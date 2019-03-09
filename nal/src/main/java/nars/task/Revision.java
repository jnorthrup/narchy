package nars.task;

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
import org.eclipse.collections.api.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import static nars.truth.func.TruthFunctions.c2wSafe;
import static org.eclipse.collections.impl.tuple.Tuples.pair;

/**
 * Truth/Task Revision & Projection (Revection)
 */
public enum Revision {;

    /** fundamental eternal revision */
    @Nullable public static Truth revise(/*@NotNull*/ Truthed a, /*@NotNull*/ Truthed b, float factor, float minEvi) {

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


    /**
     * assumes none of the tasks are eternal
     * <p>
     * warning: output task will have zero priority and input tasks will not be affected
     * this is so a merge construction can be attempted without actually being budgeted
     * <p>
     * also cause merge is deferred in the same way
     * @return
     */
    public static <T extends TaskRegion> Pair<Task, Projection> merge(NAR nar, boolean dither, T... tasks) {


        assert (tasks.length > 1);

        //quick 2-ary stamp pre-filter
        //return Stamp.overlaps((Task) x, (Task) y) ? null : merge(nar, x, y);
        if (tasks.length == 2) {
            if (Stamp.overlapsAny(tasks[0].task(), tasks[1].task()))
                return null;
        }


        long[] u = Tense.merge(dither ? nar.dtDither() : 0, tasks);
        if (u == null)
            return null;

        Projection T = nar.projection(u[0], u[1], 0).add(tasks);

        MetalLongSet stamp = T.filterCyclic(true, 2);
        if (stamp == null)
            return null;

        assert(T.size()>=2);

        Truth truth = T.truth(c2wSafe(nar.confMin.floatValue()), dither, nar);
        if (truth == null)
            return null;

        byte punc = T.punc();
        Task y = Task.tryTask(T.term, punc, truth, (c, tr) ->
                new UnevaluatedTask(c, punc,
                        tr,
                        nar.time(), T.start(), T.end(),
                        Stamp.sample(Param.STAMP_CAPACITY, stamp /* TODO account for relative evidence contributions */, nar.random())
                )
        );
        return pair(y, T);
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



























































































































































































































































































































































































































































































































































































































































































































































































































