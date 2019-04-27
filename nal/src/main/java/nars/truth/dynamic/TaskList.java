package nars.truth.dynamic;

import jcog.TODO;
import jcog.Util;
import jcog.data.list.FasterList;
import jcog.math.LongInterval;
import jcog.pri.Prioritized;
import nars.NAL;
import nars.Task;
import nars.attention.What;
import nars.control.CauseMerge;
import nars.task.DynamicTruthTask;
import nars.task.NALTask;
import nars.task.util.TaskRegion;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.function.Supplier;

/**
 * A List of Task's which can be used for various purposes, including dynamic truth and evidence calculations (as utility methods)
 */
public class TaskList extends FasterList<Task> implements TaskRegion {


    public TaskList(int initialCap) {
        super(0, new Task[initialCap]);
    }

    public TaskList(Collection<Task> t) {
        super(0, new Task[t.size()]); t.forEach(this::addFast);
    }

    public TaskList(Iterable<Task> t, int sizeEstimate) {
        super(0, new Task[sizeEstimate]); t.forEach(this::add);
    }

    private static float pri(TaskRegion x) {
        return ((Prioritized) x).priElseZero();
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
    @Nullable
    public short[] why() {
        return CauseMerge.AppendUnique.merge(NAL.causeCapacity.intValue(),
                Util.map(0, size(), short[][]::new, x -> get(x).why()));
    }

    @Override
    public float freqMin() {
        throw new TODO();
    }

    @Override
    public float freqMax() {
        throw new TODO();
    }

    @Override
    public float confMin() {
        throw new TODO();
    }

    @Override
    public float confMax() {
        throw new TODO();
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


    public final Task merge(Term content, Truth t, Supplier<long[]> stamp, boolean beliefOrGoal, long start, long end, What w) {

        NALTask dyn = DynamicTruthTask.task(content, t, stamp, beliefOrGoal, start, end, w);
        if(dyn==null)
            return null;

        dyn.cause( why() );

        dyn.pri(
                reapply(TaskList::pri, NAL.DerivationPri)
                        // * dyn.originality() //HACK
        );

        if (NAL.test.DEBUG_EXTRA)
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
