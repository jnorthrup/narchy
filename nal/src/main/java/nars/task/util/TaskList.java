package nars.task.util;

import jcog.TODO;
import jcog.data.list.FasterList;
import jcog.data.set.MetalLongSet;
import jcog.math.LongInterval;
import jcog.pri.Prioritized;
import nars.Task;
import nars.term.Term;
import nars.truth.Stamp;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Random;
import java.util.function.Supplier;

/**
 * A List of Task's which can be used for various purposes, including dynamic truth and evidence calculations (as utility methods)
 */
public class TaskList extends FasterList<Task> implements TaskRegion {


    public TaskList(int initialCap) {
        super(0, initialCap > 0 ? new Task[initialCap] : Task.EmptyArray);
    }

    public TaskList(Task[] t, int n) {
        super(n, t);
    }

    public TaskList(Collection<Task> t) {
        super(0, new Task[t.size()]);
        for (Task task : t) {
            addFast(task);
        }
    }

    public TaskList(Iterable<Task> t, int sizeEstimate) {
        super(0, new Task[sizeEstimate]);
        for (Task task : t) {
            add(task);
        }
    }

    public static float pri(TaskRegion x) {
        return ((Prioritized) x).priElseZero();
    }

    public final Task[] arrayCommit() {
        int s = size;
        if (s == 0)
            return Task.EmptyArray;
        else if (s == items.length)
            return items;
        else
            return items = Arrays.copyOf(items, s);
    }

    @Override
    public long start() {

        long start = longify((m, t) ->{
            long s = t.start();
            return s != ETERNAL && s < m ? s : m;
        }, TIMELESS);

		return start == TIMELESS ? ETERNAL : start;
    }

    @Override
    public long end() {
        return maxValue(LongInterval::end);
    }

//    @Override
//    @Nullable
//    public short[] why() {
//        return CauseMerge.AppendUnique.merge(NAL.causeCapacity.intValue(),
//                Util.map(0, size(), short[][]::new, x -> get(x).why()));
//    }

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
        for (Task task : source) {
            add(task);
        }
        return true;
    }

    public Supplier<long[]> stamp(int capacity, Random rng) {
        int ss = size();
        switch (ss) {
            case 1:
                return () -> stamp(0);
            case 2:
                return ()-> {
                    long[] a = stamp(0), b = stamp(1);
//                    if (a == null && b == null) throw new NullPointerException();
//                    if (a == null) return b;
//                    if (b == null) return a;
                    return Stamp.sample(capacity, Stamp.toSet(a.length + b.length, a, b), rng);
                };
            default:

                return () -> {
                    @Nullable MetalLongSet stampSet = Stamp.toMutableSet(
                        capacity,
                        this::stamp,
                        ss); //calculate stamp after filtering and after intermpolation filtering
                    //assert(!stampSet.isEmpty());
					return stampSet.size() > capacity ? Stamp.sample(capacity, stampSet, rng) : stampSet.toSortedArray();
                };
        }
    }

    public final @Nullable long[] stamp(int component) {
        Task t = items[component];
        return t!=null ? t.stamp() : null;
    }

    public final Term term(int i) {
        return items[i].term();
    }

    public Truth truth(int i) {
        return items[i].truth();
    }

    public long taskRange(int i) {
        return items[i].range();
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
