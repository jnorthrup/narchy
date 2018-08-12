package nars.table;

import nars.NAR;
import nars.Task;
import nars.control.proto.Remember;
import nars.task.util.TaskRank;
import nars.term.Term;
import nars.truth.Truth;

import java.io.PrintStream;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class EmptyBeliefTable implements BeliefTable {

    @Override
    public Stream<? extends Task> streamTasks() {
        return Stream.empty();
    }

    @Override
    public void match(TaskRank t) {
        //nothing
    }

    @Override
    public Task sample(long start, long end, Term template, NAR nar) {
        return null;
    }

    @Override
    public void forEachTask(Consumer<? super Task> x) {

    }

    @Override
    public boolean removeTask(Task x) {
        return false;
    }


    @Override
    public void forEachTask(long minT, long maxT, Consumer<? super Task> x) {

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
