package nars.derive.op;

import nars.derive.Derivation;
import nars.term.Term;

/**
 * simple non-caching depth-first TTL-limited derivation search
 *
 * this impl is simple but the major disadvantage is
 * that this depth-first-search is too limiting.
 *
 * a breadth-first search would give more balanced
 * coverage of the choices as well as a choice for heuristic
 * decision.
 * */
public class DirectPremiseUnify extends PremiseUnify {

    private final boolean patternsEqual;

    public DirectPremiseUnify(Term t, Term b, Taskify taskify) {
        super(t, b, taskify);
        this.patternsEqual = taskPat.equals(beliefPat);
    }

    @Override public boolean test(Derivation d) {

        boolean single = patternsEqual && d.taskTerm.equals(d.beliefTerm);

        boolean fwd = single || fwd(d);

        //first
        boolean unified = unify(d, fwd, single);

        if (unified && !single) {
            //second
            unify(d, !fwd, true);
        }

        return true;
    }




}
