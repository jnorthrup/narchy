package nars.task;

import jcog.data.set.MetalLongSet;
import nars.NAL;
import nars.NAR;
import nars.Task;
import nars.task.util.TaskRegion;
import nars.time.Tense;
import nars.truth.PreciseTruth;
import nars.truth.Stamp;
import nars.truth.Truth;
import nars.truth.Truthed;
import nars.truth.polation.TruthProjection;
import org.eclipse.collections.api.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.collections.impl.tuple.Tuples.pair;

/**
 * Truth/Task Revision & Projection (Revection)
 */
public enum Revision {;

    /** fundamental eternal revision */
    @Nullable public static Truth revise(/*@NotNull*/ Truthed a, /*@NotNull*/ Truthed b, float factor, float minEvi) {

        double ae = a.evi();
        double be = b.evi();
        double w = ae + be;
        double e = w * factor;

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
    public static <T extends TaskRegion> Pair<Task, TruthProjection> merge(NAL<NAL<NAR>> NAL, boolean dither, T[] tasks) {


        assert (tasks.length > 1);

        //quick 2-ary stamp pre-filter
        //return Stamp.overlaps((Task) x, (Task) y) ? null : merge(nar, x, y);
        if (tasks.length == 2) {
            if (Stamp.overlapsAny(tasks[0].task(), tasks[1].task()))
                return null;
        }


        long[] u = Tense.merge(dither ? NAL.dtDither() : 0, tasks);
        if (u == null)
            return null;

        TruthProjection p = NAL.projection(u[0], u[1], 0).add(tasks);

        MetalLongSet stamp = p.commit(true, 2, true);
        if (stamp == null)
            return null;

        assert(p.size()>=2);

        double eviMin =
                NAL.belief.REVISION_MIN_EVI_FILTER ? NAL.confMin.asEvi() : NAL.truth.TRUTH_EVI_MIN;
                //;

        Truth truth = p.truth(eviMin, dither, true, NAL);
        if (truth == null)
            return null;

        byte punc = p.punc();
        Task y = Task.tryTask(p.term, punc, truth, (c, tr) ->
                new UnevaluatedTask(c, punc,
                        tr,
                        NAL.time(), p.start(), p.end(),
                        Stamp.sample(NAL.STAMP_CAPACITY, stamp /* TODO account for relative evidence contributions */, NAL.random())
                )
        );
        return pair(y, p);
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



























































































































































































































































































































































































































































































































































































































































































































































































































