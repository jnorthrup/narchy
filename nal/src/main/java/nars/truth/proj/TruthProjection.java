package nars.truth.proj;

import com.google.common.collect.Iterables;
import jcog.Paper;
import jcog.Skill;
import jcog.WTF;
import jcog.data.bit.MetalBitSet;
import jcog.data.list.FasterList;
import jcog.data.set.MetalLongSet;
import nars.NAL;
import nars.Op;
import nars.Task;
import nars.task.Tasked;
import nars.task.util.TaskList;
import nars.task.util.TaskRegion;
import nars.term.Compound;
import nars.term.Term;
import nars.term.util.Intermpolate;
import nars.term.util.TermTransformException;
import nars.time.Tense;
import nars.truth.Stamp;
import nars.truth.Truth;
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

    private static final TaskComponent[] Empty_TaskComponent_Array = new TaskComponent[0];

    long start, end;
    float dur;

    /**
     * content target, either equal in all the tasks, or the result is
     * intermpolated (and evidence reduction applied as necessary)
     */
    public Term term = null;

    TruthProjection(long start, long end, float dur) {
        super(0, Empty_TaskComponent_Array);
        this.start = start;
        this.end = end;

        assert (dur >= 0);
        this.dur = dur;
    }

    /**
     * computes the final truth value
     */
    @Nullable
    public abstract Truth truth(double eviMin, boolean dither, boolean tCrop, NAL nar);

    public final boolean add(TaskRegion t) {
        return add(t.task());
    }

    public boolean add(Task t) {
        if (t == null)
            throw new NullPointerException();
        return add(new TaskComponent(t));
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
        return e < NAL.truth.EVI_MIN ? Double.NaN : e;
    }


    /**
     * removes the weakest components sharing overlapping evidence with stronger ones.
     * should be called after all entries are added
     * @param needStamp whether a stamp result should be returned, or if this can be elided if not necessary
     */
    @Nullable
    public final MetalLongSet commit(boolean shrink, int minResults, boolean needStamp) {
        int s = size();
        if (s < minResults) {
            return null;
        } else if (s == 1) {
            return only(shrink, needStamp);
        } else {
            MetalLongSet e = filterCyclicN(minResults, shrink, needStamp);
            return needStamp ? e : null;
        }
    }

    int active() {
        return count(TaskComponent::valid);
    }

    public int update(boolean force) {
        int s = size();
        if (s <= 0)
            return 0;

        int count = 0;
        for (int i = 0; i < s; i++) {
            if (update(get(i), force))
                count++;
        }
        if (size() > 1)
            sortThisByDouble(TaskComponent::eviDescending); //TODO also sort by occurrence and/or stamp to ensure oldest task is always preferred
        return count;
    }

 @Nullable private MetalLongSet filterCyclicN(int minComponents, boolean shrink, boolean needStamp) {

        assert (minComponents >= 1);

        final long bs = this.start, be = this.end;
        int remain = refocus(shrink, true);
        if (remain < minComponents) {
            clear(); return null; }

        int iter = 0;
        MetalLongSet e = null;
        while (!isEmpty()) {

            if (iter++ > 0 && shrink)
                remain = refocus(shrink, false);

            if (remain < minComponents) {
                clear();
                //OOPS
                // TODO undo
                return null;
            }
            if (remain == 1) {
                //throw new WTF();
                return ((e == null) && needStamp) ? Stamp.toMutableSet(firstValid().task) : e;
            }

            //optimized special case
            //TODO generalize; prevent unnecessary MetalLongSet creation
            if (!needStamp && remain == 2) {
                if (!Stamp.overlaps(get(0).task, get(1).task))
                    break;
                else {
                    if (minComponents < 2) {
                        //disable the weaker of the two
                        removeFast(1);
                        break;
                    } else {
                        clear();return null;
                    }
                }
            }

            if (e == null)
                e = new MetalLongSet(NAL.STAMP_CAPACITY); //first iteration
            else
                e.clear(); //2nd iteration, or after


            MetalBitSet conflict = null;
            boolean invalids = false;
            int ss = size();
            for (int i = 0; i < ss; i++) {
                TaskComponent c = get(i);
                if (c == null) {
//                    if (NAL.DEBUG)
//                        throw new NullPointerException(); //HACK
//                    else
                    invalids = true;
                    continue;
                }
                if (!c.valid()) {
                    set(i, null);
                    invalids = true;
                    continue;
                }


                long[] iis = c.task.stamp();

                if (i > 0 && Stamp.overlapsAny(e, iis)) {
                    if (conflict == null)
                        conflict = MetalBitSet.bits(size());

                    conflict.set(i);
                } else {
                    e.addAll(iis);
                }
            }

            int conflicts = conflict!=null ? conflict.cardinality() : 0;

            //1. test if they are all independent.  then all may be combined.
            if (conflicts == 0)
                break; //all ok

            //something must be removed
            //sum the non-conflicting only if that subset is itself non-conflicting and thus revisable
            double valueOK  = eviSum(conflict, false);
            double valueConflicting = conflictedEvi(conflict);
            if (valueOK > valueConflicting) {
                if (remain - conflicts < minComponents) {
                    clear();
                    return null; //impossible: nothing else to remove
                } else {
                    nullAll(conflict);
                    if (!needStamp) {
                        removeNulls();
                        break; //done
                    }

                    e = null;
                    //done but recycle to get the stamp
                }
            } else {
                if (remain - 1 < minComponents) {
                    clear();
                    return null;  //impossible: nothing else to remove
                }

                setFast(0, null); //pop the top, and retry with remaining
                invalids = true;
                e = null;
            }

            if (invalids || conflicts > 0)
                removeNulls();

        }

//        if (this.time(bs, be)) {
//            int sss = refocus(tCrop, false);
//            assert(sss >= minComponents);
//        }

        return e;
    }

    private double conflictedEvi(MetalBitSet x) {
        int n = x.cardinality();
        if (n < 2)
            return eviSum(x, true);

        if (n == 2) {
            //optimized 2-ary case
            int a, b;
            if (size > 2) {
                a = x.first(true);
                b = x.next(true, a + 1, Integer.MAX_VALUE);
            } else { a = 0; b = 1; }

            //assert(a!=b);
            TaskComponent aa = get(a), bb = get(b);
            if (Stamp.overlapsAny(aa.task.stamp(), bb.task.stamp())) {
                x.clear(1);
                set(1, null);
                return aa.evi;
            } else {
                return aa.evi + bb.evi;
            }
        } else {

            double ee = 0;
            MetalLongSet inc = new MetalLongSet(NAL.STAMP_CAPACITY);
            MetalBitSet exc = null; //first iteration
            for (int i = 0; i < size && n > 0; i++) {
                if (x.get(i)) {
                    TaskComponent tti = get(i);
                    long[] iis = tti.task.stamp();

                    if (i > 0 && Stamp.overlapsAny(inc, iis)) {
                        //overlaps
                        //greedy:
                        if (exc==null)
                            exc = MetalBitSet.bits(NAL.STAMP_CAPACITY);
                        x.clear(i);
                        exc.set(i);
                    } else {
                        //include
                        inc.addAll(iis);
                        ee += tti.evi;
                        n--;
                    }

                }
            }

            if(exc!=null)
                nullAll(exc);

            return ee;
        }
        //return eviSum(x); //all
    }

    private void nullAll(MetalBitSet x) {
        int c = x.cardinality();
        if (c == 0) {

        } else if (c == 1) {
            int a = x.first(true);
            setFast(a, null);
        } else {
            int ss = size();
            for (int i = 0; i < ss; i++) {
                if (x.get(i))
                    setFast(i, null);
            }
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


    private double eviSum(MetalBitSet b, boolean what) {
        switch (b.cardinality()) {
            case 0: return 0;
            case 1: {
                int a = b.first(what);
                return a < size ? get(a).evi : 0;
            }
            default: return eviSum(what ? b::get : ((IntPredicate)b::get).negate());
        }
    }

    private double eviSum(IntPredicate each) {
        double e = 0;
        int n = size();
        for (int i = 0; i < n; i++) {
            if (each.test(i)) {
                TaskComponent c = get(i);
                if (c == null) continue; //HACK
                double ce = c.evi;
                if (ce == ce)
                    e += ce;
            }
        }
        return e;
    }

    private MetalLongSet only(boolean shrink, boolean provideStamp) {
        refocus(shrink, true);
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

    public final TruthProjection add(TaskList tasks) {
        ensureCapacity(tasks.size());
        return add((Iterable) tasks);
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


    float intermpolateAndCull(NAL nar) {

        final int root = firstValidIndex();
        int thisSize;
        main: while ((thisSize = size()) >= 1) {
            TaskComponent rootComponent = get(root);
            Term first = rootComponent.task.term();
            this.term = first;
            if (thisSize == 1 || !first.hasAny(Op.Temporal)) {
                //assumes that all the terms are from the same concept.  so if the first target has no temporal components the rest should not either.
                return 1;
            }


            MetalBitSet matchesFirst = MetalBitSet.bits(thisSize);
            matchesFirst.set(root);
            for (int i = firstValidIndex() + 1; i < thisSize; i++) {
                TaskComponent t = this.get(i);
                if (t.valid() && first.equals(t.task.term()))
                    matchesFirst.set(i);
            }
            int mc = matchesFirst.cardinality();
            if (mc > 1) {
                if (mc < thisSize) {
                    //HACK this is too greedy
                    //exact matches are present.  remove those which are not
                    for (int i = firstValidIndex() + 1; i < thisSize; i++)
                        if (!matchesFirst.get(i))
                            setFast(i, null);
                }
                removeNulls();
                return 1f;
            } else {


                for (int next = root + 1; next < size; next++) {
                    int tryB = firstValidIndex(next);
                    if (tryB != -1) {
                        TaskComponent B = get(tryB);
                        Term a = first;
                        Term b = B.task.term();
                        final double e2Evi = B.evi;

                        float dtDiff;
                        //HACK this chooses the first available 2+-ary match, there may be better
                        if ((Float.isFinite(dtDiff = dtDiff(a, b)))) {

                            final double e1Evi = rootComponent.evi;

                            Term ab;
                            try {
                                //if there isnt more evidence for the primarily sought target, then just use those components
                                ab = Intermpolate.intermpolate((Compound) a, (Compound) b, (float) (e1Evi / (e1Evi + e2Evi)), nar);
                            } catch (TermTransformException e) {
                                //HACK TODO avoid needing to throw exception
                                if (NAL.DEBUG) {
                                    throw new RuntimeException(e);
                                } else {
                                    //ignore.  it may be a contradictory combination of events.
                                    ab = Null;
                                }
                            }

                            if (Task.validTaskTerm(ab)) {

                                this.term = ab;
                                for (int i = 0; i < size; i++)
                                    if (i != root && i != tryB) {
                                        if (get(i) != null && !get(i).task.term().equals(ab))
                                            setFast(i, null);
                                    }
                                removeNulls();
                                //return 1 - dtDiff * 0.5f; //half discounted
                                //return 1 - dtDiff;
                                return 1; //no discount for difference
                            }


                        }
                    }
                }
                removeNulls(); //HACK
                if (size() == 2) {
                    //stronger
                    remove(1);
                    return 1;
                } else {
                    assert (size() > 2);


                    if (eviSum(i -> i > 0) >= get(0).evi) {
                        // if value of remainder > value(0):
                        remove(0);  //abdicate current root and continue remaining
                        continue main;
                    } else {
                        size = 1;
                        return 1;
                    }
                }

//            //last option: remove all except the first
//            removeNulls();
//
//            this.term = first;
//            return 1;


            }
        }
        return 1; //?
    }


    public byte punc() {
        if (isEmpty()) throw new RuntimeException();
        return get(0).task.punc();
    }


    @Nullable
    public final Truth truth() {
        return truth(NAL.truth.EVI_MIN, false, false, null);
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
    private int refocus(boolean shrink, boolean all) {
        if (isEmpty())
            return 0;
        if (shrink || start == ETERNAL) {
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
                    boolean stretch = false; //TODO param
                    if (stretch) {
                        if (start < u0 && u0 < this.end) {
                            start = u0;
                            changed = true;
                        }
                        if (this.end > u1 && u1 > start) {
                            this.end = u1;
                            changed = true;
                        }
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

    public Supplier<long[]> stamper(Supplier<Random> rng) {
        int ss = size();
        if (ss == 1) {
            return ()->get(0).task.stamp();
        } else {
            //TODO optimized 2-ary case?

            return () -> {
                @Nullable MetalLongSet s = Stamp.toMutableSet(
                    NAL.STAMP_CAPACITY,
                    i -> get(i).task.stamp(),
                    ss); //calculate stamp after filtering and after intermpolation filtering
                if (s.size() > NAL.STAMP_CAPACITY) {
                    return Stamp.sample(NAL.STAMP_CAPACITY, s, rng.get());
                } else {
                    return s.toSortedArray();
                }
            };
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

        final double eviDescending() {
            double e = this.evi;
            return (e == e) ? -e : Double.POSITIVE_INFINITY;
        }
    }

    /** Truth Coherency Metric
     *  inversely proportional to the statistical variance of the contained truth's frequency components
     *
     *  TODO refine, maybe weight by evi
     * */
    @Paper public double coherency() {
        int s = size();
        if (s == 0) return 0;
        if (s == 1) return 1;

        double avg = 0;
        for (int i = 0, thisSize = this.size(); i < thisSize; i++) {
            avg += this.get(i).task.freq();
        }
        avg /= s;

        double variance = 0.0;
        for (int i = 0; i < s; i++) {
            double p = get(i).task.freq();
            double d = p - avg;
            variance += d * d;
        }
        variance /= s;

        return 1 - variance;
    }

}
