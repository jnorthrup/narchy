package nars.table;

import jcog.pri.Prioritized;
import nars.NAR;
import nars.Task;
import nars.control.proto.Remember;
import nars.task.TaskProxy;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * A model storing, ranking, and projecting beliefs or goals (tasks with TruthValue).
 * It should iterate in top-down order (highest ranking first)
 */
public interface BeliefTable extends TaskTable {

    static float eternalTaskValue(Task eternal) {
        return eternal.evi();
    }
    
    static float eternalTaskValueWithOriginality(Task eternal) {
        return eternalTaskValue(eternal) * eternal.originality();
    }

    void setCapacity(int eternals, int temporals);

    /**
     * minT and maxT inclusive
     * TODO add Predicate<> form of this for early exit
     */
    void forEachTask(boolean includeEternal, long minT, long maxT, Consumer<? super Task> x);

    /**
     * attempt to insert a task; returns what was input or null if nothing changed (rejected)
     */
    @Override
    void add(/*@NotNull*/ Remember input,  /*@NotNull*/ NAR nar);

    @Override
    default Task match(long start, long end, Term template, NAR nar) {
        return match(start, end, template, null, nar);
    }

    Task match(long start, long end, @Nullable Term template, Predicate<Task> filter, NAR nar);

    /**
     * estimates the current truth value from the top task, projected to the specified 'when' time;
     * returns null if no evidence is available
     *
     * if the term is temporal, template specifies the specifically temporalized term to seek
     * if template is null then the concept() form of the task's term is
     */
    Truth truth(long start, long end, @Nullable Term template, NAR nar);

    default Truth truth(long when, NAR nar) {
        return truth(when, when, null, nar);
    }

    default Truth truth(long start, long end, NAR nar) {
        return truth(start, end, null, nar);
    }

    default void print(/*@NotNull*/ PrintStream out) {
        this.forEachTask(t -> out.println(t + " " + Arrays.toString(t.stamp()))); 
    }

    default void print() {
        print(System.out);
    }

    default float priSum() {
        return (float) streamTasks().mapToDouble(Prioritized::pri).sum();
    }

    BeliefTable Empty = new BeliefTable() {

        @Override
        public Stream<Task> streamTasks() {
            return Stream.empty();
        }

        @Override
        public Task match(long start, long end, Term template, NAR nar) {
            return null;
        }

        @Override
        public Task sample(long start, long end, Term template, NAR nar) {
            return null;
        }

        @Override
        public void forEachTask(Consumer<? super Task> x) {

        }

        @Override
        public boolean removeTask(Task x) {
            return false;
        }

        @Override
        public int capacity() {
            return 0;
        }

        @Override
        public void setCapacity(int eternals, int temporals) {
        }

        @Override
        public void forEachTask(boolean includeEternal, long minT, long maxT, Consumer<? super Task> x) {

        }

        @Override
        public float priSum() {
            return 0;
        }


        @Override
        public int size() {
            return 0;
        }


        @Override
        public void add(/*@NotNull*/ Remember input,  /*@NotNull*/ NAR nar) {

        }

        @Override
        public Task match(long start, long end, Term template, Predicate<Task> filter, NAR nar) {
            return null;
        }

        @Override
        public void print(/*@NotNull*/ PrintStream out) {

        }

        @Override
        public Truth truth(long start, long end, Term template, NAR nar) {
            return null;
        }

        @Override
        public void clear() {

        }

    };

    /** matches, and projects to the specified time-range if necessary */
    default Task answer(long start, long end, Term template, Predicate<Task> filter, NAR nar) {
        Task m = match(start, end, template, filter, nar);
        if (m == null)
            return null;
        if (m.containedBy(start,end))
            return m;
        Task t = Task.project(false, m, start, end, nar, false);
        if (t instanceof TaskProxy)
            t = ((TaskProxy)t).clone();
        return t;
    }






    



    /* simple metric that guages the level of inconsistency between two differnt tables, used in measuring graph intercoherency */
    /*default float coherenceAgainst(BeliefTable other) {
        
        return Float.NaN;
    }*/





















































































































































































    /*public float rank(final Task s, final long now) {
        return rankBeliefConfidenceTime(s, now);
    }*/







}










































    /* when does projecting to now not play a role? I guess there is no case,
    
    
    
    Ranker BeliefConfidenceOrOriginality = (belief, bestToBeat) -> {
        final float confidence = belief.getTruth().getConfidence();
        final float originality = belief.getOriginality();
        return or(confidence, originality);
    };*/










































