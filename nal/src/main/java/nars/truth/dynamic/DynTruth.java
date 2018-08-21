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
import nars.truth.Truth;
import nars.util.TimeAware;
import org.eclipse.collections.api.tuple.primitive.ObjectBooleanPair;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static nars.Op.*;

/**
 * Created by me on 12/4/16.
 */
public class DynTruth extends FasterList<Task> implements TaskRegion {



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

    public float pri(long start, long end) {

        int s = size;
        assert (s > 0);

        if (start == ETERNAL)
            return meanValue(DynTruth::pri);

        double total = 0;
        double range = (end-start)+1;
        for (TaskRegion d : this) {
            float p = DynTruth.pri(d);
            if (d.task().isEternal()) {
                total += p;
            } else {
                total += p * d.range() / range;
            }
        }
        return (float) total;
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



    @Override
    public @Nullable Task task() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends Task> source) {
        source.forEach(this::add);
        return true;
    }

    public final Task task(Term term, Truth t, Function<Random, long[]> stamp, boolean beliefOrGoal, NAR n) {
        //private Truthed eval(Supplier<Term> superterm, Truth t, boolean taskOrJustTruth, boolean beliefOrGoal, float freqRes, float confRes, float eviMin, NAR nar) {
        assert(t!=null);
        return task(()->term, t, stamp, beliefOrGoal, n);

    }


    public Task task(Supplier<Term> term, Truth t, Function<Random,long[]> stamp, boolean beliefOrGoal, NAR nar) {

        Term content = term.get();
        if (content == null) {
            //throw new NullPointerException("template is null; missing information how to build");
            return null;
        }

        Op op = content.op();
        if (op == NEG) {
            content = content.unneg();
            op = content.op();
            t = t.neg();
        }

        long start, end;
        if (size() > 1) {
            if (op == CONJ) {
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
                long[] u = Param.DynamicTruthTimeMerge(this.array());
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

        NALTask dyn = new DynamicTruthTask(
                r.getOne(), beliefOrGoal,
                r.getTwo() ? t.neg() : t,
                nar, Tense.dither(start, nar), Tense.dither(end, nar),
                stamp.apply(nar.random()));

        dyn.cause = cause();

        dyn.pri(pri(start, end));

        if (Param.DEBUG_EXTRA)
            dyn.log("Dynamic");

        return dyn;
    }



    public <T extends Task> Consumer<T> adding(@Nullable Predicate<Task> filter) {
        return filter==null ? this::add : (T x) -> {
            if (filter.test(x))
                add(x);
        };
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
