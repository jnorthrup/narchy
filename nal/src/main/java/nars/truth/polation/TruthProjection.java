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
import nars.term.Compound;
import nars.term.Term;
import nars.term.util.Intermpolate;
import nars.term.util.TermException;
import nars.time.Tense;
import nars.truth.Stamp;
import nars.truth.Truth;
import nars.truth.dynamic.TaskList;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.IntPredicate;
import java.util.function.Supplier;

import static nars.term.atom.Bool.Null;
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

    long start;
    long end;
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
    public abstract Truth truth(double eviMin, boolean dither, boolean tCrop, NAR nar);

    public final boolean add(TaskRegion t) {
        return add(t.task());
    }

    public boolean add(Task t) {
        if (t == null)
            throw new NullPointerException();
        return add(new TaskComponent(t));
    }


    @Override
    protected final TaskComponent[] newArray(int newCapacity) {
        return new TaskComponent[newCapacity];
    }


    private boolean update(TaskComponent tc, boolean force) {
        double e = tc.evi;
        if (force || (e!=e)) {
            tc.evi = e = evi(tc.task);
        }
        return e==e;
    }


    private double evi(Task task) {
        double e;
        if (start == ETERNAL) {
            if (!task.isEternal())
                throw new WTF("eternal truthpolation requires eternal tasks");
            e = task.evi();
        } else {
            e = TruthIntegration.evi(task, start, end, dur);
        }
        return e < Param.truth.TRUTH_EVI_MIN ? Double.NaN : e;
    }


    /**
     * removes the weakest components sharing overlapping evidence with stronger ones.
     * should be called after all entries are added
     * @param needStamp whether a stamp result should be returned, or if this can be elided if not necessary
     */
    @Nullable
    public final MetalLongSet commit(boolean tCrop, int minResults, boolean needStamp) {
        int s = size();
        if (s < minResults) {
            return null;
        } else if (s == 1) {
            return only(tCrop, needStamp);
        } else {
            MetalLongSet e = filterCyclicN(minResults, tCrop || start==ETERNAL, needStamp);
            return needStamp ? e : null;
        }
    }

    int active() {
        return count(TaskComponent::valid);
    }

    private int update(boolean force) {
        if (force)
            invalidate();

        int s = size();
        if (s <= 0)
            return 0;

        int count = 0;
        for (int i = 0; i < s; i++) {
            if (update(get(i), force))
                count++;
        }
        if (count > 0)
            sortByEvidence();
        return count;
    }

 @Nullable private MetalLongSet filterCyclicN(int minComponents, boolean tCrop, boolean needStamp) {

        assert (minComponents >= 1);

        int ss = size();

        long bs = this.start, be = this.end;

        MetalLongSet e = null;
        while (ss > 1) {
            int activeRemain = refocus(tCrop, true);
            if (activeRemain < minComponents) {
                //OOPS
                // TODO undo
                return null;
            }
            if (activeRemain == 1) {
                //throw new WTF();
                return ((e == null) && needStamp) ? Stamp.toMutableSet(firstValid().task) : e;
            }

            if (e == null) {
                //optimized special case
                if (activeRemain == 2 && !needStamp) {
                    if (!Stamp.overlaps(get(0).task, get(1).task))
                        break;
                    else {
                        //TODO prevent unnecessary MetalLongSet creation
                    }
                }
                e = new MetalLongSet(Param.STAMP_CAPACITY); //first iteration
            } else
                e.clear(); //2nd iteration, or after


            MetalBitSet conflict = MetalBitSet.bits(ss);
            for (int i = 0; i < ss; i++) {
                TaskComponent c = get(i);
                if (!c.valid())
                    continue;

                long[] iis = c.task.stamp();

                if (i > 0 && Stamp.overlapsAny(e, iis)) {
                    conflict.set(i);
                } else {
                    e.addAll(iis);
                }
            }

            int conflicts = conflict.cardinality();

            //1. test if they are all independent.  then all may be combined.
            if (conflicts == 0)
                break; //all ok

            ss = activeRemain;

            //something must be removed:
            double valueConflicting = eviSum(conflict::get);
            double valueOK  = eviSum(conflict::getNot);
            if (valueOK > valueConflicting) {
                if (ss - conflicts < minComponents)
                    return null; //impossible: nothing else to remove

                removeAll(conflict);

                ss -= conflicts;
            } else {
                if (conflicts < minComponents)
                    return null; //impossible: nothing else to remove

                conflict.negate();

                //TODO this could be destructive, so try it only: disableAll(conflict)
                removeAll(conflict);

                ss = conflicts;
            }

        }

        if (this.time(bs, be)) {
            int sss = refocus(tCrop, false);
            assert(sss >= minComponents);
        }

        return e;
    }

    private void removeAll(MetalBitSet x) {
        int c = x.cardinality();
        if (c == 0) {

        } else if (c == 1) {
            removeFast(x.first(true));
        } else {
            int ss = size();
            for (int i = 0; i < ss; i++) {
                if (x.get(i))
                    setFast(i, null);
            }
            removeNulls();
        }
    }

    private boolean time(long bs, long be) {
        if (this.start!=bs || this.end != be) {
            this.start = bs; this.end = be;
            return true;
        }
        return false;
    }

//    private void cull(int minComponents, int conflict) {
//        if (minComponents <= 1)
//            oneForAll(conflict);
//        else
//            removeFast(conflict);
//    }

    private void sortByEvidence() {
        if (size() > 1)
            sortThisByDouble(tc -> (tc.evi==tc.evi) ? -tc.evi : Double.POSITIVE_INFINITY); //TODO also sort by occurrence and/or stamp to ensure oldest task is always preferred
    }

//    /**
//     * a one-for-all and all-for-one decision
//     */
//    private void resolve(int conflict) {
//        if (size() == 2) {
//            removeFast(1); //the weaker will obviously be the 1th one regardless
//            return;
//        }
//        TaskComponent cc = get(conflict);
//        if (cc.evi < eviSumExcept(conflict)) {
//            removeFast(conflict);
//        } else {
//            //remove all except strongest conflict
//            clear();
//            add(cc);
//        }
//    }


    private double eviSum(IntPredicate each) {
        double e = 0;
        int n = size();
        for (int i = 0; i < n; i++) {
            if (each.test(i)) {
                TaskComponent c = get(i);
                double ce = c.evi;
                if (ce == ce)
                    e += ce;
            }
        }
        return e;
    }

    private MetalLongSet only(boolean tCrop, boolean provideStamp) {
        refocus(tCrop, true);
        return provideStamp ? Stamp.toMutableSet(firstValid().task) : null;
    }

    private TaskComponent firstValid() {
        int i = firstValidIndex();
        return i!=-1 ? get(i) : null;
    }

    private int firstValidIndex() {
        return indexOf(TaskComponent::valid);
    }
    private int firstValidIndex(int after) {
        return indexOf(after, TaskComponent::valid);
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
    private TruthProjection addTasks(Iterable<? extends Task> tasks) {
        tasks.forEach(this::add);
        return this;
    }

    public final TruthProjection add(Collection<? extends Tasked> tasks) {
        ensureCapacity(tasks.size());
        return add((Iterable) tasks);
    }
    public final TruthProjection addTasks(Collection<? extends Task> tasks) {
        ensureCapacity(tasks.size());
        return add((Iterable) tasks);
    }

    private TruthProjection add(Tasked tt) {
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


            for (int next = root+1; next < size; next++) {
                int tryB = firstValidIndex(next);
                if (tryB!=-1) {
                    TaskComponent B = get(tryB);
                    Term a = first;
                    Term b = B.task.term();
                    final double e2Evi = B.evi;

                    if ((Float.isFinite(dtDiff(a, b)))) {

                        final double e1Evi = rootComponent.evi;

                        Term ab;
                        try {
                            //if there isnt more evidence for the primarily sought target, then just use those components
                            ab = Intermpolate.intermpolate((Compound)a, (Compound)b, (float) (e1Evi / (e1Evi + e2Evi)), nar);
                        } catch (TermException e) {
                            //HACK TODO avoid needing to throw exception
                            if (Param.DEBUG) {
                                throw new RuntimeException(e);
                            } else {
                                //ignore.  it may be a contradictory combination of events.
                                ab = Null;
                            }
                        }

                        if (Task.validTaskTerm(ab)) {

                            this.term = ab;
                            for (int i = 0; i < size; i++)
                                if (i != root && i != tryB)
                                    set(i, null);
                            removeNulls();
                            //return 1 - dtDiff * 0.5f; //half discounted
                            //return 1 - dtDiff;
                            return 1; //no discount for difference
                        }


                    }
                    set(tryB, null); //eliminate and continue
                }
            }


            //last option: remove all except the first
            removeNulls();

            this.term = first;
            return 1;


        }


    }


    public byte punc() {
        if (isEmpty()) throw new RuntimeException();
        return get(0).task.punc();
    }


    @Nullable
    public final Truth truth() {
        return truth(Param.truth.TRUTH_EVI_MIN, false, false, null);
    }


    public void print() {
        forEach(t -> System.out.println(t.task.proof()));
    }

    public final long start() {
        return start;
    }

    public final long end() {
        return end;
    }


    /**
     * aka "shrinkwrap", or "trim". use after filtering cyclic.
     * adjust start/end to better fit the (remaining) task components and minimize temporalizing truth dilution.
     * if the start/end has changed, then evidence for each will need recalculated
     * returns the number of active tasks
     * @param all - true if applying to the entire set of tasks; false if applying only to those remaining active
     */
    private int refocus(boolean tCrop, boolean all) {
        if (tCrop || start == ETERNAL) {
            final long u0, u1;
            if ((all ? size() : active()) > 1) {
                long[] union = Tense.union(Iterables.transform(this,
                        all ?
                                ((TaskComponent x) -> x.task) :
                                ((TaskComponent x) -> x.valid() ? x.task : null)));
                u0 = union[0]; u1 = union[1];
            } else {
                TruthProjection.TaskComponent only = all ? getFirst() : firstValid();
                u0 = only.task.start(); u1 = only.task.end();
            }

            boolean changed = false;
            if (u0 != ETERNAL) {
                if (start == ETERNAL) {
                    //override eternal range with the entire calculated union
                    start = u0; this.end = u1; changed = true;
                } else {
                    if (start < u0 && u0 < this.end) {
                        start = u0;
                        changed = true;
                    }
                    if (this.end > u1 && u1 > start) {
                        this.end = u1;
                        changed = true;
                    }
                }
            } else {
                if (start!=ETERNAL) {
                    start = end = ETERNAL; changed = true;
                }
            }

            if (changed)
                return update(true);
        }

        return update(false);
    }

    private void invalidate() {
        forEach(TaskComponent::invalidate);
    }

    public Supplier<long[]> stamper(Supplier<Random> rng) {
        @Nullable MetalLongSet s = Stamp.toMutableSet(
                Param.STAMP_CAPACITY,
                i -> get(i).task.stamp(),
                size()); //calculate stamp after filtering and after intermpolation filtering
        if (s.size() > Param.STAMP_CAPACITY) {
            return ()->Stamp.sample(Param.STAMP_CAPACITY, s, rng.get());
        } else {
            return s::toSortedArray;
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
        }
        return t;
    }

    public final void forEachTask(Consumer<Task> each) {
        forEachWith((x, e) -> {
            if (x.valid())
                e.accept(x.task);
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
        double evi = Double.NaN;

        TaskComponent(Task task) {
            this.task = task;
        }

        @Override
        public String toString() {
            return task + "=" + evi;
        }

        public boolean valid() {
            return evi == evi;
        }

        @Override
        public @Nullable Task task() {
            return task;
        }

        void invalidate() {
            evi = Double.NaN;
        }
    }


}
