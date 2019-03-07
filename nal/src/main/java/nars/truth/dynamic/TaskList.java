package nars.truth.dynamic;

import jcog.TODO;
import jcog.Util;
import jcog.data.list.FasterList;
import jcog.math.LongInterval;
import jcog.pri.Prioritized;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.control.CauseMerge;
import nars.task.DynamicTruthTask;
import nars.task.NALTask;
import nars.task.util.TaskRegion;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Random;
import java.util.function.Function;

/**
 * A List of Task's which can be used for various purposes, including dynamic truth and evidence calculations (as utility methods)
 */
public class TaskList extends FasterList<Task> implements TaskRegion {


    public TaskList(int initialCap) {
        super(initialCap);
    }

    public TaskList(int size, Task[] t) {
        super(size, t);
//        for (Task x : t) if (x == null) throw new NullPointerException(); //TEMPORARY
    }

    private static float pri(TaskRegion x) {
        return ((Prioritized) x).priElseZero();
    }

    @Override
    protected Object[] newArray(int newCapacity) {
        return new Task[newCapacity];
    }

    @Override
    public long start() {

        long start = longify((long m, Task t)->{
            long s = t.start();
            return s != ETERNAL && s < m ? s : m;
        }, TIMELESS);

        if (start == TIMELESS)
            return ETERNAL;
        else
            return start;
    }

    @Override
    public long end() {
        return maxValue(LongInterval::end);
    }

    @Override
    public float coordF(int dimension, boolean maxOrMin) {
        throw new TODO();
    }

    @Override
    @Nullable
    public short[] cause() {
        return CauseMerge.AppendUnique.merge(Param.causeCapacity.intValue(),
                Util.map(0, size(), short[][]::new, x -> get(x).cause()));
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


    public final Task task(Term content, Truth t, Function<Random,long[]> stamp, boolean beliefOrGoal, long start, long end, NAR nar) {

        NALTask dyn = DynamicTruthTask.Task(content, t, stamp, beliefOrGoal, start, end, nar);

        dyn.cause( cause() );

        dyn.pri(
                //pri(start, end)
                reapply(TaskList::pri, Param.DerivationPri)
                        * dyn.originality() //HACK
        );

        if (Param.DEBUG_EXTRA)
            dyn.log("Dynamic");

        return dyn;
    }


    //    protected float pri(long start, long end) {
//
//        //TODO maybe instead of just range use evi integration
//
//
//        if (start == ETERNAL) {
//            //TODO if any sub-tasks are non-eternal, maybe combine in proportion to their relative range / evidence
//            return reapply(DynTruth::pri, Param.DerivationPri);
//        } else {
//
//
//            double range = (end - start) + 1;
//
//            return reapply(sub -> {
//                float subPri = DynTruth.pri(sub);
//                long ss = sub.start();
//                double pct = ss!=ETERNAL ? (1.0 + Longerval.intersectLength(ss, sub.end(), start, end))/range : 1;
//                return (float) (subPri * pct);
//            }, Param.DerivationPri);
//
//        }
//    }
}
