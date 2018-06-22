package nars.table;

import nars.NAR;
import nars.Param;
import nars.Task;
import nars.control.proto.Remember;
import nars.task.signal.SignalTask;
import nars.term.Term;
import nars.truth.Truth;
import nars.truth.polation.TruthIntegration;

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



    @Override
    public void add(Remember r, NAR n) {
        table(r.isEternal()).add(r, n);

        if (Param.ETERNALIZE_FORGOTTEN_TEMPORALS) {
            if (eternal != EternalTable.EMPTY && !r.forgotten.isEmpty() &&
                    temporal.size()>=temporal.capacity()-1 /* some tolerance for full test */) {

                r.forgotten.forEach(t ->{
                   if (!(t instanceof SignalTask) && !t.isEternal()) {
                       //TODO maybe sort by evi decreasing
                       Task e = eternal.eternalize(t, temporal.capacity(), temporal.tableDur(), n);
                       if (e!=null)
                            eternal.add(r, n, e);
                   }
                });
            }
        }

    }


    @Override
    public Stream<Task> streamTasks() {
        return Stream.concat(eternal.streamTasks(), temporal.streamTasks());
    }

    /**
     * TODO this value can be cached per cycle (when,now) etc
     */
    @Override
    public Truth truth(long start, long end, Term template, NAR nar) {
        return temporal.truth(start, end, eternal, template, nar.dur());
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











    @Override
    public void forEachTask(boolean includeEternal, long minT, long maxT, Consumer<? super Task> x) {
        if (includeEternal) {
            eternal.forEachTask(x);
        }
        temporal.whileEach(minT, maxT, (t)-> { x.accept(t); return true; });
    }






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
        if (ete == null) return tmp;
        if (tmp == null) return ete;
        float e = TruthIntegration.eviInteg(ete,start,end,1);
        float t = TruthIntegration.eviInteg(tmp,start,end,1);
        return nar.random().nextFloat() < (t/Math.max(Float.MIN_NORMAL, (e+t))) ? tmp : ete;
    }

    @Override
    public Task match(long start, long end, Term template, Predicate<Task> filter, NAR nar) {
        return temporal.match(start, end, template, eternal, nar, filter);
    }

    public final TaskTable table(boolean eternalOrTemporal) {
         return eternalOrTemporal ? eternal : temporal;
    }

}



