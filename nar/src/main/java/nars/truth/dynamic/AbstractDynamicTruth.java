package nars.truth.dynamic;

import jcog.Util;
import jcog.util.ObjectLongLongPredicate;
import nars.NAL;
import nars.NAR;
import nars.Task;
import nars.concept.TaskConcept;
import nars.table.BeliefTable;
import nars.table.dynamic.DynamicTruthTable;
import nars.term.Compound;
import nars.term.Term;
import nars.time.When;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.ObjectBooleanToObjectFunction;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

import static nars.Op.BELIEF;
import static nars.Op.GOAL;

/**
 * Created by me on 12/4/16.
 */
abstract public class AbstractDynamicTruth {

    abstract public Truth truth(DynTaskify d /* eviMin, */);


    public abstract boolean evalComponents(Compound superterm, long start, long end, ObjectLongLongPredicate<Term> each);

    /**
     * used to reconstruct a dynamic target from some or all components
     */
    abstract public Term reconstruct(Compound superterm, long start, long end, DynTaskify d);


    /** default subconcept Task resolver */
    public Task subTask(TaskConcept subConcept, Term subTerm, long subStart, long subEnd, Predicate<Task> filter, DynTaskify d) {

        BeliefTable table = (BeliefTable) subConcept.table(d.beliefOrGoal ? BELIEF : GOAL);
        NAR nar = d.nar;
        int dur = d.dur;
        Task bt;
        switch (NAL.DYN_TASK_MATCH_MODE) {
            case 0:
                bt = table.matchExact(subStart, subEnd, subTerm, filter, dur, nar);
                break;
            case 1:
                bt = table.match(subStart, subEnd, subTerm, filter, dur, nar);
                break;
            case 2:
                bt = table.sample(new When(subStart, subEnd, dur, nar), subTerm, filter);
                break;
            default:
                throw new UnsupportedOperationException();
        }
        return bt;
    }

    @Nullable
    public static ObjectBooleanToObjectFunction<Term, BeliefTable[]> table(AbstractDynamicTruth... models) {
        return (Term t, boolean beliefOrGoal) ->
                Util.map(m -> new DynamicTruthTable(t, m, beliefOrGoal), new BeliefTable[models.length], models);
    }


}