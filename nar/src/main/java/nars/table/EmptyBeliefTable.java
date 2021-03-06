package nars.table;

import nars.Task;
import nars.control.op.Remember;
import nars.task.util.Answer;

import java.io.PrintStream;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class EmptyBeliefTable implements BeliefTable {

    public static final BeliefTable Empty = new EmptyBeliefTable();

    protected EmptyBeliefTable() {

    }

    @Override
    public Stream<? extends Task> taskStream() {
        return Stream.empty();
    }

    @Override
    public void match(Answer a) {
        //nothing
    }

    @Override
    public Task[] taskArray() {
        return Task.EmptyArray;
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
    public void remember( Remember r  ) {

    }


    @Override
    public void print( PrintStream out) {

    }

    @Override
    public void clear() {

    }

}
