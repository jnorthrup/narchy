package nars.concept.dynamic;

import jcog.TODO;
import jcog.Util;
import jcog.list.FasterList;
import jcog.math.LongInterval;
import jcog.pri.Prioritized;
import nars.*;
import nars.control.Cause;
import nars.task.EviDensity;
import nars.task.NALTask;
import nars.task.util.InvalidTaskException;
import nars.task.util.TaskRegion;
import nars.term.Term;
import nars.time.Tense;
import nars.truth.Stamp;
import nars.truth.Truth;
import nars.truth.Truthed;
import org.eclipse.collections.api.tuple.primitive.ObjectBooleanPair;
import org.eclipse.collections.api.tuple.primitive.ObjectFloatPair;
import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static nars.Op.*;
import static nars.time.Tense.XTERNAL;
import static nars.truth.TruthFunctions.c2wSafe;

/**
 * Created by me on 12/4/16.
 */
public final class DynTruth extends FasterList<TaskRegion> implements Prioritized, TaskRegion {

    LongHashSet evi = null;

    public DynTruth(int initialCap) {
        super(initialCap);
    }

    public float pri() {

        int s = size();
        assert (s > 0);

        if (s > 1) {
            //float f = 1f / s;
            //            for (Task x : e) {
            //                BudgetMerge.plusBlend.apply(b, x.budget(), f);
            //            }
            //            return b;
            //return e.maxValue(Task::priElseZero); //use the maximum of their truths

            //TODO sum weighted by evidence
            return meanValue(DynTruth::pri); //average value
        } else {
            return pri(get(0));
        }
    }

    private static float pri(LongInterval x) {
        if (x instanceof Prioritized)
            return ((Prioritized) x).priElseZero();

        //TODO ??

        return 0;
    }

    @Override
    public long start() {
        throw new TODO();
    }

    @Override
    public long end() {
        throw new TODO();
    }

    @Override
    @Nullable
    public short[] cause() {
        return Cause.sample(Param.causeCapacity.intValue(),
                Util.map(0, size(), x -> get(x).cause(), short[][]::new));
    }

    @Override
    public float coordF(boolean maxOrMin, int dimension) {
        throw new TODO();
    }


    List<Stamp> evidence() {
        List<Stamp> s = $.newArrayList();
        for (LongInterval x : this) {
            Stamp ss = stamp(x);
            if (ss != null)
                s.add(ss);
        }
        return s;
    }

    @Deprecated
    public static Stamp stamp(LongInterval x) {
        if (x instanceof Task)
            return ((Task) x);
//        } else if (x instanceof DynTruth) {
//            return ((DynTruth)x).stamp();
//        }
        return null;
    }

    @Nullable
    public Truthed truth(Term superterm, DynamicTruthModel m, boolean taskOrJustTruth, boolean beliefOrGoal, NAR nar) {


        Truth t = m.truth(this, nar);
        if (t == null)
            return null;


        float evi = t.evi();
        float eviMin = c2wSafe(nar.confMin.floatValue());
        if (evi < eviMin)
            return null;

        //TODO compute max valid overlap to terminate the zip early
        ObjectFloatPair<long[]> ss = Stamp.zip(evidence(), Param.STAMP_CAPACITY);
        evi = evi * Param.overlapFactor(ss.getTwo());
        if (evi < eviMin)
            return null;

        float freq = t.freq();

        float f;
        long start, end;
        if (taskOrJustTruth) {

            if (size() > 1) {
                if (superterm.op() == CONJ) {
                    long min = Long.MAX_VALUE;
                    long maxRange = Long.MIN_VALUE;
                    for (int i = 0, thisSize = size(); i < thisSize; i++) {
                        LongInterval ii = get(i);
                        long iis = ii.start();
                        if (iis != ETERNAL) {
                            min = Math.min(min, iis);
                            maxRange = Math.max(maxRange, ii.end() - iis);
                        }
                    }

                    if (min == Long.MAX_VALUE)
                        start = end = ETERNAL; //all eternal
                    else {
                        start = min;
                        end = min + maxRange;
                    }

                } else {
                    //dilute the evidence in proportion to temporal sparseness for non-temporal results

                    EviDensity se = new EviDensity(this);
                    evi *= se.factor();
                    if (evi!=evi || evi < eviMin)
                        return null;
                    start = se.unionStart;
                    end = se.unionEnd;

                }
            } else {
                //only one task
                LongInterval only = get(0);
                start = only.start();
                end = only.end();
            }

            if (superterm.op() == NEG) {
                superterm = superterm.unneg(); //unneg if constructing a task, but dont if just returning the truth
                f = 1f - freq;
            } else {
                f = freq;
            }

        } else {
            start = end = XTERNAL; //not used
            f = freq;
        }

        Truth tr = Truth.theDiscrete(f, evi, nar);
        if (tr == null)
            return null;
        if (!taskOrJustTruth)
            return tr;

        float priority = pri();



        // then if the term is valid, see if it is valid for a task
        @Nullable ObjectBooleanPair<Term> r = Task.tryContent(
                m.construct(superterm, this),
                beliefOrGoal ? BELIEF : GOAL, true);
        if (r == null)
            return null;

        NALTask dyn = new DynTruthTask(
                r.getOne(), beliefOrGoal,
                tr.negIf(r.getTwo()), nar, start, end,
                ss.getOne());
        //if (ss.getTwo() > 0) dyn.setCyclic(true);

        dyn.cause = cause();
        dyn.priSet(priority);

        if (Param.DEBUG)
            dyn.log("Dynamic");

        return dyn;
    }

    @Override
    public @Nullable Task task() {
        throw new TODO();
    }

    @Override
    public void clear() {
        super.clear();
        evi.clear();
    }

    @Override
    public boolean add(TaskRegion newItem) {
        super.add(newItem);

        if (newItem!=null) {
            long[] stamp = newItem.task().stamp();

            if (evi == null)
                evi = new LongHashSet(stamp.length * 2 /* estimate */);

            evi.addAll(stamp);
            evi.compact(); //because it may be compared against frequently
        }

        return true;
    }

    public boolean filterOverlap(Task task) {
        if (evi != null) {

            long[] s = task.stamp();
            for (long x : s) {
                if (evi.contains(x))
                    return false; //overlap
            }
        }
        return true;
    }

    public static class DynTruthTask extends NALTask {

        DynTruthTask(Term c, boolean beliefOrGoal, Truth tr, NAR nar, long start, long end, long[] stamp) throws InvalidTaskException {
            super(c, beliefOrGoal ? Op.BELIEF : Op.GOAL, tr, nar.time(), start, (start == Tense.ETERNAL || c.op().temporal) ? start : end, stamp);
        }

        @Override
        public DynTruthTask pri(float p) {
            super.pri(p);
            return this;
        }

        @Override
        public void meta(String key, Object value) {
            //dont store meta since these are temporary
        }

        @Override
        public boolean isInput() {
            return false;
        }

        @Override
        public float eternalizability() {
            return 1;
        }
    }

}
