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
abstract public class TruthPolation extends FasterList<TruthPolation.TaskComponent> {

    public long start, end;
    int dur;

    /**
     * content target, either equal in all the tasks, or the result is
     * intermpolated (and evidence reduction applied as necessary)
     */
    public Term term = null;

    TruthPolation(long start, long end, int dur) {
        super(0);
        this.start = start;
        this.end = end;

        assert (dur >= 0);
        this.dur = dur;
    }

    /**
     * computes the final truth value
     */
    @Nullable public abstract Truth truth(float eviMin, NAR nar);

    @Nullable public Truth truth(float eviMin, boolean dither, NAR nar) {
        Truth t  = truth(eviMin, nar);
        if (t == null)
            return null;
        if (dither) {
            t = t.dither(nar);
            if (t == null || t.evi() < eviMin)
                return null;
        }
        return t;
    }

    public final boolean add(TaskRegion t) {
        return add(t.task());
    }

    public boolean add(Task t) {
        return add(new TaskComponent(t));
    }




    @Nullable
    final TaskComponent update(int i, float eviMin) {
        return update(get(i), eviMin);
    }

    @Nullable
    private TaskComponent update(TaskComponent tc, float eviMin) {
        if (!tc.isComputed()) {

            Task task = tc.task;

            if ((tc.evi = evi(task)) >= eviMin) {
                tc.freq = task.freq(start, end);
            } else
                return null;
        }

        return tc.evi >= eviMin ? tc : null;
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

    public final TruthPolation filtered() {
        filterCyclic(false);
        return this;
    }

    @Nullable
    public final MetalLongSet filterCyclic(boolean provideStamp) {
        return filterCyclic(provideStamp, 1);
    }

    /**
     * removes the weakest components sharing overlapping evidence with stronger ones.
     * should be called after all entries are added
     */
    @Nullable
    public final MetalLongSet filterCyclic(boolean provideStamp, int minResults) {

        int s;
        if ((s = size()) < minResults) return null;
        else if (s == 1) return only(provideStamp);

        validate(null);
        if ((s = size()) < minResults) return null;

        if ((s = size()) < minResults) return null;
        else if (s == 1) return only(provideStamp);

        sortThisByFloat(tc -> -tc.evi); //TODO also sort by occurrence and/or stamp to ensure oldest task is always preferred


        if (s == 1) {
            return only(provideStamp);
        } else {

            MetalLongSet e = new MetalLongSet(Param.STAMP_CAPACITY);

            int ss = size();

            //TODO permute alternate insertion order here
            for (int i = 0; i < ss; ) {
                Task ii = get(i).task;
                long[] iis = ii.stamp();

                if (i == 0 || !Stamp.overlapsAny(e, iis)) {
                    if (e != null)
                        e.addAll(iis);
                    i++;
                } else {
                    removeFast(i);
                    ss--;
                }
            }
            return provideStamp ? e : null;

            //                for (int j = 0; j < i; j++) {
//                    Task jj = get(j).task;
//                    if (Stamp.overlaps(iis, jj.stamp())) {
//                        keep = false;
//                        break;
//                    }
//                }

//            removeIf(tc -> {
//                Task tt = tc.task;
//                if (tt == theSelected)
//                    return false; //skip and keep
//
//                return false;
////
////                long[] stamp = tt.stamp();
////                boolean mustTest = false;
////                for (int i = 0, stampLength = stamp.length; i < stampLength; i++) {
////                    long ss = stamp[i];
////                    if (!e.addAt(ss)) {
////                        //collision: test previous results pair-wise
////                        mustTest = true;
////                    }
////                    //continue adding all
////                }
////
//////                        //remove any contributed unique stamp components added for this task that overlaps
//////                        if (i > 0) {
//////                            for (int j = 0; j < i; j++) {
//////                                boolean removed = e.remove(stamp[j]);
//////                                assert (removed);
//////                            }
//////                        }
////                        return true;
////                    }
////                }
////
////                return false;
//            });


        }
    }

    @Nullable
    private MetalLongSet only(boolean provideStamp) {
        return provideStamp ? Stamp.toSet(get(0).task) : null;
    }


    public final TruthPolation add(Tasked... tasks) {
        ensureCapacity(tasks.length);
        for (Tasked t : tasks) {
            if (t != null)
                add(t);
        }
        return this;
    }

    private TruthPolation add(Iterable<? extends Tasked> tasks) {
        tasks.forEach(this::add);
        return this;
    }

    public final TruthPolation add(Collection<? extends Tasked> tasks) {
        ensureCapacity(tasks.size());
        return add((Iterable) tasks);
    }

    public final TruthPolation add(Tasked tt) {
        add(tt.task());
        return this;
    }

    float intermpolate(NAR nar) {
        int thisSize = this.size();
        if (thisSize == 0) return 0;

        Term first = get(0).task.term();
        if (thisSize == 1 || !first.hasAny(Op.Temporal)) {
            //assumes that all the terms are from the same concept.  so if the first target has no temporal components the rest should not either.
            this.term = first;
            return 1;
        }



        MetalBitSet matchingFirst = MetalBitSet.bits(thisSize);
        matchingFirst.set(0);
        for (int i = 1; i < thisSize; i++) {
            TaskComponent t = this.get(i);
            Term ttt = t.task.term();
            if (first.equals(ttt))
                matchingFirst.set(i);
//            if (!first.equals(ttt)) {
//                if (second != null) {
//
//                    removeAbove(i);
//                    break;
//                } else {
//                    second = t.task;
//                }
//            }
        }
        if (matchingFirst.cardinality() > 1) {
            this.term = first;
            //exact matches are present.  remove those which are not
            for (int i = 1; i < thisSize; i++)
                if (!matchingFirst.get(i))
                    set(i, null);
            removeNulls();
            return 1f;
        } else {

            //use the first successful intermpolation, if possible

            int tryB = 1;
            final float e1Evi = get(0).evi;
            Term a = first;
            TaskComponent B = get(tryB);
            Term b = B.task.term();

            if ((Float.isFinite(dtDiff(a, b)))) {

//                long firstStart = first.start();
//                long secondStart = second.start();
                final float e2Evi = B.evi;
//                Task finalFirst = A.task;
//                Task finalSecond = B.task;
//                removeIf(x -> {
//                    Task xx = x.task;
//                    Term xxx;
//                    if (xx == finalFirst || (xxx = xx.target()).equals(a)) {
//                        e1Evi[0] += x.evi;
//                        return false;
//                    } else if (xx == finalSecond || xxx.equals(b)) {
//                        e2Evi[0] += x.evi;
//                        return false;
//                    } else {
//                        return true;
//                    }
//                });

                //if there isnt more evidence for the primarily sought target, then just use those components
                Term ab = Intermpolate.intermpolate(a,
                        b, e1Evi / (e1Evi + e2Evi), nar);

                if (Task.validTaskTerm(ab)) {

                    this.term = ab;
                    removeAbove(1+1);  assert(size()==2);
                    //return 1 - dtDiff * 0.5f; //half discounted
                    //return 1 - dtDiff;
                    return 1; //no discount for difference
                }


            }


            //last option: remove all except the first
            removeAbove(1); assert(size()==1);
            this.term = a;
            return 1;


//            Term theFirst = first;
//            Term finalSecond = second;
//            float e1, e2;
//            if (size() > 2) {
//
//                e1 = (float) sumOfFloat(x -> x.task.target().equals(theFirst) ? x.evi : 0);
//                e2 = (float) sumOfFloat(x -> x.task.target().equals(finalSecond) ? x.evi : 0);
//            } else {
//                e1 = get(0).evi;
//                e2 = get(1).evi;
//            }
//            float firstProp = e1 / (e1 + e2);


//            if (Task.taskConceptTerm(target)) {
//                if (count(t -> !t.task.target().equals(theFirst)) > 1) {
//                    //remove any that are different and just combine what matches the first
//                    removeIfTermDiffers(theFirst);
//                    return 1;
//                } else {
//                    this.target = target;
//                    return differenceFactor;
//                }
//            } else {
//                removeIfTermDiffers(theFirst);
//                return 1f;
//            }
        }


    }

//    private void removeIfTermDiffers(Term theFirst) {
//        removeIf(t -> !t.task.target().equals(theFirst));
//        this.target = theFirst;
//    }

    public byte punc() {
        if (isEmpty()) throw new RuntimeException();
        return get(0).task.punc();
    }

    public TaskRegion[] tasks() {
        int size = this.size();
        TaskRegion[] t = new TaskRegion[size];
        for (int i = 0; i < size; i++) {
            t[i] = get(i).task;
        }
        return t;
    }

    @Nullable
    public final Truth truth() {
        return truth(Float.MIN_NORMAL, null);
    }

//    /** refined time involving the actual contained tasks.  the pre-specified interval may be larger but
//     * after filtering tasks, it may have shrunk.
//     */
//    public TimeRange taskRange() {
//        long s = Long.MAX_VALUE, e = Long.MIN_VALUE;
//
//        for (TaskComponent x : this) {
//            long a = x.task.start();
//            if (a!=ETERNAL) {
//                a = Math.min(end, a);
//                if (a < s)
//                    s = a;
//                long b = x.task.end();
//                b = Math.max(start, b);
//                if (b > e)
//                    e = b;
//            }
//        }
//
//        if (s == Long.MAX_VALUE)
//            return TimeRange.ETERNAL; //unchanged, must be due to eternal content only
//        else if (start == ETERNAL)
//            return new TimeRange(new long[] { s, e });
//        else {
//            long[] t = new long[]{
//                    Math.max(s, start), Math.min(e, end)
//            };
//            if (t[0] > t[1])
//                throw new WTF();
//            return new TimeRange(t);
//        }
//    }

//    public Task getTask(int i) {
//        return get(i).task;
//    }

    public void print() {
        forEach(t -> System.out.println(t.task.proof()));
    }

    public long start() {
        return start;
    }

    public long end() {
        return end;
    }

    /**
     * use after filtering cyclic.
     * adjust start/end to better fit the (remaining) task components and minimize temporalizing truth dilution.
     * if the start/end has changed, then evidence for each will need recalculated
     *  */
    public void refocus(NAR nar) {
        if (size()<2)
            return;

        long[] se = Tense.union(Iterables.transform(this, (TaskComponent x)->x.task));
        if (se[0] == ETERNAL)
            return; //eternal

        if (start==ETERNAL) {
            //override eternal range with the calculated union
        } else {
            se[0] = Math.min(end, Math.max(start, se[0]));
            se[1] = Math.max(start, Math.min(end, se[1]));
        }

        int dtDither = nar.dtDither();
        if (dtDither > 1) {
            Tense.dither(se, dtDither);
        }
        if (se[0]!=start || se[1]!=end) {
            invalidateEvi();
            start = se[0];
            end = se[1];
        }
    }

    protected void validate(@Nullable NAR nar) {
        if (nar!=null && start == ETERNAL)
            refocus(nar);

        removeIf(x -> update(x, Float.MIN_NORMAL) == null);
    }


    private void invalidateEvi() {
        forEach(TaskComponent::invalidate);
    }

    public final Task task(TaskList d, Truth tt, boolean beliefOrGoal, NAR nar) {
        return d.task(term, tt, this::stamper, beliefOrGoal, start(), end(), nar);
    }

    private long[] stamper(Random rng) {
        @Nullable MetalLongSet stampSet = Stamp.toSet(Param.STAMP_CAPACITY, size(), this); //calculate stamp after filtering and after intermpolation filtering
        if (stampSet.size() > Param.STAMP_CAPACITY) {
            return Stamp.sample(Param.STAMP_CAPACITY, stampSet, rng);
        } else {
            return stampSet.toSortedArray();
        }
    }


    /** TODO extend TaskList as TruthTaskList storing evi,freq pairs of floats in a compact float[] */
    @Deprecated protected static class TaskComponent implements Tasked {
        final Task task;

        /**
         * NaN if not yet computed
         */
        float evi = NaN;
        float freq = NaN;

        TaskComponent(Task task) {
            this.task = task;
        }

        @Override
        public String toString() {
            return evi + "," + freq + '=' + task;
        }

        boolean isComputed() {
            float f = freq;
            return f == f;
        }

        @Override
        public @Nullable Task task() {
            return task;
        }

        public void invalidate() {
            evi = freq = Float.NaN;
        }
    }


}
