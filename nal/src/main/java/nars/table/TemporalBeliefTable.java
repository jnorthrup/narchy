package nars.table;

import jcog.math.Interval;
import nars.NAR;
import nars.Task;
import nars.concept.TaskConcept;
import nars.task.SignalTask;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;


public interface TemporalBeliefTable extends TaskTable, Iterable<Task> {


    /**
     * range will be between 0 and 1
     */
    static float temporalTaskPriority(Task t, long start, long end, int dur) {
//        if (t.isDeleted())
//            return Float.NEGATIVE_INFINITY;

        //return ((1+t.conf()) * (1+t.priElseZero())) / (1f + Math.abs((start+end)/2 - t.mid())/((float)dur));
        //return t.conf() / (1f + t.distanceTo(start, end)/((float)dur));
        //return (float) (t.conf() / (1f + Math.log(1f+t.distanceTo(start, end)/((float)dur))));
        //return t.conf() / (1f + t.distanceTo(start, end)/dur);

//
//        //float fdur = dur;
//        //float range = t.range();
        return
                t.evi(start, end, dur)
                //t.evi() * (1+Interval.intersectLength(start, end, t.start(), t.end()))

////                //t.conf(now, dur) *
////                //t.evi(now, dur) *
////                //* range == 0 ? 1f : (float) (1f + Math.sqrt(t.range()) / dur); ///(1+t.distanceTo(start, end)))); ///fdur
        ;
//        float fdur = dur;
//        return
//                //(1f + t.evi()) *
//                //(t.evi(start,end,dur))
//                (t.conf(start,end,dur))
//                * (float)Math.sqrt(1f + t.range()/fdur) //boost for duration
//                ;
//
//                //(1f + t.evi()) * //raw because time is considered below. this covers cases where the task eternalizes
//                //(1f / (1 + t.distanceTo(start, end)/fdur));
//
//                //(1f + t.conf()) * //raw because time is considered below. this covers cases where the task eternalizes
//                //t.evi(start,end,dur) *
//                //t.conf(now, dur) *
//                //t.evi(now, dur) *
//                /* ((float)Math.sqrt(1+t.range()/fdur)) */
//                 //1 / ((1 + t.distanceTo(start, end)/fdur));
    }

    /** finds or generates the strongest match to the specified parameters.
     * Task against is an optional argument which can be used to compare internal temporal dt structure for similarity */
    Task match(long start, long end, @Nullable Term against, NAR nar);

    /** estimates the truth value for the provided time.
     * the eternal table's top value, if existent, contributes a 'background'
     * level in interpolation.
     * */
    Truth truth(long start, long end, EternalTable eternal, int dur);


    void setCapacity(int temporals);

    default Consumer<Task> stretch(SignalTask changed) {
        throw new UnsupportedOperationException();
    }

    TemporalBeliefTable Empty = new TemporalBeliefTable() {

        @Override
        public void add(@NotNull Task t, TaskConcept c, NAR n) {

        }

        @Override
        public void setCapacity(int c) {

        }

        @Override
        public int capacity() {
            //throw new UnsupportedOperationException();
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
        public Iterator<Task> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public Stream<Task> stream() {
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
        public Task match(long start, long end, @Nullable Term against, NAR nar) {
            return null;
        }

        @Override
        public Truth truth(long start, long end, EternalTable eternal, int dur) {
            return null;
        }

        @Override
        public void clear() {

        }
    };


    public void whileEach(Predicate<? super Task> each);

    /** minT and maxT inclusive.  while the predicate remains true, it will continue scanning */
    default void whileEach(long minT, long maxT, Predicate<? super Task> each) {
        whileEach(x -> {
            if (x.start() >= minT && x.end() <= maxT)
                if (!each.test(x))
                    return false; //stop
            return true; //continue
        });
    }
}
