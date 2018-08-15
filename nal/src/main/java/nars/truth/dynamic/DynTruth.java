package nars.truth.dynamic;

import jcog.TODO;
import jcog.Util;
import jcog.data.list.FasterList;
import jcog.math.LongInterval;
import jcog.pri.Prioritized;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.control.Cause;
import nars.control.proto.TaskLinkTask;
import nars.task.ITask;
import nars.task.NALTask;
import nars.task.util.TaskException;
import nars.task.util.TaskRegion;
import nars.term.Term;
import nars.time.Tense;
import nars.truth.PreciseTruth;
import nars.truth.Stamp;
import nars.truth.Truth;
import nars.truth.Truthed;
import nars.util.TimeAware;
import org.eclipse.collections.api.tuple.primitive.ObjectBooleanPair;
import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import static nars.Op.*;
import static nars.truth.TruthFunctions.c2wSafe;
import static nars.truth.TruthFunctions.w2cSafe;

/**
 * Created by me on 12/4/16.
 */
public final class DynTruth extends FasterList<TaskRegion> implements Prioritized, TaskRegion {

    private LongHashSet evi = null;

    public DynTruth(int initialCap) {
        super(initialCap);
    }

    private static float pri(TaskRegion x) {
        return ((Prioritized) x).priElseZero();
    }

    @Override
    protected Object[] newArray(int newCapacity) {
        return new TaskRegion[newCapacity];
    }

    public float pri() {

        int s = size;
        assert (s > 0);

        if (s > 1 /* and all differ, because if they are equal then the average will be regardless of their relative rank */) {
            if (anySatisfy(t -> t.start() == ETERNAL))
                return meanValue(DynTruth::pri);

            double total = 0, totalEvi = 0;

            for (TaskRegion d : this) {
                float p = DynTruth.pri(d);
                double e = ((Truthed) d).conf() /*evi()*/ * d.range();
                total += p * e;
                totalEvi += e;
            }
            return (float) (total / totalEvi);

        } else {
            return pri(get(0));
        }
    }

    @Override
    public long start() {
        return minValue(LongInterval::start);
    }

    @Override
    public long end() {
        return maxValue(LongInterval::end);
    }

    @Override
    public double coord(boolean maxOrMin, int dimension) {
        throw new TODO();
    }

    @Override
    @Nullable
    public short[] cause() {
        return Cause.merge(Param.causeCapacity.intValue(),
                Util.map(0, size(), x -> get(x).cause(), short[][]::new));
    }

    /** eval without any specific time or truth dithering */
    public final Truthed eval(Term superterm, @Deprecated BiFunction<DynTruth, NAR, Truth> truthModel, boolean taskOrJustTruth, boolean beliefOrGoal, NAR nar) {
        return eval(superterm, truthModel, taskOrJustTruth,  beliefOrGoal, 0, 0, Float.MIN_NORMAL, nar);
    }
    /** eval without any specific time or truth dithering */
    public final Truthed eval(Supplier<Term> superterm, Truth t, boolean taskOrJustTruth, boolean beliefOrGoal, NAR nar) {
        return eval(superterm, t, taskOrJustTruth,  beliefOrGoal, 0, 0, Float.MIN_NORMAL, nar);
    }
    /**
     * TODO make Task truth dithering optional
     */
    @Deprecated  private Truthed eval(Term superterm, @Deprecated BiFunction<DynTruth, NAR, Truth> truthModel, boolean taskOrJustTruth, boolean beliefOrGoal, float freqRes, float confRes, float eviMin, NAR nar) {

        Truth t = truthModel.apply(this, nar);
        if (t == null)
            return null;
        return eval(()->superterm, t, taskOrJustTruth, beliefOrGoal, freqRes, confRes, eviMin, nar);
    }

    public Truthed eval(Supplier<Term> superterm, Truth t, boolean taskOrJustTruth, boolean beliefOrGoal, float freqRes, float confRes, float eviMin, NAR nar) {


        float evi = t.evi();
//        if (evi < Math.min(eviMin, c2wSafe(confRes)))
//            return null;


        float freq = t.freq();

        float f;


        if (taskOrJustTruth) {

            Term content = superterm.get();
            if (content == null)
                return null;

            if (content.op() == NEG) {
                content = content.unneg();
                f = 1.0f - freq;
            } else {
                f = freq;
            }

            long start, end;
            if (size() > 1) {
                if (content.op() == CONJ) {
                    long min = TIMELESS;
                    long minRange = 0;
                    boolean eternals = false;
                    for (int i = 0, thisSize = size(); i < thisSize; i++) {
                        LongInterval ii = get(i);
                        long iis = ii.start();
                        if (iis != ETERNAL) {
                            min = Math.min(min, iis);
                            minRange = Math.min(minRange, ii.end() - iis);
                        } else {
                            eternals = true;
                        }
                    }

                    if (eternals && min == TIMELESS) {
                        start = end = ETERNAL;
                    } else {
                        assert (min != TIMELESS);


                        start = min;
                        end = (min + minRange);
                    }


                } else {
                    long[] u = //Tense.union(this.array());
                            Tense.intersection(this.array());
                    start = u[0];
                    end = u[1];
                }
            } else {

                LongInterval only = get(0);
                start = only.start();
                end = only.end();
            }



            @Nullable ObjectBooleanPair<Term> r = Task.tryContent(
                    content,
                    beliefOrGoal ? BELIEF : GOAL, !Param.DEBUG_EXTRA);
            if (r == null)
                return null;

            PreciseTruth tr = Truth.theDithered(r.getTwo() ? (1 - f) : f, freqRes, evi, confRes, w2cSafe(eviMin));
            if (tr == null)
                return null; //TODO see if this can be detected earlier, by comparing evi before term construction


            NALTask dyn = new DynamicTruthTask(
                    r.getOne(), beliefOrGoal,
                    tr,
                    nar, Tense.dither(start, nar), Tense.dither(end, nar),
                    this.evi.toArray());


            dyn.cause = cause();

            dyn.pri(pri());

            if (Param.DEBUG_EXTRA)
                dyn.log("Dynamic");

            return dyn;

        } else {
            return Truth.theDithered(freq, freqRes, evi, confRes, w2cSafe(eviMin));
        }

    }

    @Override
    public @Nullable Task task() {
        throw new UnsupportedOperationException();
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
    public boolean add(@NotNull TaskRegion newItem) {

        super.add(newItem);

        if (newItem != null) {
            long[] stamp = ((Stamp) newItem).stamp();

            if (evi == null)
                evi = new LongHashSet(stamp.length * 2 /* estimate */);

            evi.addAll(stamp);

        }

        return true;
    }

    boolean doesntOverlap(Task task) {
        if (evi != null) {

            long[] s = task.stamp();
            for (long x : s) {
                if (evi.contains(x))
                    return false;
            }
        }
        return true;
    }

    public final Truth truth(Term term, BiFunction<DynTruth, NAR, Truth> o, boolean beliefOrGoal, NAR n) {
        return (Truth) eval(term, o, false, beliefOrGoal, n);
    }

    public final Task task(Term term, Truth t, boolean beliefOrGoal, NAR n) {
        return task(term, t, beliefOrGoal, false, n);

    }
    public final Task task(Term term, Truth t, boolean beliefOrGoal, boolean ditherTruth, NAR n) {
        //private Truthed eval(Supplier<Term> superterm, Truth t, boolean taskOrJustTruth, boolean beliefOrGoal, float freqRes, float confRes, float eviMin, NAR nar) {
        assert(t!=null);
        float fRes, cRes, eMin;
        if (ditherTruth) {
            fRes = n.freqResolution.floatValue();
            cRes = n.confResolution.floatValue();
            eMin = c2wSafe(n.confMin.floatValue());
        } else {
            fRes = Param.TRUTH_EPSILON;
            cRes = Float.MIN_NORMAL;
            eMin = Float.MIN_NORMAL;
        }
        return (Task) eval(()->term, t, true, beliefOrGoal,
                fRes,cRes,eMin, n);

    }


    static class DynamicTruthTask extends NALTask {

        DynamicTruthTask(Term c, boolean beliefOrGoal, Truth tr, TimeAware n, long start, long end, long[] stamp) throws TaskException {
            super(c, beliefOrGoal ? Op.BELIEF : Op.GOAL, tr, n.time(), start, end, stamp);
            if (c.op() == NEG)
                throw new UnsupportedOperationException(c + " has invalid task content op (NEG)");
        }

        @Override
        public ITask perceive(Task result, NAR n) {
            return new TaskLinkTask(this);
        }

        @Override
        public boolean isInput() {
            return false;
        }

    }

}
