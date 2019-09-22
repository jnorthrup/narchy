package nars.derive.action.op;

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

        boolean single = patternsEqual;
        //assert(!(single && !d.taskTerm.equals(d.beliefTerm))); //should be eliminated by prefilters
        if (single && !d.taskTerm.equals(d.beliefTerm))
            return false;

        boolean fwd = single || fwd(d);

        int before = d.size();
        if (before!=0) {
            d.revert(0); //TODO ensure avoided the need for this and remove
            //assert(before==0);
        }

        if (unify(d, fwd, single) && !single) {
            if (d.live())
                unify(d, !fwd, true);
        }

        d.clear(); //revert(0);

        if (!d.live())
            return false; //break

        return true;
    }




}
