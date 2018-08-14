package nars.task;

import jcog.Texts;
import jcog.Util;
import jcog.pri.Priority;
import jcog.sort.Top;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.control.Cause;
import nars.subterm.Subterms;
import nars.task.util.TaskRegion;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.util.Conj;
import nars.time.Tense;
import nars.truth.PreciseTruth;
import nars.truth.Stamp;
import nars.truth.Truth;
import nars.truth.Truthed;
import nars.truth.polation.TruthIntegration;
import nars.truth.polation.TruthPolation;
import org.eclipse.collections.api.set.primitive.LongSet;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.function.Predicate;

import static nars.Op.*;
import static nars.time.Tense.*;

/**
 * Revision / Projection / Revection Utilities
 */
public class Revision {

    public static final Logger logger = LoggerFactory.getLogger(Revision.class);

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


    static Term intermpolate(/*@NotNull*/ Term a, long bOffset, /*@NotNull*/ Term b, float aProp, float curDepth, NAR nar) {

        if (a.equals(b) && bOffset == 0)
            return a;

        if (a instanceof Atomic || b instanceof Atomic)
            return Null; //atomics differ

        Op ao = a.op();
        Op bo = b.op();
        if (ao != bo)
            return Null;


        int len = a.subs();
        assert len > 0;
//        if (len == 0) {
//            //WTF
//            if (a.op() == PROD) return a;
////            else
////                throw new WTF();
//        }


        if (ao.temporal) {
            if (ao == CONJ) {
                return Conj.conjIntermpolate(a, b, aProp, bOffset, nar);
            } else if (ao == IMPL) {
                return dtMergeDirect(a, b, aProp, curDepth, nar);
            } else
                throw new UnsupportedOperationException();
        } else {


            Term[] ab = new Term[len];
            boolean change = false;
            Subterms aa = a.subterms();
            Subterms bb = b.subterms();
            for (int i = 0; i < len; i++) {
                Term ai = aa.sub(i);
                Term bi = bb.sub(i);
                if (!ai.equals(bi)) {
                    Term y = intermpolate(ai, 0, bi, aProp, curDepth / 2f, nar);
                    if (y == null || y instanceof Bool && !(ai instanceof Bool))
                        return Null;

                    if (!ai.equals(y)) {
                        change = true;
                        ai = y;
                    }
                }
                ab[i] = ai;
            }

            return !change ? a : ao.the(chooseDT(a, b, aProp, nar), ab);
        }

    }


    /*@NotNull*/
    private static Term dtMergeDirect(/*@NotNull*/ Term a, /*@NotNull*/ Term b, float aProp, float depth, NAR nar) {

        depth /= 2f;

        Term a0 = a.sub(0), a1 = a.sub(1), b0 = b.sub(0), b1 = b.sub(1);

        int dt = chooseDT(a, b, aProp, nar);
        if (a0.equals(b0) && a1.equals(b1)) {
            return a.dt(dt);
        } else {
            Term na = intermpolate(a0, 0, b0, aProp, depth, nar);
            if (na == Null || na == False) return na;

            Term nb = intermpolate(a1, 0, b1, aProp, depth, nar);
            if (nb == Null || nb == False) return nb;


            return a.op().the(dt, na, nb);
        }

    }

    public static int chooseDT(Term a, Term b, float aProp, NAR nar) {
        int adt = a.dt(), bdt = b.dt();
        return chooseDT(adt, bdt, aProp, nar);
    }

    static int chooseDT(int adt, int bdt, float aProp, NAR nar) {
        int dt;
        if (adt == bdt) {
            dt = adt;
        } else if (adt == XTERNAL || bdt == XTERNAL) {

            dt = adt == XTERNAL ? bdt : adt;
            //dt = choose(adt, bdt, aProp);

        } else if (adt == DTERNAL || bdt == DTERNAL) {

            dt = DTERNAL;
            //dt = choose(adt, bdt, aProp, nar.random());
            //dt = (adt == DTERNAL) ? bdt : adt;

        } else {
            dt = merge(adt, bdt, aProp, nar);
        }


        return Tense.dither(dt, nar);
    }

    /** merge delta */
    static int merge(int adt, int bdt, float aProp, NAR nar) {
        if (adt >= 0 == bdt >= 0 && (float) Math.abs(adt - bdt) /nar.dur() <= nar.intermpolationDurLimit.floatValue()) {
            //merge if they are the same sign or within a some number of durations

            long abdt = Util.lerp(aProp, bdt, adt); // (((long) adt) + (bdt)) / 2L;
            assert Math.abs(abdt) < Integer.MAX_VALUE;

            return (int) abdt;

        } else {
            //different directions and exceed a duration in difference.
            //discard temporal information by resorting to eternity
            return DTERNAL;
        }
    }
    /** merge occurrence */
    public static long merge(long at, long bt, float aProp, NAR nar) {
        long dt;
        long diff = Math.abs(at - bt);
        if (diff == 1) {
            return choose(at, bt, aProp, nar.random());
        }
        if ((float) diff /nar.dur() <= nar.intermpolationDurLimit.floatValue()) {
            //merge if within a some number of durations
            dt = Util.lerp(aProp, bt, at);
        } else {
            dt = ETERNAL;
        }
        return dt;
    }

//    static Term choose(Term a, Term b, float aProp, /*@NotNull*/ Random rng) {
//        return (rng.nextFloat() < aProp) ? a : b;
//    }
    static int choose(int a, int b, float aProp, /*@NotNull*/ Random rng) {
        return rng.nextFloat() < aProp ? a : b;
    }
    static long choose(long a, long b, float aProp, /*@NotNull*/ Random rng) {
        return rng.nextFloat() < aProp ? a : b;
    }

//    /*@NotNull*/
//    public static Term[] choose(/*@NotNull*/ Term[] a, Term[] b, float aBalance, /*@NotNull*/ Random rng) {
//        int l = a.length;
//        Term[] x = new Term[l];
//        for (int i = 0; i < l; i++) {
//            x[i] = choose(a[i], b[i], aBalance, rng);
//        }
//        return x;
//    }


    public static Term intermpolate(/*@NotNull*/ Term a, /*@NotNull*/ Term b, float aProp, NAR nar) {
        return intermpolate(a, 0, b, aProp, nar);
    }

    /**
     * a is left aligned, dt is any temporal shift between where the terms exist in the callee's context
     */
    static Term intermpolate(/*@NotNull*/ Term a, long dt, /*@NotNull*/ Term b, float aProp, NAR nar) {
        return intermpolate(a, dt, b, aProp, 1, nar);
    }


    /**
     * forces projection
     */
    @Nullable
    public static Task merge(NAR nar, TaskRegion... tt) {
        assert tt.length > 1;
        long[] u = Tense.dither(Tense.union(tt), nar);
        return merge(nar, nar.dur(), u[0], u[1], true, tt);
    }


    @Nullable
    public static Task merge(NAR nar, int dur, long start, long end, boolean forceProjection, TaskRegion... tasks) {

//        tasks = ArrayUtils.removeNulls(tasks, Task[]::new);


        /*Truth.EVI_MIN*/

        float range = start != ETERNAL ? end - start + 1 : 1;
        float eviMinInteg = Param.TRUTH_MIN_EVI;
        Task defaultTask;
        if (!forceProjection) {
            defaultTask = (Task) tasks[0];
//            eviMinInteg = range * TruthIntegration.eviAvg(defaultTask, tasks[0].start(), tasks[0].end(), dur);
        } else {
            defaultTask = null;
        }

        TruthPolation T = Param.truth(start, end, dur).add(tasks);

        if (T.size() == 1) {
            if (!forceProjection) {
                return T.getTask(0);
            } else {
                return null; //fail
            }
        }

        LongSet stamp = T.filterCyclic();

        Truth truth = T.truth(nar);
        if (truth == null)
            return defaultTask;

        float truthEvi = truth.evi();

        if (truthEvi * range < eviMinInteg)
            return defaultTask;

        Truth cTruth = Truth.theDithered(truth.freq(), truthEvi, nar);
        if (cTruth == null)
            return defaultTask;

        byte punc = T.punc();
        Task t = Task.tryTask(T.term, punc, cTruth, (c, tr) -> {



                    return new NALTask(c, punc,
                            tr,
                            nar.time(), start, end,
                            Stamp.sample(Param.STAMP_CAPACITY, stamp /* TODO account for relative evidence contributions */, nar.random())
                    );
                }
        );

        if (t == null)
            return null;

        tasks = T.tasks();

        t.pri(Priority.fund(Util.max((TaskRegion p) -> p.task().priElseZero(), tasks),
                true,
                Tasked::task, tasks));

        ((NALTask) t).cause(Cause.merge(Param.causeCapacity.intValue(), tasks));

        return t;
    }


    /**
     * heuristic representing the difference between the dt components
     * of two temporal terms.
     * 0 means they are identical or otherwise match.
     * > 0 means there is some difference.
     * <p>
     * this adds a 0.5 difference for && vs &| and +1 for each dt
     * XTERNAL matches anything
     */
    public static float dtDiff(Term a, Term b) {
        return dtDiff(a, b, 1);
    }

    static float dtDiff(Term a, Term b, int depth) {
        if (a.equals(b)) return 0f;


        Op ao = a.op();
        Op bo = b.op();
        if (ao != bo)
            return Float.POSITIVE_INFINITY;


        Subterms aa = a.subterms();
        int len = aa.subs();
        Subterms bb = b.subterms();

        float d = 0;

        boolean aSubsEqualsBSubs = aa.equals(bb);
        if (a.op() == CONJ && !aSubsEqualsBSubs) {

            Conj c = new Conj();
            String as = Conj.sequenceString(a, c).toString();
            String bs = Conj.sequenceString(b, c).toString();

            int levDist = Texts.levenshteinDistance(as, bs);
            float seqDiff = (float) levDist / Math.min(as.length(), bs.length());


            float rangeDiff = Math.max(1f, Math.abs(a.dtRange() - b.dtRange()));

            d += (1f + rangeDiff) * (1f + seqDiff);

        } else {
            if (!aSubsEqualsBSubs) {
                if (aa.subs() != bb.subs())
                    return Float.POSITIVE_INFINITY;

                for (int i = 0; i < len; i++)
                    d += dtDiff(aa.sub(i), bb.sub(i), depth + 1);
            } else {

                int adt = a.dt();
                int bdt = b.dt();
                if (adt != bdt) {
                    if (adt == XTERNAL || bdt == XTERNAL) {
                        //zero, match
                    } else {

                        boolean ad = adt == DTERNAL;
                        boolean bd = bdt == DTERNAL;
                        if (!ad && !bd)
                            d += Math.abs(adt - bdt);
                        else if (adt == DTERNAL)
                            d += 1f + Math.abs(bdt) / 2f; //one is dternal the other is not, record at least some difference
                        else if (bdt == DTERNAL)
                            d += 1f + Math.abs(adt) / 2f; //one is dternal the other is not, record at least some difference
                    }
                }

            }
        }

        return d / depth;
    }

    public static Task mergeOrChoose(@Nullable Task x, @Nullable Task y, long start, long end, Predicate<Task> filter, NAR nar) {
        if (x == null && y == null)
            return null;

        if (filter != null) {
            if (x != null && !filter.test(x))
                x = null;
            if (y != null && !filter.test(y))
                y = null;
        }

        if (y == null)
            return x;

        if (x == null)
            return y;

        if (x.equals(y))
            return x;


        Top<Task> top = new Top<>(t -> TruthIntegration.eviInteg(t, 1));

        if (x.term().equals(y.term()) && !Stamp.overlapsAny(x, y)) {

            Task xy = merge(nar, nar.dur(), start, end, true, x, y);
            if (xy != null && (filter == null || filter.test(xy)))
                top.accept(xy);
        }
        top.accept(x);
        top.accept(y);

        return top.the;
    }
}



























































































































































































































































































































































































































































































































































































































































































































































































































