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

    public DefaultBeliefTable(TemporalBeliefTable t) {
        eternal = new EternalTable(0);
        temporal = t;
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
    public final void forEachTask(Consumer<? super Task> action) {
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
        return eternal.size() /* eternal */ + temporal.size();
    }

    @Override
    @Deprecated
    public int capacity() {
        //throw new UnsupportedOperationException("doesnt make sense to call this");
        return eternal.capacity() /* eternal */ + temporal.capacity();
    }

    @Override
    public final void setCapacity(int eternals, int temporals) {
        temporal.setCapacity(temporals);
        eternal.setCapacity(eternals);
    }

    /**
     * get the most relevant belief/goal with respect to a specific time.
     */
    @Override
    public Task match(long start, long end, Term template, NAR nar, Predicate<Task> filter) {

        Task ete = eternal.strongest();
        if (filter!=null && ete!=null && !filter.test(ete))
            ete = null;

        Task tmp = temporal.match(start, end, template, nar, filter);

        if (tmp == null) {
            return ete;
        } else {
            if (ete == null) {
                return tmp;
            } else {
                return (ete.evi() > tmp.evi(start, end, nar.dur())) ?
                        ete : tmp;
            }
        }
    }


    @Override
    public boolean add(Task input, TaskConcept concept, NAR nar) {
        return (input.isEternal() ? eternal : temporal).add(input, concept, nar);
    }


}



