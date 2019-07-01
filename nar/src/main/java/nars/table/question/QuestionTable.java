package nars.table.question;

import nars.NAR;
import nars.Task;
import nars.control.op.Remember;
import nars.table.TaskTable;
import nars.task.util.Answer;
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
        public Stream<? extends Task> taskStream() {
            return Stream.empty();
        }

        @Override
        public void match(Answer m) {

        }

        @Override
        public void remember(/*@NotNull*/ Remember r) {

        }

        @Override
        public Task match(long start, long end, Term template, float dur, NAR nar) {
            return null;
        }


        @Override
        public void clear() {

        }

        @Override
        public void forEachTask(Consumer<? super Task> x) {

        }

        @Override
        public boolean removeTask(Task x, boolean delete) {
            return false;
        }


        @Override
        public void setTaskCapacity(int newCapacity) {

        }


        @Override
        public int taskCount() {
            return 0;
        }

    };



}
