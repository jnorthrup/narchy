package nars.task.util;

import jcog.data.set.MetalLongSet;
import nars.NAL;
import nars.NAR;
import nars.Task;
import nars.task.TemporalTask;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.truth.PreciseTruth;
import nars.truth.Stamp;
import nars.truth.Truth;
import nars.truth.Truthed;
import nars.truth.dynamic.DynTaskify;
import nars.truth.dynamic.DynamicConjTruth;
import nars.truth.proj.TruthProjection;
import org.eclipse.collections.api.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import static nars.time.Tense.ETERNAL;
import static org.eclipse.collections.impl.tuple.Tuples.pair;

/**
 * Truth/Task Revision & Projection (Revection)
 */
public enum Revision {;

    /** classic eternal revision */
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
    public static <T extends TaskRegion> Pair<Task, TruthProjection> merge(NAR n, boolean dither, int minComponents, T[] tasks) {

        assert (tasks.length >= minComponents);

        if (tasks.length == 2) {
            Task a = tasks[0].task(), b = tasks[1].task();

            //quick 2-ary stamp pre-filter
            if (Stamp.overlapsAny(a, b))
                return null;

            int dtDither = n.dtDither();
            if (a.minTimeTo(b) > n.intermpolationRangeLimit.floatValue()*Math.min(a.range(), b.range())) {
//            long sepThresh = Util.lerp(
//                    (Math.abs(a.freq()-b.freq())+Math.abs(a.conf()-b.conf()))/2,
//                    //low frequency difference: require large separation (relative to the task ranges)
//                    ((double)LongInterval.intersectLength(a.start(), a.end(), b.start(), b.end())/
//                            LongInterval.unionLength(a.start(), a.end(), b.start(), b.end()),
//
//                            Math.max(dtDither, a.range() + b.range()),
//                    //high frequency difference: require only some separation
//                    dtDither);
//
//            if (a.minTimeTo(b.start(), b.end()) >= sepThresh) {
//                @Nullable Pair<Task, TruthProjection> c = conjoin(n, dither, minComponents, tasks);
//                if (c!=null)
//                    return c;
                return null;

                //else: try default revise strategy (below)
            }
        }

        return revise(n, dither, minComponents, tasks);
    }

    /** temporal-induction conjunction merge strategy
     * TODO needs tested probably sequence term template constructed
     * */
    @Nullable private static <T extends TaskRegion> Pair<Task, TruthProjection> conjoin(NAR nar, boolean dither, int minComponents, T[] x) {
        final int dur = 0;

        DynTaskify d = new DynTaskify(DynamicConjTruth.ConjIntersection, x[0].task().isBeliefOrGoal(), true, true, dur, nar);
        for (int i = 0, xx = x.length; i < xx; i++) {
            T t = x[i];
            Task tt = t.task();
            if (tt.isNegative()) {
                d.componentPolarity.clear(i);
            }
            d.add(tt);
        }

        Task y = d.taskify();
        if (y==null)
            return null;

        Term term = y.term(); //d.model.reconstruct(null, d, ETERNAL, ETERNAL);
        if (term instanceof Bool || term.volume() > d.nar.termVolMax.intValue())
            return null;

        //HACK this tp is dummy
        TruthProjection tp = d.nar.projection(y.start(), term.eventRange()+y.end(), dur);
        d.forEach(tp::add);
        int active = tp.update(true);
        if (active!=x.length)
            return null;

        return pair(y, tp /* TODO */);
    }

    /** truth revision task merge strategy */
    @Nullable static <T extends TaskRegion> Pair<Task, TruthProjection> revise(NAL nal, boolean dither, int minComponents, T[] tasks) {

        TruthProjection p = nal.projection(ETERNAL, ETERNAL, 0).add(tasks);

        MetalLongSet stamp = p.commit(true, minComponents, true);
        if (stamp == null)
            return null;

        assert(p.size()>=2);

        double eviMin =
                NAL.belief.REVISION_MIN_EVI_FILTER ? nal.confMin.evi() : NAL.truth.EVI_MIN;

        Truth truth = p.truth(eviMin, dither, false, nal);
        if (truth == null)
            return null;

        byte punc = p.punc();
        Task y = Task.tryTask(p.term, punc, truth, (c, tr) ->
                new TemporalTask(c, punc,
                        tr,
                        nal.time(), p.start(), p.end(),
                        Stamp.sample(NAL.STAMP_CAPACITY, stamp /* TODO account for relative evidence contributions */, nal.random())
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



























































































































































































































































































































































































































































































































































































































































































































































































































