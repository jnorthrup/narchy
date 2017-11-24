package nars.table;

import nars.NAR;
import nars.Task;
import nars.concept.BaseConcept;

import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * holds a set of ranked question/quests tasks
 * top ranking items are stored in the lower indexes so they will be first iterated
 */
public interface TaskTable extends Iterable<Task> {


    /**
     * attempt to insert a task.
     *
     * @return: the input task itself, it it was added to the table
     * an existing equivalent task if this was a duplicate
     */
    void add(Task t, BaseConcept c, NAR n);


    int capacity();


    /**
     * number of items in this collection
     */
    int size();

    default boolean isEmpty() {
        return size() == 0;
    }

//    @Nullable
//    default BivariateGridInterpolator getWaveFrequencyConfidenceTime() {
//        return null;
//    }
//
//    @Nullable
//    default UnivariateInterpolator getWaveFrequencyConfidence() {
//        return null;
//    }
//
//    @Nullable
//    default UnivariateInterpolator getWaveConfidenceTime() {
//        return null;
//    }


    void forEachTask(Consumer<? super Task> x);
//    default void forEachTask(Consumer<? super Task> x) {
//        //TODO filter deleted tasks
//        taskIterator().forEachRemaining(x);
//    }



    /** returns true if the task was removed */
    boolean removeTask(Task x);

    void clear();

    Stream<Task> stream();


    //boolean contains(Task t);

//    @Nullable
//    QuestionTable EMPTY = new QuestionTable() {
//
//        @Override
//        public
//        @Nullable
//        Task add(Task t, Memory m) {
//            return t;
//        }
//
//        @Override
//        public
//        @Nullable
//        Task get(Task t) {
//            return null;
//        }
//
//        @Override
//        public Iterator<Task> iterator() {
//            return Iterators.emptyIterator();
//        }
//
//        @Override
//        public int capacity() {
//            return 0;
//        }
//
//
//        @Override
//        public void setCapacity(int newCapacity) {
//
//        }
//
//        @Override
//        public int size() {
//            return 0;
//        }
//
//        @Override
//        public void clear() {
//
//        }
//
//        @Override
//        public boolean isEmpty() {
//            return true;
//        }
//
//        @Override
//        public void remove(Task belief, NAR nar) {
//            throw new UnsupportedOperationException();
//        }
//
//
//
//    };

//    /** forcibly remove a held Task
//     *  should eventually invoke TaskTable.removeTask() */
//    void remove(Task belief, List<Task> displ);


    //void add(Task incoming, List<Task> displaced);

    //Task put(Task incoming);
}
