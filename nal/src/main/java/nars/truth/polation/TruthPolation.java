package nars.truth.polation;

import jcog.Paper;
import jcog.Skill;
import jcog.WTF;
import jcog.data.list.FasterList;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.task.Revision;
import nars.task.Tasked;
import nars.task.util.TaskRegion;
import nars.term.Term;
import nars.truth.Stamp;
import nars.truth.Truth;
import org.eclipse.collections.api.set.primitive.LongSet;
import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;
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
        return /* t.intersects(start, end) &&*/  //<- hard filter
                super.add(new TaskComponent(t));
    }

    /**
     * remove components contributing no evidence
     */
    public final TruthPolation filter() {
        if (size() > 1)
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

            float eAvg = TruthIntegration.eviAvg(task, start, end, dur);

            if (eAvg < Param.TRUTH_MIN_EVI) {
                tc.evi = -1;
                return null;
            } else {
                tc.freq = task.freq(start, end);
                tc.evi = eAvg;
                return tc;
            }
        } else {
            return tc.evi >= Param.TRUTH_MIN_EVI ? tc : null;
        }


    }

    public final LongSet filterCyclic() {
        return filterCyclic(true);
    }

    /**
     * removes the weakest components sharing overlapping evidence with stronger ones.
     * should be called after all entries are added
     */
    @Nullable public final LongSet filterCyclic(boolean provideStampForOneTask) {
        filter();

        int s = size();

        if (s > 1) {
            sortThisByFloat(tc -> -tc.evi);


            if (s == 2) {

                if (Stamp.overlapsAny(get(0).task.stamp(), get(1).task.stamp())) {
                    remove(1);
                    s = size();
                }
            }
        }

        if (s == 1)
            return provideStampForOneTask ? Stamp.toSet(get(0).task) : null;


        LongHashSet e = new LongHashSet(s * 4);
        removeIf(tc -> {
            long[] stamp = tc.task.stamp();


            for (long ss : stamp) {
                if (!e.add(ss))
                    return true;
            }


            return false;
        });

        return e;
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
        if (thisSize == 1) {
            term = get(0).task.term();
            return 1;
        }

        Term first = null, second = null;

        for (int i = 0; i < thisSize; i++) {
            TaskComponent t = this.get(i);
            Term ttt = t.task.term();
            if (i == 0) {
                first = ttt;
                if (!ttt.hasAny(Op.Temporal))
                    break;
            } else {
                if (!first.equals(ttt)) {
                    if (second != null) {

                        removeAbove(i);
                        break;
                    } else {
                        second = ttt;
                    }
                }


            }
        }

        if (second == null) {
            term = first;
            return 1f;
        } else {

            float differenceFactor;
            Term a = first.term();
            Term b = second.term();


            float diff = dtDiff(a, b);
            if (!Float.isFinite(diff))
                return 0;
            if (diff > 0) {
                differenceFactor = Param.evi(1f,
                        Math.round(diff / 2f) /* /2 since it is shared between the two */,
                        Math.max(1, dur) /* cant be zero */);
            } else {


                differenceFactor = 1f;
            }

            Term theFirst = first;
            Term finalSecond = second;
            float e1, e2;
            if (size() > 2) {

                e1 = (float) sumOfFloat(x -> x.task.term().equals(theFirst) ? x.evi : 0);
                e2 = (float) sumOfFloat(x -> x.task.term().equals(finalSecond) ? x.evi : 0);
            } else {
                e1 = get(0).evi;
                e2 = get(1).evi;
            }
            float firstProp = e1 / (e1 + e2);
            Term term = Revision.intermpolate(first, second, firstProp, nar);


            if (Task.taskConceptTerm(term)) {
                if (count(t -> !t.task.term().equals(theFirst)) > 1) {
                    //remove any that are different and just combine what matches the first
                    removeIfTermDiffers(theFirst);
                    return 1;
                } else {
                    this.term = term;
                    return differenceFactor;
                }
            } else {
                removeIfTermDiffers(theFirst);
                return 1f;
            }
        }


    }

    private void removeIfTermDiffers(Term theFirst) {
        removeIf(t -> !t.task.term().equals(theFirst));
        this.term = theFirst;
    }

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
    public Truth truth() {
        return truth(null);
    }

    /** refined time involving the actual contained tasks.  the pre-specified interval may be larger but
     * after filtering tasks, it may have shrunk.
     */
    public long[] taskRange() {
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
            return new long[] { ETERNAL, ETERNAL }; //unchanged, must be due to eternal content only
        else if (start == ETERNAL)
            return new long[] { s, e };
        else {
            long[] t = new long[]{
                    Math.max(s, start), Math.min(e, end)
            };
            if (t[0] > t[1])
                throw new WTF();
            return t;
        }
    }

    public Task getTask(int i) {
        return get(i).task;
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
