package nars.unify.op;

import nars.$;
import nars.Op;
import nars.derive.Derivation;
import nars.derive.premise.PreDerivation;
import nars.term.atom.Atomic;
import nars.term.control.AbstractPred;
import nars.term.control.PrediTerm;

import java.util.Collections;
import java.util.List;

import static nars.Op.VAR_PATTERN;

/**
 * requires a specific subterm to have minimum bit structure
 */
public final class TaskBeliefHas extends AbstractPred<PreDerivation> {

    /** higher number means a stucture with more enabled bits will be decomposed to its components */
    public static final int SPLIT_THRESHOLD = 4;

    public final boolean taskOrBelief;
    public final int structure;
    private final boolean inclOrExclude;

    public static List<TaskBeliefHas> get(boolean taskOrBelief, int bits, boolean inclOrExcl) {

        
        bits &= ~VAR_PATTERN.bit;

        int numBits = Integer.bitCount(bits);
        assert (numBits > 0);
        if ((numBits == 1) || (numBits > SPLIT_THRESHOLD)) {
            return Collections.singletonList(new TaskBeliefHas(taskOrBelief, bits, inclOrExcl));
        } else {
            List<TaskBeliefHas> components = $.newArrayList(numBits);
            for (Op o : Op.values()) {


                int b = o.bit;
                if ((bits & b) != 0) { 
                    components.add(new TaskBeliefHas(taskOrBelief, b, inclOrExcl));
                }
            }
            return components;
        }
    }

    final static Atomic has = Atomic.the("has");

    private TaskBeliefHas(boolean taskOrBelief, int structure, boolean includeOrExclude) {
        super($.func(has,
                taskOrBelief ? Derivation.Task : Derivation.Belief,
                Op.strucTerm(structure)).negIf(!includeOrExclude)
        );

        this.inclOrExclude = includeOrExclude;
        this.structure = structure;
        this.taskOrBelief = taskOrBelief;

        assert(this.structure > 0): "no filter effected";
    }

    @Override
    public boolean remainInAND(PrediTerm[] p) {
        boolean subsumed = false;
        for (PrediTerm x : p) {
            if (x==this) continue;
            if (x instanceof TaskBeliefIs) {
                //is is more specific than has, so subsume if redundant

                TaskBeliefIs t = (TaskBeliefIs)x;
                if (((taskOrBelief && t.task) || (!taskOrBelief && t.belief))) {
                    if (t.isOrIsNot == inclOrExclude) {
                        if (!subsumed && (t.isOrIsNot) &&((t.structure | structure) == t.structure)){
                            subsumed = true;
                        }
                    } else {
                        if ((t.structure & structure) != 0)
                            throw new RuntimeException("conflict: " + t + " " + this);
                    }
                }
            }
        }
        return !subsumed;
    }

    @Override
    public final boolean test(PreDerivation d) {
        return inclOrExclude == Op.hasAll(
                taskOrBelief ? d._taskStruct : d._beliefStruct,
                structure);
    }


    @Override
    public float cost() {
        return 0.25f;
    }
}
