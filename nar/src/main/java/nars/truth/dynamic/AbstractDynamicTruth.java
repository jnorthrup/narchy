package nars.truth.dynamic;

import jcog.Util;
import jcog.util.ObjectLongLongPredicate;
import nars.table.BeliefTable;
import nars.table.dynamic.DynamicTruthTable;
import nars.term.Compound;
import nars.term.Term;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.ObjectBooleanToObjectFunction;
import org.jetbrains.annotations.Nullable;

/**
 * Created by me on 12/4/16.
 */
abstract public class AbstractDynamicTruth {

    abstract public Truth truth(DynTaskify d /* eviMin, */);


    public abstract boolean evalComponents(Compound superterm, long start, long end, ObjectLongLongPredicate<Term> each);

    /**
     * used to reconstruct a dynamic target from some or all components
     */
    abstract public Term reconstruct(Compound superterm, DynTaskify d, long start, long end);

//    /**
//     * allow filtering of resolved Tasks
//     */
//    public boolean acceptComponent(Compound superTerm, Task componentTask) {
//        return true;
//    }


    @Nullable
    public static ObjectBooleanToObjectFunction<Term, BeliefTable[]> table(AbstractDynamicTruth... models) {
        return (Term t, boolean beliefOrGoal) ->
                Util.map(m -> new DynamicTruthTable(t, m, beliefOrGoal), new BeliefTable[models.length], models);
    }


}