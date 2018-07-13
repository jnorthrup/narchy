package nars.truth.polation;

import jcog.Paper;
import jcog.Skill;
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

    public TruthPolation add(Task t) {
        super.add(new TaskComponent(t));
        return this;
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

            float eTotal = TruthIntegration.eviAvg(task, start, end, dur);

            if (eTotal < Param.TRUTH_MIN_EVI) {
                tc.evi = -1;
                return null; 
            } else {
                tc.freq = task.freq(start, end);
                tc.evi = eTotal;
                return tc;
            }
        } else {
            return tc.evi >= Param.TRUTH_MIN_EVI ? tc : null;
        }


    }

    /**
     * removes the weakest components sharing overlapping evidence with stronger ones.
     * should be called after all entries are added
     */
    public final LongSet filterCyclic() {
        filter();

        int s = size();

        if (s > 1) {
            sortThisByFloat(tc -> -tc.evi); 
            

            if (s == 2) {
                
                if (Stamp.overlapsAny(get(0).task.stamp(), get(1).task.stamp())) {
                    remove(1);
                }
            }
        }

        if (s == 1)
            return Stamp.toSet(get(0).task);

        

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
                differenceFactor = (float) Param.evi(1f,
                        diff / 2f /* /2 since it is shared between the two */,
                        Math.max(1, dur) /* cant be zero */); 
            } else {
                
                
                differenceFactor = 1f;
            }

            Term finalFirst = first;
            Term finalSecond = second;
            float e1, e2;
            if(size() > 2) {
                
                e1 = (float) sumOfFloat(x -> x.task.term().equals(finalFirst) ? x.evi : 0);
                e2 = (float) sumOfFloat(x -> x.task.term().equals(finalSecond) ? x.evi : 0);
            } else {
                e1 = get(0).evi;
                e2 = get(1).evi;
            }
            float firstProp = e1 / (e1 + e2);
            Term term = Revision.intermpolate(first, second, firstProp, nar);

















            

            if (Task.validTaskTerm(term)) {
                this.term = term;
                return differenceFactor;
            } else {
                removeIf(t -> !t.task.term().equals(finalFirst));
                this.term = first;
                return 1f;
            }
        }












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
