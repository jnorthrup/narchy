package nars.table;

import nars.NAR;
import nars.Task;
import nars.concept.TaskConcept;
import nars.task.signal.SignalTask;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class ProxyTemporalBeliefTable implements TemporalBeliefTable {

    public final TemporalBeliefTable ref;

    public ProxyTemporalBeliefTable(TemporalBeliefTable ref) {
        this.ref = ref;
    }

    @Override
    public Task match(long start, long end, @Nullable Term against, NAR nar, Predicate<Task> filter) {
        return ref.match(start, end, against, nar, filter);
    }

    @Override
    public Truth truth(long start, long end, EternalTable eternal, int dur) {
        return ref.truth(start, end, eternal, dur);
    }

    @Override
    public void setCapacity(int temporals) {
        ref.setCapacity(temporals);
    }

    @Override
    public void update(SignalTask x, Runnable change) {
        ref.update(x, change);
    }

    @Override
    public void whileEach(Predicate<? super Task> each) {
        ref.whileEach(each);
    }

    @Override
    public boolean add(Task t, TaskConcept c, NAR n) {
        return ref.add(t, c, n);
    }

    @Override
    public int capacity() {
        return ref.capacity();
    }

    @Override
    public int size() {
        return ref.size();
    }

    @Override
    public void forEachTask(Consumer<? super Task> x) {
        ref.forEachTask(x);
    }

    @Override
    public boolean removeTask(Task x) {
        return ref.removeTask(x);
    }

    @Override
    public void clear() {
        ref.clear();
    }

    @Override
    public Stream<Task> streamTasks() {
        return ref.streamTasks();
    }


}
