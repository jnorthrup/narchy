package nars.table.temporal;

import jcog.Skill;
import nars.NAR;
import nars.Task;
import nars.control.proto.Remember;
import nars.table.TaskTable;
import nars.table.eternal.EternalTable;
import nars.task.Revision;
import nars.task.signal.SignalTask;
import nars.task.util.TaskRegion;
import nars.term.Term;
import nars.truth.Stamp;
import nars.truth.Truth;
import nars.truth.polation.TruthIntegration;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.set.primitive.ImmutableLongSet;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;


/**
 * https://en.wikipedia.org/wiki/Compressed_sensing
 */
@Skill("Compressed_sensing")
public interface TemporalBeliefTable extends TaskTable {


    /**
     * range will be between 0 and 1
     * should return non-negative values only
     */
    static float value(Task t, long start, long end, long dur) {

        float v = TruthIntegration.eviInteg(t, start, end, dur);

//        if (t.isGoal()) {
//            //v *= t.polarity();
//            //exp(polarity)? ~= softmax
//            v *= 0.5f + t.polarity()/2f;
//        }

        return v;

//        float absDistance =
//
//
//                t.midTimeTo( start ) + ((start!=end) ? t.midTimeTo(end) : 0);
//
//
//
//        float ownEvi =
//                Revision.eviInteg(t, t.start(), t.end(), dur);
//
//
//
//        return  ( ownEvi / (1 + (/*Math.log(1+*/absDistance/ dur)));


    }

    static FloatFunction<TaskRegion> mergeability(Task x, float tableDur) {
        ImmutableLongSet xStamp = Stamp.toSet(x);

        long xStart = x.start();
        long xEnd = x.end();

        return (TaskRegion _y) -> {
            Task y = (Task) _y;

            if (Stamp.overlapsAny(xStamp, y.stamp()))
                return Float.NaN;


            return
                    (1f / (1f +
                            (Math.abs(y.start() - xStart) + Math.abs(y.end() - xEnd)) / tableDur))

                    ;
        };
    }

    static float costDtDiff(Term template, Term x, int dur) {
        return Revision.dtDiff(template, x) / (dur /* * dur*/);
    }

    /**
     * finds or generates the strongest match to the specified parameters.
     * Task against is an optional argument which can be used to compare internal temporal dt structure for similarity
     */
    Task match(long start, long end, @Nullable Term template, EternalTable eternals, NAR nar, Predicate<Task> filter);

    /**
     * estimates the truth value for the provided time.
     * the eternal table's top value, if existent, contributes a 'background'
     * level in interpolation.
     */
    Truth truth(long start, long end, EternalTable eternal, Term template, int dur);

    void setCapacity(int temporals);

    void update(SignalTask x, Runnable change);

    long tableDur();

    void whileEach(Predicate<? super Task> each);

    /**
     * finds all temporally intersectnig tasks.  minT and maxT inclusive.  while the predicate remains true, it will continue scanning
     * TODO contains/intersects parameter
     */
    default void whileEach(long minT, long maxT, Predicate<? super Task> each) {
        whileEach(x -> {
            if (!x.isDeleted() && x.intersects(minT, maxT))
                return each.test(x);
            else
                return true;
        });
    }


    TemporalBeliefTable Empty = new TemporalBeliefTable() {

        @Override
        public void add(Remember t, NAR n) {
        }

        @Override
        public void update(SignalTask x, Runnable change) {

        }

        @Override
        public long tableDur() {
            return 0;
        }

        @Override
        public void setCapacity(int c) {

        }

        @Override
        public int capacity() {

            return Integer.MAX_VALUE;
        }

        @Override
        public boolean removeTask(Task x) {
            return false;
        }

        @Override
        public int size() {
            return 0;
        }


        @Override
        public Stream<Task> streamTasks() {
            return Stream.empty();
        }

        @Override
        public void forEachTask(Consumer<? super Task> x) {

        }

        @Override
        public void whileEach(Predicate<? super Task> each) {

        }

        @Override
        public void whileEach(long minT, long maxT, Predicate<? super Task> x) {

        }

        @Override
        public Task match(long start, long end, @Nullable Term template, EternalTable ete, NAR nar, Predicate<Task> filter) {
            return null;
        }

        @Override
        public Truth truth(long start, long end, EternalTable eternal, Term template, int dur) {
            return null;
        }

        @Override
        public void clear() {

        }
    };



}
