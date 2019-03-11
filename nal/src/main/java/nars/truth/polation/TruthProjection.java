package nars.truth.polation;

import com.google.common.collect.Iterables;
import jcog.Paper;
import jcog.Skill;
import jcog.WTF;
import jcog.data.bit.MetalBitSet;
import jcog.data.list.FasterList;
import jcog.data.set.MetalLongSet;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.task.Tasked;
import nars.task.util.TaskRegion;
import nars.term.Term;
import nars.term.util.Intermpolate;
import nars.time.Tense;
import nars.truth.Stamp;
import nars.truth.Truth;
import nars.truth.dynamic.TaskList;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Random;
import java.util.function.Consumer;

import static java.lang.Float.NaN;
import static nars.term.util.Intermpolate.dtDiff;
import static nars.time.Tense.ETERNAL;

/**
 * Truth Interpolation and Extrapolation of Temporal Beliefs/Goals
 * see:
 * https:
 * https:
 */
@Paper
@Skill({"Interpolation", "Extrapolation"})
abstract public class TruthProjection extends FasterList<TruthProjection.TaskComponent> {

    public long start, end;
    int dur;

    /**
     * content target, either equal in all the tasks, or the result is
     * intermpolated (and evidence reduction applied as necessary)
     */
    public Term term = null;

    TruthProjection(long start, long end, int dur) {
        super(0);
        this.start = start;
        this.end = end;

        assert (dur >= 0);
        this.dur = dur;
    }

    /**
     * computes the final truth value
     */
    @Nullable
    public abstract Truth truth(float eviMin, boolean dither, NAR nar);

    @Nullable
    public final Truth truth(float eviMin, NAR nar) {
        return truth(eviMin, false, nar);
    }

    public final boolean add(TaskRegion t) {
        return add(t.task());
    }

    public boolean add(Task t) {
        return add(new TaskComponent(t));
    }


    @Override
    protected final TaskComponent[] newArray(int newCapacity) {
        return new TaskComponent[newCapacity];
    }


    @Nullable
    protected boolean update(TaskComponent tc, boolean force) {
        float e = tc.evi;
        if (force || (e!=e)) {
            tc.evi = e = evi(tc.task);
        }
        return e==e;
    }


    protected float evi(Task task) {
        if (start == ETERNAL) {
            if (!task.isEternal())
                throw new WTF("eternal truthpolation requires eternal tasks");
            return task.evi();
        } else {
            return TruthIntegration.evi(task, start, end, dur);
        }
    }


    /**
     * removes the weakest components sharing overlapping evidence with stronger ones.
     * should be called after all entries are added
     */
    @Nullable
    public final MetalLongSet commit(boolean provideStamp, int minResults) {


        if (size() < minResults) return null;

        int s = refocus();

        if (s < minResults) return null;
        else if (s == 1) {
            return only(provideStamp);
        } else {
            MetalLongSet e = filterCyclicN(minResults);
            return provideStamp ? e : null;
        }
    }
    public int active() {
        return count(TaskComponent::valid);
    }

    private int update(boolean force) {
        int s = size();
        if (s > 0) {
            int count = 0;
            for (int i = 0; i < s; i++) {
                if (update(get(i), force))
                    count++;
            }
            if (count > 0)
                sortByEvidence();
            return count;
        }
        return 0;
    }

    private MetalLongSet filterCyclicN(int minComponents) {

        assert (minComponents >= 1);

        int ss = size();

        MetalLongSet e = null;
        do {

            if (e == null) {
                e = new MetalLongSet(Param.STAMP_CAPACITY); //first iteration
            } else {
                //2nd iteration, or after

                int activeRemain = refocus();
                if (activeRemain < minComponents) {
                    //OOPS
                    // TODO undo
                    return null;
                }
                e.clear();
            }


            int weakestConflict = Integer.MIN_VALUE, strongestConflict = Integer.MAX_VALUE;
            for (int i = ss - 1; i >= 0; i--) {
                TaskComponent c = get(i);
                if (!c.valid())
                    continue;
                Task ii = c.task;
                long[] iis = ii.stamp();
                if (i < ss - 1) {
                    if (Stamp.overlapsAny(e, iis)) {
                        strongestConflict = Math.min(i, strongestConflict);
                        weakestConflict = Math.max(i, weakestConflict);
                        continue;
                    }
                }
                e.addAll(iis); //first
            }

            //1. test if they are all independent.  then all may be combined.
            if (weakestConflict < 0)
                return e; //all ok


            //something must be removed:
            if (ss - 1 < minComponents)
                return null; //impossible: nothing else to remove

            if (strongestConflict == ss - 1) {
                //2. if the only overlapping task is the least ranked, just remove that and continue.
                removeLastFast();
                return e;
            } else {
                if (strongestConflict == weakestConflict) {
                    //it is only conflict present in this set.
                    // eliminate it if the sum of the other evidences are greater
                    cull(minComponents, strongestConflict);
                } else {

                    //TODO try evaluating the truth by removing each of the conflicting tasks

                    //for now, try removing the weakestConflict and repeat
                    cull(minComponents, weakestConflict);

                    //TODO early exit possible here if weakestConflict == 0, then 'e' will be correct for return in some cases
                }

            }

            ss = size();
            if (ss < minComponents)
                return null;

        } while (ss > 1);

        if (ss == 1)
            return e;

        return null;

    }

    private void cull(int minComponents, int conflict) {
        if (minComponents <= 1)
            oneForAll(conflict);
        else
            removeFast(conflict);
    }

    private void sortByEvidence() {
        sortThisByFloat(tc -> -tc.evi); //TODO also sort by occurrence and/or stamp to ensure oldest task is always preferred
    }

    /**
     * a one-for-all and all-for-one decision
     */
    private void oneForAll(int conflict) {
        if (size() == 2) {
            removeFast(1); //the weaker will obviously be the 1th one regardless
            return;
        }
        TaskComponent cc = get(conflict);
        if (cc.evi < eviSumExcept(conflict)) {
            removeFast(conflict);
        } else {
            //remove all except strongest conflict
            clear();
            add(cc);
        }
    }

    public double eviSum() {
        return eviSumExcept(-1);
    }

    public double eviSumExcept(int task) {
        double e = 0;
        int n = size();
        for (int i = 0; i < n; i++) {
            if (i != task) {
                TaskComponent c = get(i);
                float ce = c.evi;
                if (ce == ce)
                    e += ce;
            }
        }
        return e;
    }

    private MetalLongSet only(boolean provideStamp) {
        return provideStamp ? Stamp.toMutableSet(firstValid().task) : null;
    }

    private TaskComponent firstValid() {
        return get(firstValidIndex());
    }

    protected int firstValidIndex() {
        return indexOf(TaskComponent::valid);
    }

    public final boolean valid(int i) {
        return get(i).valid();
    }

    public final TruthProjection add(Tasked... tasks) {
        ensureCapacity(tasks.length);
        for (Tasked t : tasks)
            add(t); //if (t != null)
        return this;
    }

    private TruthProjection add(Iterable<? extends Tasked> tasks) {
        tasks.forEach(this::add);
        return this;
    }

    public final TruthProjection add(Collection<? extends Tasked> tasks) {
        ensureCapacity(tasks.size());
        return add((Iterable) tasks);
    }

    public final TruthProjection add(Tasked tt) {
        add(tt.task());
        return this;
    }

    float intermpolateAndCull(NAR nar) {

        final int root = firstValidIndex();
        int thisSize = size();
        TaskComponent rootComponent = get(root);
        Term first = rootComponent.task.term();
        if (thisSize == 1 || !first.hasAny(Op.Temporal)) {
            //assumes that all the terms are from the same concept.  so if the first target has no temporal components the rest should not either.
            this.term = first;
            return 1;
        }


        MetalBitSet matchingFirst = MetalBitSet.bits(thisSize);
        matchingFirst.set(root);
        for (int i = firstValidIndex()+1; i < thisSize; i++) {
            TaskComponent t = this.get(i);
            if (t.valid()) {
                Term ttt = t.task.term();
                if (first.equals(ttt))
                    matchingFirst.set(i);
            }
        }
        if (matchingFirst.cardinality() > 1) {
            this.term = first;
            //exact matches are present.  remove those which are not
            for (int i = firstValidIndex()+1; i < thisSize; i++)
                if (!matchingFirst.get(i))
                    set(i, null);
            removeNulls();
            return 1f;
        } else {

            //use the first successful intermpolation, if possible

            int tryB = 1;
            final float e1Evi = rootComponent.evi;
            Term a = first;
            TaskComponent B = get(tryB);
            Term b = B.task.term();

            if ((Float.isFinite(dtDiff(a, b)))) {

                final float e2Evi = B.evi;

                //if there isnt more evidence for the primarily sought target, then just use those components
                Term ab = Intermpolate.intermpolate(a,
                        b, e1Evi / (e1Evi + e2Evi), nar);

                if (Task.validTaskTerm(ab)) {

                    this.term = ab;
                    removeAbove(1 + 1);
                    assert (size() == 2);
                    //return 1 - dtDiff * 0.5f; //half discounted
                    //return 1 - dtDiff;
                    return 1; //no discount for difference
                }


            }


            //last option: remove all except the first
            removeAbove(1);
            assert (size() == 1);
            this.term = a;
            return 1;


        }


    }


    public byte punc() {
        if (isEmpty()) throw new RuntimeException();
        return get(0).task.punc();
    }


    @Nullable
    public final Truth truth() {
        return truth(Float.MIN_NORMAL, null);
    }


    public void print() {
        forEach(t -> System.out.println(t.task.proof()));
    }

    public long start() {
        return start;
    }

    public long end() {
        return end;
    }

//    private void cull() {
//        removeIf(x -> update(x, Float.MIN_NORMAL) == null);
//    }

    boolean allInvalid() {
        return !this.anySatisfy(TaskComponent::valid);
    }

    /**
     * aka "shrinkwrap", or "trim". use after filtering cyclic.
     * adjust start/end to better fit the (remaining) task components and minimize temporalizing truth dilution.
     * if the start/end has changed, then evidence for each will need recalculated
     * returns the number of active tasks
     */
    private int refocus() {
        long[] se;
        boolean allInvalid = allInvalid(); //uninitialized or not

        if ((allInvalid ? size() : active()) > 1) {
            se = Tense.union(Iterables.transform(this,
                    allInvalid ?
                            ((TaskComponent x) -> x.task) :
                            ((TaskComponent x) -> x.valid() ? x.task : null)));
        } else {
            TruthProjection.TaskComponent only = allInvalid ? getFirst() : firstValid();
            se = new long[]{only.task.start(), only.task.end()};
        }

        if (se[0] != ETERNAL) {
            if (start == ETERNAL) {
                //override eternal range with the calculated union
            } else {
                se[0] = Math.min(end, Math.max(start, se[0]));
                se[1] = Math.max(start, Math.min(end, se[1]));
            }
        }
        if (allInvalid || (se[0] != start || se[1] != end)) {
            if (!allInvalid)
                invalidate();

            start = se[0];
            end = se[1];

            return update(true);
        } else {
            return update(false);
        }

    }

    private void invalidate() {
        forEach(TaskComponent::invalidate);
    }

    public long[] stamper(Random rng) {
        @Nullable MetalLongSet s = Stamp.toMutableSet(
                Param.STAMP_CAPACITY,
                i -> get(i).task.stamp(),
                size()); //calculate stamp after filtering and after intermpolation filtering
        if (s.size() > Param.STAMP_CAPACITY) {
            return Stamp.sample(Param.STAMP_CAPACITY, s, rng);
        } else {
            return s.toSortedArray();
        }
    }

    @Deprecated
    public TaskList list() {
        int thisSize;
        TaskList t = new TaskList(thisSize = this.size());
        for (int i = 0; i < thisSize; i++) {
            TaskComponent x = this.get(i);
            if (x.valid())
                t.add(x.task);
            else
                System.out.println("skipped");
        }
        return t;
    }

    public void forEachTask(Consumer<Task> each) {
        forEachWith((x, e) -> {
            if (x.valid()) {
                e.accept(x.task);
            } else {
                System.out.println("skipped");
            }
        }, each);
    }


    /**
     * TODO extend TaskList as TruthTaskList storing evi,freq pairs of floats in a compact float[]
     */
    @Deprecated
    public static class TaskComponent implements Tasked {
        final Task task;

        /**
         * NaN if not yet computed
         */
        float evi = NaN;

        TaskComponent(Task task) {
            this.task = task;
        }

        @Override
        public String toString() {
            return task + "=" + evi;
        }

        boolean valid() {
            return evi == evi;
        }

        @Override
        public @Nullable Task task() {
            return task;
        }

        public void invalidate() {
            evi = Float.NaN;
        }
    }


}
