package nars.concept.dynamic;

import jcog.TODO;
import jcog.Util;
import jcog.list.FasterList;
import jcog.math.LongInterval;
import jcog.pri.Prioritized;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.control.Cause;
import nars.task.NALTask;
import nars.task.util.InvalidTaskException;
import nars.task.util.TaskRegion;
import nars.term.Term;
import nars.truth.Truth;
import nars.truth.Truthed;
import nars.util.TimeAware;
import nars.time.Tense;
import org.eclipse.collections.api.tuple.primitive.ObjectBooleanPair;
import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.function.BiFunction;

import static nars.Op.*;
import static nars.truth.TruthFunctions.w2cSafe;
import static nars.time.Tense.XTERNAL;

/**
 * Created by me on 12/4/16.
 */
public final class DynTruth extends FasterList<TaskRegion> implements Prioritized, TaskRegion {

    LongHashSet evi = null;

    public DynTruth(int initialCap) {
        super(initialCap);
    }

    @Override
    protected Object[] newArray(int newCapacity) {
        return new TaskRegion[newCapacity];
    }

    public float pri() {

        int s = size;
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

    private static float pri(TaskRegion x) {
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

    /** TODO make Task truth dithering optional */
    public Truthed eval(Term superterm, @Deprecated BiFunction<DynTruth, NAR, Truth> truthModel, boolean taskOrJustTruth, boolean beliefOrGoal, float freqRes, float confRes, float eviMin, NAR nar) {

        Truth t = truthModel.apply(this, nar);
        if (t == null)
            return null;

        float evi = t.evi();
        if (evi < eviMin)
            return null;

//        //TODO compute max valid overlap to terminate the zip early
//        ObjectFloatPair<long[]> ss = Stamp.zip((List) this, Param.STAMP_CAPACITY);

//        evi = evi * Param.overlapFactor(ss.getTwo());
//        if (evi < eviMin)
//            return null;

        float freq = t.freq();

        float f;
        long start, end;
        if (taskOrJustTruth) {

            if (size() > 1) {
                if (superterm.op() == CONJ) {
                    long min = TIMELESS;
                    long maxRange = 0;
                    boolean eternals = false;
                    for (int i = 0, thisSize = size(); i < thisSize; i++) {
                        LongInterval ii = get(i);
                        long iis = ii.start();
                        if (iis != ETERNAL) {
                            min = Math.min(min, iis);
                            maxRange = Math.max(maxRange, ii.end() - iis);
                        } else {
                            eternals = true;
                        }
                    }

                    if (eternals && min == TIMELESS) {
                        start = end = ETERNAL;
                    } else {
                        assert (min != TIMELESS);
//                    if (min == Long.MAX_VALUE)
//                        start = end = ETERNAL; //all eternal
//                    else {
                        start = min;
                        end = (min + maxRange);
                    }
//                    }

                } else {
                    //dilute the evidence in proportion to temporal sparseness for non-temporal results

//                    EviDensity se = new EviDensity(nar.dur(), this);
//                    evi = se.factor(evi);
//                    if (evi!=evi || evi < eviMin)
//                        return null;
//                    start = se.unionStart;
//                    end = se.unionEnd;

                    long[] u = Tense.union(this.array());
                    start = u[0]; end = u[1];
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


        if (!taskOrJustTruth)
            return Truth.theDithered(f, freqRes, evi, confRes, w2cSafe(eviMin));

        //undithered until final step for max precision

        // then if the term is valid, see if it is valid for a task
        Term content = truthModel instanceof DynamicTruthModel ?
                ((DynamicTruthModel) truthModel).construct(superterm, this) :
                superterm;


        @Nullable ObjectBooleanPair<Term> r = Task.tryContent(
                //if dynamic truth model, construct the appropriate term
                //otherwise consider the superterm argument as the task content
                content,
                beliefOrGoal ? BELIEF : GOAL, !Param.DEBUG_EXTRA);
        if (r == null)
            return null;

        NALTask dyn = new DynamicTruthTask(
                r.getOne(), beliefOrGoal,
                Truth.theDithered(r.getTwo() ? (1-f) : f, freqRes, evi, confRes, w2cSafe(eviMin)),
                nar, start, end,
                this.evi.toArray());
        //if (ss.getTwo() > 0) dyn.setCyclic(true);

        dyn.cause = cause();

        dyn.priSet(pri());

        if (Param.DEBUG_EXTRA)
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
    public boolean addAll(Collection<? extends TaskRegion> source) {
        source.forEach(this::add);
        return true;
    }

    @Override
    public boolean add(TaskRegion newItem) {
        super.add(newItem);

        if (newItem!=null) {
            long[] stamp = ((Task)newItem).stamp();

            if (evi == null)
                evi = new LongHashSet(stamp.length * 2 /* estimate */);

            evi.addAll(stamp);
            //evi.compact(); //because it may be compared against frequently
        }

        return true;
    }

    public boolean doesntOverlap(Task task) {
        if (evi != null) {

            long[] s = task.stamp();
            for (long x : s) {
                if (evi.contains(x))
                    return false; //overlap
            }
        }
        return true;
    }

    public final Truth truth(Term term, BiFunction<DynTruth,NAR,Truth> o, boolean beliefOrGoal, NAR timeAware) {
        return (Truth) eval(term, o, false, beliefOrGoal, 0, 0, Float.MIN_NORMAL /*Truth.EVI_MIN*/, timeAware);
    }

    public final Task task(Term term, BiFunction<DynTruth,NAR,Truth> o, boolean beliefOrGoal, NAR timeAware) {
        return (Task) eval(term, o, true, beliefOrGoal, 0, 0, Float.MIN_NORMAL /*Truth.EVI_MIN*/, timeAware);
    }

    public static class DynamicTruthTask extends NALTask {

        DynamicTruthTask(Term c, boolean beliefOrGoal, Truth tr, TimeAware timeAware, long start, long end, long[] stamp) throws InvalidTaskException {
            super(c, beliefOrGoal ? Op.BELIEF : Op.GOAL, tr, timeAware.time(), start, end, stamp);
            if (c.op()==NEG)
                throw new UnsupportedOperationException(c + " has invalid task content op (NEG)");
        }

//        @Override
//        public DynamicTruthTask pri(float p) {
//            super.pri(p);
//            return this;
//        }


        @Override
        public boolean isInput() {
            return false;
        }

    }

}
