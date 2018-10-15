package nars.truth.dynamic;

import jcog.TODO;
import jcog.Util;
import jcog.data.list.FasterList;
import jcog.math.LongInterval;
import jcog.pri.Prioritized;
import jcog.pri.ScalarValue;
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
import java.util.function.Function;

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

    protected float pri(long start, long end) {

        //TODO maybe instead of just range use evi integration


        if (start == ETERNAL) {
            //TODO if any sub-tasks are non-eternal, maybe combine in proportion to their relative range / evidence
            return reapply(DynTruth::pri, Param.DerivationPri);
        } else {


            long range = (end - start) + 1;

            return reapply(sub -> {
                float subPri = DynTruth.pri(sub);
                long subRange;
                if (sub.isEternal() || ((subRange = sub.range()) >= range)) {
                    return subPri;
                } else {
                    return (float)(subPri * ((double)subRange) / range);
                }
            }, Param.DerivationPri);

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
    public double coord(int dimension, boolean maxOrMin) {
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


    public Task task(Term content, Truth t, Function<Random,long[]> stamp, boolean beliefOrGoal, NAR nar) {

        if (content == null) {
            throw new NullPointerException("template is null; missing information how to build");
            //return null;
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
                long earliest = TIMELESS;
                long minRange = TIMELESS;
                boolean eternals = false;
                for (int i = 0, thisSize = size(); i < thisSize; i++) {
                    LongInterval ii = get(i);
                    long iis = ii.start();
                    if (iis != ETERNAL) {
                        earliest = Math.min(earliest, iis);
                        long tRange = ii.end() - iis;
                        minRange = (minRange != TIMELESS) ? Math.min(minRange, tRange) : tRange;
                    } else {
                        eternals = true;
                    }
                }


                if (eternals && earliest == TIMELESS) {
                    start = end = ETERNAL;
                } else {
                    assert (earliest != TIMELESS);

                    if (minRange == TIMELESS)
                        minRange = 0;

                    start = earliest;
                    end = (earliest + minRange);
                }


            } else {
                long[] u = Tense.union(this.array());
                start = u[0];
                end = u[1];
            }

            start = Tense.dither(start, nar);
            end = Tense.dither(end, nar);
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
                nar, start, end,
                stamp.apply(nar.random()));

        dyn.cause( cause() );

        dyn.pri(Math.max(ScalarValue.EPSILON, pri(start, end)));

        if (Param.DEBUG_EXTRA)
            dyn.log("Dynamic");

        return dyn;
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
