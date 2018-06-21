package nars.table;

import jcog.bag.impl.hijack.PriorityHijackBag;
import jcog.data.map.MRUCache;
import nars.NAR;
import nars.Task;
import nars.control.proto.Remember;
import nars.term.Term;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * task table used for storing Questions and Quests.
 * simpler than Belief/Goal tables
 */
public interface QuestionTable extends TaskTable {


    /**
     * allows question to pass through it to the link activation phase, but
     * otherwise does not store it
     */


    /*@NotNull*/ QuestionTable Empty = new QuestionTable() {

        @Override
        public Stream<Task> streamTasks() {
            return Stream.empty();
        }

        @Override
        public void add(/*@NotNull*/ Remember t, NAR n) {

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

    void capacity(int newCapacity);

    /**
     * unsorted, MRU policy.
     * this impl sucks actually
     * TODO make one based on ArrayHashSet
     */
    class DefaultQuestionTable extends MRUCache<Task, Task> implements QuestionTable {

        final Object lock =
                this;


        public DefaultQuestionTable() {
            super(0);
        }

        @Override
        public void capacity(int newCapacity) {
            synchronized (lock) {
                setCapacity(newCapacity);


            }
        }


        @Override
        public void add(/*@NotNull*/ Remember r, NAR n) {
            Task u;
            Task t = r.input;
            synchronized (lock) {
                u = merge(t, t, (prev, next) -> {
                    r.merge(prev);
                    return next;
                });
            }

        }

        @Override
        public int capacity() {
            return capacity;
        }


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
            for (Task y: t) {
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

    class HijackQuestionTable extends PriorityHijackBag<Task, Task> implements QuestionTable {

        public HijackQuestionTable(int cap, int reprobes) {
            super(cap, reprobes);
        }

        @Override
        protected Task merge(Task existing, Task incoming, @Nullable MutableFloat overflowing) {
            return existing;
        }

//        @Override
//        public void priAdd(Task entry, float amount) {
//
//        }

        @Override
        public Task key(Task value) {
            return value;
        }

        @Override
        public boolean isEmpty() {
            return super.isEmpty();
        }

        @Override
        public void capacity(int newCapacity) {
            setCapacity(newCapacity);
        }

        @Override
        public void add(Remember r, NAR n) {
            Task x = put(r.input, null);
            if (x != r) {
                if (x!=null)
                    r.merge(x); //existing
                else
                    r.reject();
            } else {
                r.remember(x);
            }
            //TODO track displaced questions
        }

        @Override
        public void forEachTask(Consumer<? super Task> x) {
            forEachKey(x);
        }

        @Override
        public boolean removeTask(Task x) {
            return remove(x) != null;
        }

        @Override
        public Stream<Task> streamTasks() {
            return stream();
        }

        @Override
        public Task sample(long start, long end, Term template, NAR nar) {
            //commit();
            switch (size()) {
                case 0:
                    return null;
                case 1:
                    return next(0, (xx) -> false); //first one
                default:
                    return QuestionTable.super.sample(start, end, template, nar);
            }
        }
    }


}
