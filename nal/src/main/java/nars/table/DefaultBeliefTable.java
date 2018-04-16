package nars.table;

import nars.NAR;
import nars.Task;
import nars.concept.TaskConcept;
import nars.term.Term;
import nars.truth.Truth;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;


/**
 * Stores beliefs ranked in a sorted ArrayList, with strongest beliefs at lowest indexes (first iterated)
 */
public class DefaultBeliefTable implements BeliefTable {

    public final EternalTable eternal;

    public final TemporalBeliefTable temporal;

    public DefaultBeliefTable(EternalTable e, TemporalBeliefTable t) {
        eternal = e;
        temporal = t;
    }

    public DefaultBeliefTable(TemporalBeliefTable t) {
        this(new EternalTable(0), t);
    }

    @Override
    public boolean add(Task x, TaskConcept concept, NAR nar) {
        return table(x).add(x, concept, nar);
    }

    protected final boolean add(Task x, NAR nar) {
        TaskConcept concept = (TaskConcept) x.concept(nar, true);
        return concept!=null && table(x).add(x, concept, nar);
    }

    @Override
    public Stream<Task> streamTasks() {
        return Stream.concat(eternal.streamTasks(), temporal.streamTasks()).filter(x -> !x.isDeleted());
    }

    /**
     * TODO this value can be cached per cycle (when,now) etc
     */
    @Override
    public Truth truth(long start, long end, NAR nar) {
        return temporal.truth(start, end, eternal, nar.dur());
    }

    @Override
    public boolean removeTask(Task x) {
        return (x.isEternal()) ? eternal.removeTask(x) : temporal.removeTask(x);
    }

    @Override
    public void clear() {
        temporal.clear();
        eternal.clear();
    }

//    @NotNull
//    @Override
//    @Deprecated
//    public final Iterator<Task> iterator() {
//        return Iterators.concat(
//                eternal.iterator(),
//                temporal.iterator()
//        );
//    }

    @Override
    public void forEachTask(boolean includeEternal, long minT, long maxT, Consumer<? super Task> x) {
        if (includeEternal) {
            eternal.forEachTask(x);
        }
        temporal.whileEach(minT, maxT, (t)-> { x.accept(t); return true; });
    }

//    @Override
//    public void forEach(Consumer<? super Task> action) {
//        forEachTask(action);
//    }

    @Override
    public void forEachTask(Consumer<? super Task> action) {
        eternal.forEachTask(action);
        temporal.forEachTask(action);
    }

    @Override
    public float priSum() {
        final float[] total = {0};
        Consumer<Task> totaler = t -> total[0] += t.priElseZero();
        eternal.forEachTask(totaler);
        temporal.forEachTask(totaler);
        return total[0];
    }

    @Override
    public int size() {
        return eternal.size()  + temporal.size();
    }

    @Override
    @Deprecated
    public int capacity() {
        //throw new UnsupportedOperationException("doesnt make sense to call this");
        return eternal.capacity() + temporal.capacity();
    }

    @Override
    public final void setCapacity(int eternals, int temporals) {
        temporal.setCapacity(temporals);
        eternal.setCapacity(eternals);
    }

    @Override
    public Task sample(long start, long end, Term template, NAR nar) {
        Task ete = eternal.sample(start, end, template, nar);
        Task tmp = temporal.sample(start, end, template, nar);
        return Task.eviMax(ete, tmp, start, end);
    }

    @Override
    public Task match(long start, long end, Term template, Predicate<Task> filter, NAR nar) {
        return temporal.match(start, end, template, eternal, nar, filter);
    }



    public final TaskTable table(Task x) {
        return table(x.isEternal());
    }

    public final TaskTable table(boolean eternalOrTemporal) {
         return eternalOrTemporal ? eternal : temporal;
    }

}



