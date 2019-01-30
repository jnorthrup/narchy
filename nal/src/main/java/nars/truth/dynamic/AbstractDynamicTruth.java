package nars.truth.dynamic;

import nars.NAR;
import nars.Task;
import nars.concept.util.ConceptBuilder;
import nars.table.BeliefTable;
import nars.table.BeliefTables;
import nars.table.dynamic.DynamicTruthTable;
import nars.term.Term;
import nars.truth.Truth;

import java.util.List;

/**
 * Created by me on 12/4/16.
 */
abstract public class AbstractDynamicTruth {

    abstract public Truth truth(DynEvi var1, NAR nar);

    public abstract boolean components(Term superterm, long start, long end, ObjectLongLongPredicate<Term> each);

    /**
     * used to reconstruct a dynamic target from some or all components
     */
    abstract public Term reconstruct(Term superterm, List<Task> c, NAR nar, long start, long end);

    /**
     * allow filtering of resolved Tasks
     */
    public boolean acceptComponent(Term superTerm, Term componentTerm, Task componentTask) {
        return true;
    }

    public BeliefTable newTable(Term t, boolean beliefOrGoal, ConceptBuilder cb) {
        return new BeliefTables(
                new DynamicTruthTable(t, this, beliefOrGoal),
                cb.newTemporalTable(t, beliefOrGoal),
                cb.newEternalTable(t)
        );
    }


    @FunctionalInterface
    public interface ObjectLongLongPredicate<T> {
        boolean accept(T object, long start, long end);
    }





}