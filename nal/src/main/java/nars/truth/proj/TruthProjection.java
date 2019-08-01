package nars.truth.proj;

import jcog.Paper;
import jcog.Skill;
import jcog.WTF;
import jcog.data.bit.MetalBitSet;
import jcog.data.set.MetalLongSet;
import jcog.pri.ScalarValue;
import jcog.util.ArrayUtil;
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;

import static java.lang.System.arraycopy;
import static jcog.Util.assertFinite;
import static nars.NAL.STAMP_CAPACITY;
import static nars.term.atom.Bool.Null;
import static nars.term.util.Intermpolate.dtDiff;

/**
 * Truth Interpolation and Extrapolation of Temporal Beliefs/Goals
 * see:
 * https:
 * https:
 */
@Paper
@Skill({"Interpolation", "Extrapolation"})
abstract public class TruthProjection extends TaskList {

    long start, end;
    float dur;

    /**
     * content target, either equal in all the tasks, or the result is
     * intermpolated (and evidence reduction applied as necessary)
     */
    public Term term = null;
    protected float eviFactor = 1;

    /** active evidence cache */
    @Nullable double[] evi = null;

    TruthProjection(long start, long end, float dur) {
        super(0);
        this.start = start;
        this.end = end;

        assert (dur >= 0);
        this.dur = dur;
    }

    public final Truth truth(double eviMin, boolean dither, boolean tShrink, NAL nar) {
        return truth(eviMin, dither, tShrink, true, nar);
    }

    /**
     * computes the final truth value
     */
    @Nullable
    public abstract Truth truth(double eviMin, boolean dither, boolean tShrink, boolean commitFirst, NAL nar);

    private boolean update(int i) {
        Task t = items[i];
        return t!=null && valid(evi[i] = evi(t));
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
        return e < NAL.truth.EVI_MIN ? 0 : e;
    }


    /**
     * removes the weakest components sharing overlapping evidence with stronger ones.
     * should be called after all entries are added
     * @param needStamp whether a stamp result should be returned, or if this can be elided if not necessary
     */
    @Nullable
    public final MetalLongSet commit(boolean shrink, int minResults, boolean needStamp, NAL n) {
        int s = size();
        if (s < minResults) {
            return null;
        } else if (s == 1) {
            return only(shrink, needStamp);
        } else {
            MetalLongSet e = filterCyclicN(minResults, shrink, needStamp);

            int activeBefore = activeUpdate(shrink);

            float c = intermpolateAndCull(n); assertFinite(c);
            eviFactor = c;

            if (eviFactor < ScalarValue.EPSILON)
                return null;

            int activeAfterIntermpolateCull = activeUpdate(shrink);
            if (activeAfterIntermpolateCull == 0)
                return null;
            else if (shrink && activeAfterIntermpolateCull != activeBefore) {
                e = filterCyclicN(minResults, shrink, needStamp);
            }

            activeUpdate(shrink); //just ensure updated

            return needStamp ? e : null;
        }
    }

    @Deprecated private int activeUpdate(boolean shrink) {
        return evi!=null ? active() : refocus(shrink, true);
    }

    int active() {
        if (size==0)
            return 0;
        double[] a = evi;
        if (a==null)
            throw new NullPointerException("evi cache is uncalculated");
        int y = 0;
        for (double aa : a) {
            if (valid(aa))
                y++;
        }
        return y;
    }

    public int update() {
        int s = size();
        if (s <= 0)
            return 0;

        int count = 0;
        if (evi == null || evi.length < s)
            evi = new double[s];
        for (int i = 0; i < s; i++) {
            if (update(i))
                count++;
        }
        if (count == 0) {
            Arrays.fill(evi, 0);
            return 0;
        }
        if (size() > 1) {
            //descending
            ArrayUtil.quickSort(0, s, (a,b)->
                 Double.compare(evi[b], evi[a])
            , (a,b)->{
                ArrayUtil.swap(items, a, b);
                ArrayUtil.swap(evi, a, b);
            });
        }
        return count;
    }

 @Nullable private MetalLongSet filterCyclicN(int minComponents, boolean shrink, boolean needStamp) {

        assert (minComponents >= 1);

        @Deprecated int remain = refocus(shrink, true);
        if (remain < minComponents) {
            clear(); return null;
        }

        int iter = 0;
        MetalLongSet e = null;
        while (!isEmpty()) {

            if (evi == null || (iter++ > 0 && shrink))
                remain = refocus(shrink, false);

            if (remain < minComponents) {
                //OOPS
                // TODO undo
                clear();
                return null;
            }
            if (remain == 1) {
                //throw new WTF();
                e = needStamp ? Stamp.toMutableSet(firstValid()) : null;
                break;
            }

            //optimized special case
//            //TODO generalize; prevent unnecessary MetalLongSet creation
//            if (!needStamp && remain == 2) {
//                if (!Stamp.overlap(get(0).task, get(1).task))
//                    break;
//                else {
//                    if (minComponents < 2) {
//                        //disable the weaker of the two
//                        removeFast(1);
//                        break;
//                    } else {
//                        clear();return null;
//                    }
//                }
//            }
            int ss = size();

            Task[] items = this.items;
            if (NAL.REVISION_ALLOW_OVERLAP_IF_DISJOINT_TIME) {
                //HACK temporary strategy
                boolean disjointOrNonOverlapping = true;
                int count = 0;
                overlapDisjoint: for (int i = 0; i < ss; i++) {
                    if (!valid(i))
                        continue;
                    Task ii = items[i];
                    for (int j = i; j < ss; j++) {
                        if (!valid(j))
                            continue;
                        Task jj = items[j];
                        if (Stamp.overlap(ii, jj)) {
                            disjointOrNonOverlapping = false;
                            break overlapDisjoint;
                        }
                    }
                    count++;
                }
                if (disjointOrNonOverlapping && count >= minComponents) {
                    if (needStamp) {
                        MetalLongSet evi = new MetalLongSet(ss*STAMP_CAPACITY);
                        for (int i = 0; i < ss; i++) {
                            if (valid(i))
                                evi.addAll(stamp(i));
                        }
                        e = evi;
                        break;
                    } else {
                        //done
                        break;
                    }
                }
            }

            if (e == null)
                e = new MetalLongSet(STAMP_CAPACITY * remain);
            else
                e.clear(); //2nd iteration, or after


            MetalBitSet conflict = null;

            for (int i = 0; i < ss; i++) {
                if (!valid(i)) {
                    continue;
                }
                Task c = items[i];
//                if (c == null)
//                    continue;

                long[] iis = c.stamp();

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
                        break; //done
                    }

                    //done but recycle to get the stamp
                }
            } else {
                if (remain - 1 < minComponents) {
                    clear();
                    return null;  //impossible: nothing else to remove
                }

                remove(0);  //pop the top, and retry with remaining
            }
            //e = null;
            e.clear();


        }

        removeNulls();
        return e;
    }

    @Override
    public void clear() {
        super.clear();
        evi = null;
    }

    @Override
    public boolean removeNulls() {
        int sizeBefore = size;
        if (evi!=null) {
            //verify that all zero evidence slots also have null tasks, if not then nullify the corresponding task slot
            for (int i= 0; i < sizeBefore; i++) {
                if (!valid(evi[i])) {
                    //assert(items[i] == null);
                    items[i] = null;
                    evi[i] = 0;
                }
            }
        }

        boolean result = false;
        if (super.removeNulls()) {
            if (evi!=null) {
                for (int i = 0; i < sizeBefore; ) {
                    if (evi[i] == 0 && i < sizeBefore-1) {
                        arraycopy(evi, i+1, evi, i, sizeBefore-1-i);
                        evi[sizeBefore-1] = 0;
                        sizeBefore--;
                    } else
                        i++;
                }
            }
            result = true;
        }
        trimToSize();
        return result;
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
            Task aa = get(a), bb = get(b);
            if (Stamp.overlap(aa, bb)) {
                x.clear(1);
                set(1, null);
                return evi[a];
            } else {
                return evi[a] + evi[b];
            }
        } else {

            double ee = 0;
            MetalLongSet inc = new MetalLongSet(n * STAMP_CAPACITY);
            MetalBitSet exc = null; //first iteration
            for (int i = 0; i < size && n > 0; i++) {
                if (x.get(i)) {
                    long[] iis = stamp(i);

                    if (i > 0 && Stamp.overlapsAny(inc, iis)) {
                        //overlaps
                        //greedy:
                        if (exc==null)
                            exc = MetalBitSet.bits(STAMP_CAPACITY);
                        x.clear(i);
                        exc.set(i);
                    } else {
                        //include
                        inc.addAll(iis);
                        ee += evi[i];
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
        return eviSum(what ? b::get : ((IntPredicate)b::get).negate());
    }

    private double eviSum(IntPredicate each) {
        double e = 0;
        int n = size();
        for (int i = 0; i < n; i++) {
            if (each.test(i)) {
                double ce = evi[i];
                if (ce == ce)
                    e += ce;
            }
        }
        return e;
    }

    private MetalLongSet only(boolean shrink, boolean provideStamp) {
        return refocus(shrink, true)>0 && provideStamp ? Stamp.toMutableSet(firstValid()) : null;
    }

    private Task firstValid() {
        int i = firstValidIndex();
        return i!=-1 ? get(i) : null;
    }

    private int firstValidIndex() {
        return indexOf((IntPredicate) this::valid);
    }
    private int firstValidIndex(int after) {
        return indexOf(after, (IntPredicate) this::valid);
    }

    public final boolean valid(int i) {
        return items[i]!=null && valid(evi[i]);
    }

    /** test for whether an amount of evidence is valid */
    public final boolean valid(double e) {
         return e > Double.MIN_NORMAL;
    }

    public final TruthProjection add(Tasked... tasks) {
        return add(tasks.length, tasks);
    }

    public final <T extends Tasked> TruthProjection add(int firstN, T... tasks) {
        ensureCapacity(firstN);
        for (int i = 0; i < firstN; i++)
            add(tasks[i]);
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

    public final TruthProjection addAll(TaskList tasks) {
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

        int thisSize;
        main: while ((thisSize = size()) >= 1) {
            final int root = firstValidIndex();
            Task rootComponent = get(root);
            Term first = rootComponent.term();
            this.term = first;
            if (thisSize == 1 || !first.hasAny(Op.Temporal)) {
                //assumes that all the terms are from the same concept.  so if the first target has no temporal components the rest should not either.
                return 1;
            }


            MetalBitSet matchesFirst = MetalBitSet.bits(thisSize);
            matchesFirst.set(root);
            for (int i = firstValidIndex() + 1; i < thisSize; i++) {
                Task t = this.get(i);
                if (valid(i) && first.equals(t.term()))
                    matchesFirst.set(i);
            }
            int mc = matchesFirst.cardinality();
            if (mc > 1) {
                if (mc < thisSize) {
                    //HACK this is too greedy
                    //exact matches are present.  remove those which are not
                    for (int i = firstValidIndex() + 1; i < thisSize; i++)
                        if (!matchesFirst.get(i))
                            remove(i);
                }
                return 1f;
            } else {


                for (int next = root + 1; next < size; next++) {
                    int tryB = firstValidIndex(next);
                    if (tryB != -1) {
                        Task B = get(tryB);
                        Term a = first;
                        Term b = B.term();
                        final double e2Evi = evi[tryB];

                        float dtDiff;
                        //HACK this chooses the first available 2+-ary match, there may be better
                        if ((Float.isFinite(dtDiff = dtDiff(a, b)))) {

                            final double e1Evi = evi[root];

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
                                        if (get(i) != null && !get(i).term().equals(ab))
                                            remove(i);
                                    }

                                //return 1 - dtDiff * 0.5f; //half discounted
                                //return 1 - dtDiff;
                                return 1; //no discount for difference
                            }


                        }
                    }
                }
                //removeNulls(); //HACK
                if (size() == 2) {
                    //stronger
                    remove(1);
                    return 1;
                } else {
                    assert (size() > 2);


                    if (eviSum(i -> i > 0) >= evi[0]) {
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

    @Override
    public Task remove(int index) {
        items[index] = null;
        evi[index] = 0;
        return null; //HACK
    }

    public byte punc() {
        if (isEmpty()) throw new RuntimeException();
        return get(0).punc();
    }

    @Nullable
    public final Truth truth() {
        return truth(NAL.truth.EVI_MIN, false, false, null);
    }


    public void print() {
        forEach(t -> System.out.println(t.proof()));
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
        if (shrink || start == ETERNAL || evi==null) {
            final long u0, u1;
            boolean changed = (evi==null);
            if (changed || ((all ? size() : active()) > 1)) {
                long[] union = Tense.union(changed||all ?
                    this :
                    ()->IntStream.range(0, size())
                        .filter(this::valid)
                        .mapToObj(this::get)
                        .filter(Objects::nonNull) //HACK
                        .map(x -> (TaskRegion)x).iterator());

                u0 = union[0]; u1 = union[1];
            } else {
                Task only = all ? getFirst() : firstValid();
                u0 = only.start(); u1 = only.end();
            }

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
                return update();
        }

        return active();
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
            avg += this.items[i].freq();
        }
        avg /= s;

        double variance = 0.0;
        for (int i = 0; i < s; i++) {
            double p = items[i].freq();
            double d = p - avg;
            variance += d * d;
        }
        variance /= s;

        return 1 - variance;
    }

    @Deprecated public boolean valid() {
        return (active()==size());
    }

//    /**
//     * TODO extend TaskList as TruthTaskList storing evi,freq pairs of floats in a compact float[]
//     */
//    @Deprecated
//    public static class TaskComponent implements Tasked {
//        public final Task task;
//
//        /**
//         * NaN if not yet computed
//         */
//        double evi = Double.NaN;
//
//        TaskComponent(Task task) {
//            this.task = task;
//        }
//
//        @Override
//        public String toString() {
//            return task + "=" + evi;
//        }
//
//        public boolean valid() {
//            return evi == evi;
//        }
//
//        @Override
//        public @Nullable Task task() {
//            return task;
//        }
//
////        void invalidate() {
////            evi = Double.NaN;
////        }
//
//
//        public final long[] stamp() {
//            assert(valid());
////            if (!valid())
////                throw new WTF();
//            return task.stamp();
//        }
//    }

}
