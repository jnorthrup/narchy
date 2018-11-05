package nars.table;

import nars.NAR;
import nars.Task;
import nars.control.proto.Remember;
import nars.task.util.Answer;

import java.io.PrintStream;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class EmptyBeliefTable implements BeliefTable {

    @Override
    public Stream<? extends Task> streamTasks() {
        return Stream.empty();
    }

    @Override
    public void match(Answer t) {
        //nothing
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
    public int size() {
        return 0;
    }


    @Override
    public void add(/*@NotNull*/ Remember r,  /*@NotNull*/ NAR nar) {

    }


    @Override
    public void print(/*@NotNull*/ PrintStream out) {

    }

    @Override
    public void clear() {

    }

}
