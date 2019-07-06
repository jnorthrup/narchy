package nars.table;

import nars.Op;
import nars.Task;
import nars.control.op.Remember;
import nars.task.util.Answer;

import java.io.PrintStream;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class EmptyBeliefTable implements BeliefTable {

    @Override
    public Stream<? extends Task> taskStream() {
        return Stream.empty();
    }

    @Override
    public void match(Answer t) {
        //nothing
    }

    @Override
    public Task[] taskArray() {
        return Op.EmptyTaskArray;
    }

    @Override
    public void forEachTask(Consumer<? super Task> x) {

    }

    @Override
    public boolean removeTask(Task x, boolean delete) {
        return false;
    }


    @Override
    public void forEachTask(long minT, long maxT, Consumer<? super Task> x) {

    }

    @Override
    public void setTaskCapacity(int newCapacity) {

    }


    @Override
    public int taskCount() {
        return 0;
    }


    @Override
    public void remember(/*@NotNull*/ Remember r  /*@NotNull*/) {

    }


    @Override
    public void print(/*@NotNull*/ PrintStream out) {

    }

    @Override
    public void clear() {

    }

}
