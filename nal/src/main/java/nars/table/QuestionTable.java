package nars.table;

import jcog.data.map.MRUCache;
import nars.NAR;
import nars.Task;
import nars.concept.TaskConcept;
import nars.task.NALTask;
import nars.term.Term;

import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * task table used for storing Questions and Quests.
 * simpler than Belief/Goal tables
 */
public interface QuestionTable extends TaskTable {


    void capacity(int newCapacity);


    /**
     * allows question to pass through it to the link activation phase, but
     * otherwise does not store it
     */
    ///*@NotNull*/ QuestionTable Unstored = new EmptyQuestionTable();

    /*@NotNull*/ QuestionTable Empty = new QuestionTable() {

        @Override
        public Stream<Task> streamTasks() {
            return Stream.empty();
        }

        @Override
        public boolean add(/*@NotNull*/ Task t, TaskConcept c, NAR n) {
            return false;
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
        public void clear() {

        }

        @Override
        public void forEachTask(Consumer<? super Task> x) {

        }

        @Override
        public boolean removeTask(Task x) {
            return false;
        }


        @Override
        public void capacity(int newCapacity) {

        }

        @Override
        public int capacity() {
            return 0;
        }

        @Override
        public int size() {
            return 0;
        }

    };

    /**
     * unsorted, MRU policy.
     * this impl sucks actually
     * TODO make one based on ArrayHashSet
     */
    class DefaultQuestionTable extends MRUCache<Task, Task> implements QuestionTable {

        final Object lock =
                this;
        //new Object();

        public DefaultQuestionTable() {
            super(0);
        }

        @Override
        public void capacity(int newCapacity) {
            synchronized (lock) {
                setCapacity(newCapacity);
                //TODO
                //while (size() > newCapacity) {
                //   remove(weakest());
                //}
            }
        }

        @Override
        protected void onEvict(Map.Entry<Task, Task> entry) {
//            Task x = entry.getKey();
//            Task y = entry.getValue();
//            x.delete();
//            if (y != x)
//                y.delete();
        }

        @Override
        public boolean add(/*@NotNull*/ Task t, TaskConcept c, NAR n) {
            Task u;
            final float tPri = t.pri();
            if (tPri != tPri)
                return false;

            synchronized (lock) {
                u = merge(t, t, (prev, next) -> {
                    ((NALTask) prev).causeMerge(next);
                    return prev;
                });
            }

            if (u != t) {
                //absorbed
                //t.delete();
            }

            return true;
        }

        @Override
        public int capacity() {
            return capacity;
        }

//
//        @Override
//        public Iterator<Task> iterator() {
//            return ArrayIterator.get(toArray());
//        }

        @Override
        public Stream<Task> streamTasks() {
            Task[] t = toArray();
            return t.length > 0 ? Stream.of(t) : Stream.empty();
        }

        public Task[] toArray() {

            int s = size();
            if (s == 0) {
                return Task.EmptyArray;
            } else {
                synchronized (lock) {
                    return values().toArray(new Task[s]);
                }
            }

        }

        @Override
        public void forEachTask(Consumer<? super Task> x) {
            Task[] t = toArray();
            for (Task y : t) {
                if (y == null)
                    continue;
                if (y.isDeleted()) {
                    removeTask(y);
                } else {
                    x.accept(y);
                }
            }
        }

        @Override
        public boolean removeTask(Task x) {
            synchronized (lock) {
                return remove(x) != null;
            }
        }

        @Override
        public void clear() {
            synchronized (lock) {
                super.clear();
            }
        }

    }

//    /** untested */
//    class EmptyQuestionTable extends QuestionTable.NullQuestionTable {
//
//        final static HijackQuestionTable common = new HijackQuestionTable(1024, 3);
//
//        @Override
//        public void add(/*@NotNull*/ Task t, BaseConcept c, NAR n) {
//            Task e = common.get(t);
//            float activation = t.priElseZero();
//            if (e ==null) {
//                common.put(t);
//
//
//                //TaskTable.activate(t, t.priElseZero(), n);
//            } else {
//                activation -= e.priElseZero();
//            }
//
//            Activate.activate(t, activation, n);
//        }
//
//        @Override
//        public int capacity() {
//            return Integer.MAX_VALUE;
//        }
//
//
//    }
}
