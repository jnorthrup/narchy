package nars.table.question;

import nars.NAR;
import nars.Task;
import nars.control.proto.Remember;
import nars.table.TaskTable;
import nars.term.Term;

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
        public Stream<? extends Task> streamTasks() {
            return Stream.empty();
        }

        @Override
        public void add(/*@NotNull*/ Remember r, NAR n) {

        }

        @Override
        public Task match(long start, long end, Term template, NAR nar) {
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
        public void setCapacity(int newCapacity) {

        }


        @Override
        public int size() {
            return 0;
        }

    };

    void setCapacity(int newCapacity);


}
