package nars.table;

import nars.NAR;
import nars.Task;
import nars.control.proto.Remember;
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
    public Task match(long start, long end, @Nullable Term template, EternalTable ete, NAR nar, Predicate<Task> filter) {
        return ref.match(start, end, template, ete, nar, filter);
    }

    @Override
    public Truth truth(long start, long end, EternalTable eternal, Term template, int dur) {
        return ref.truth(start, end, eternal, template, dur);
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
    public void add(Remember t, NAR n) {
        ref.add(t, n);
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
