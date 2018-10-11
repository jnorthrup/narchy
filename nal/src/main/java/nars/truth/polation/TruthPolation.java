package nars.truth.polation;

import jcog.Paper;
import jcog.Skill;
import jcog.WTF;
import jcog.data.list.FasterList;
import jcog.data.set.MetalLongSet;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.task.Revision;
import nars.task.Tasked;
import nars.task.util.TaskRegion;
import nars.task.util.TimeRange;
import nars.term.Term;
import nars.truth.Stamp;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

import static java.lang.Float.NaN;
import static nars.task.Revision.dtDiff;
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

    final long start;
    final long end;
    int dur;

    /**
     * content term, either equal in all the tasks, or the result is
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
    @Nullable
    public abstract Truth truth(NAR nar);

    public boolean add(Task t) {
        if (t!=null) {
            return /* t.intersects(start, end) &&*/  //<- hard filter
                    super.add(new TaskComponent(t));
        }
        return false;
    }

    /**
     * remove components contributing no evidence
     */
    public final TruthPolation filter() {
        removeIf(x -> update(x) == null);
        return this;
    }

    @Nullable
    final TaskComponent update(int i) {
        return update(get(i));
    }

    @Nullable
    private TaskComponent update(TaskComponent tc) {
        if (!tc.isComputed()) {

            Task task = tc.task;

            float eContrib = TruthIntegration.evi(task, start, end, dur);

            if (eContrib < Param.TRUTH_MIN_EVI) {
                tc.evi = -1;
                return null;
            } else {
                tc.freq = task.freq(start, end);
                tc.evi = eContrib;
                return tc;
            }
        } else {
            return tc.evi >= Param.TRUTH_MIN_EVI ? tc : null;
        }


    }

    public final MetalLongSet filterCyclic() {
        return filterCyclic(true);
    }

    public final TruthPolation filtered() {
        return filtered(null);
    }

    public final TruthPolation filtered(@Nullable Task against) {
        filterCyclic(against, false);
        return this;
    }

    @Nullable public final MetalLongSet filterCyclic(boolean provideStampIfOneTask) {
        return filterCyclic(null, provideStampIfOneTask);
    }
    @Nullable public final MetalLongSet filterCyclic(boolean provideStampIfOneTask, int minResults) {
        return filterCyclic(null, provideStampIfOneTask, minResults);
    }

    @Nullable public final MetalLongSet filterCyclic(@Nullable Task selected, boolean provideStamp) {
        return filterCyclic(selected, provideStamp, 1);
    }

    /**
     * removes the weakest components sharing overlapping evidence with stronger ones.
     * should be called after all entries are added
     */
    @Nullable public final MetalLongSet filterCyclic(@Nullable Task selected, boolean provideStamp, int minResults) {

        int s = size();
        if (s == 0) {
            return null;
        } else if (s == 1) {
            return only(provideStamp);
        }

        filter();
        if ((s = size()) < minResults)
            return null;

        sortThisByFloat(tc -> -tc.evi); //TODO also sort by occurrence and/or stamp to ensure oldest task is always preferred

        if (selected == null)
            selected = get(0).task; //strongest


        if (s == 1)
            return only(provideStamp);
        else if (s == 2) {
            Task a = get(0).task;
            Task b = get(1).task;
            long[] as = a.stamp();
            long[] bs = b.stamp();
            if (Stamp.overlapsAny(as, bs)) {
                if (a == selected) remove(1); else remove(0);
                return (provideStamp ? Stamp.toSet(selected) : null);
            } else {
                return provideStamp ? Stamp.toSet(as.length + bs.length, a, b) : null;
            }
        } else {

            MetalLongSet e = Stamp.toSet(s * Param.STAMP_CAPACITY/2, selected);

            Task theSelected = selected;
            removeIf(tc -> {
                Task tt = tc.task;
                if (tt == theSelected)
                    return false; //skip and keep

                long[] stamp = tt.stamp();
                for (int i = 0, stampLength = stamp.length; i < stampLength; i++) {
                    long ss = stamp[i];
                    if (!e.add(ss)) {
                        //remove any contributed unique stamp components added for this task that overlaps
                        if (i > 0) {
                            for (int j = 0; j < i; j++) {
                                boolean removed = e.remove(stamp[j]);
                                assert(removed);
                            }
                        }
                        return true;
                    }
                }

                return false;
            });

            return provideStamp ? e : null;
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

        Term firstTerm = get(0).task.term();
        if (thisSize == 1 || !firstTerm.hasAny(Op.Temporal)) {
            //assumes that all the terms are from the same concept.  so if the first term has no temporal components the rest should not either.
            this.term = firstTerm;
            return 1;
        }

        Task first = null, second = null;

        for (int i = 0; i < thisSize; i++) {
            TaskComponent t = this.get(i);
            Term ttt = t.task.term();
            if (i == 0) {
                first = t.task;
                if (!ttt.hasAny(Op.Temporal))
                    break;
            } else {
                if (!first.equals(ttt)) {
                    if (second != null) {

                        removeAbove(i);
                        break;
                    } else {
                        second = t.task;
                    }
                }


            }
        }

        if (second == null) {
            term = first.term();
            return 1f;
        } else {


            Term a = first.term();
            Term b = second.term();
            long firstStart = first.start();
            long secondStart = second.start();
            final float[] e1Evi = {0};
            final float[] e2Evi = { 0 };
            Task finalFirst = first;
            Task finalSecond = second;
            removeIf(x -> {
               if (x.task == finalFirst || x.task.term().equals(a)) {
                   e1Evi[0] += x.evi;
                   return false;
               } else if (x.task == finalSecond || x.task.term().equals(b)) {
                   e2Evi[0] += x.evi;
                   return false;
               } else {
                   return true;
               }
            });

            Term term = Revision.intermpolate(a,
                    firstStart!=ETERNAL && secondStart!=ETERNAL ? secondStart - firstStart : 0,
                    b, e1Evi[0] /(e1Evi[0] + e2Evi[0]), nar);

            if (Task.taskConceptTerm(term)) {
                float diff = dtDiff(a, b);
                if (!Float.isFinite(diff))
                    return 0;

                //assert(diff >= 0 && diff <= 1.0);
                //float differenceFactor = 1 - diff;

                float differenceFactor = 1f / (1f + diff);

                this.term = term;
                return differenceFactor;
            } else {
                removeIf(x -> !x.task.term().equals(a));
                assert(size() > 0);
                this.term = a;
                return 1;
            }




//            Term theFirst = first;
//            Term finalSecond = second;
//            float e1, e2;
//            if (size() > 2) {
//
//                e1 = (float) sumOfFloat(x -> x.task.term().equals(theFirst) ? x.evi : 0);
//                e2 = (float) sumOfFloat(x -> x.task.term().equals(finalSecond) ? x.evi : 0);
//            } else {
//                e1 = get(0).evi;
//                e2 = get(1).evi;
//            }
//            float firstProp = e1 / (e1 + e2);


//            if (Task.taskConceptTerm(term)) {
//                if (count(t -> !t.task.term().equals(theFirst)) > 1) {
//                    //remove any that are different and just combine what matches the first
//                    removeIfTermDiffers(theFirst);
//                    return 1;
//                } else {
//                    this.term = term;
//                    return differenceFactor;
//                }
//            } else {
//                removeIfTermDiffers(theFirst);
//                return 1f;
//            }
        }


    }

//    private void removeIfTermDiffers(Term theFirst) {
//        removeIf(t -> !t.task.term().equals(theFirst));
//        this.term = theFirst;
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
        return truth(null);
    }

    /** refined time involving the actual contained tasks.  the pre-specified interval may be larger but
     * after filtering tasks, it may have shrunk.
     */
    public TimeRange taskRange() {
        long s = Long.MAX_VALUE, e = Long.MIN_VALUE;

        for (TaskComponent x : this) {
            long a = x.task.start();
            if (a!=ETERNAL) {
                a = Math.min(end, a);
                if (a < s)
                    s = a;
                long b = x.task.end();
                b = Math.max(start, b);
                if (b > e)
                    e = b;
            }
        }

        if (s == Long.MAX_VALUE)
            return TimeRange.ETERNAL; //unchanged, must be due to eternal content only
        else if (start == ETERNAL)
            return new TimeRange(new long[] { s, e });
        else {
            long[] t = new long[]{
                    Math.max(s, start), Math.min(e, end)
            };
            if (t[0] > t[1])
                throw new WTF();
            return new TimeRange(t);
        }
    }

    public Task getTask(int i) {
        return get(i).task;
    }

    public void print() {
        forEach(t -> System.out.println(t.task.proof()));
    }


    protected static class TaskComponent {
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
    }


}
