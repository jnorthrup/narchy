package nars.derive.util;

import nars.derive.Deriver;
import nars.term.Term;
import nars.unify.UnifySubst;
import org.jetbrains.annotations.Nullable;

/** used to determine a premise's "belief task" for the provided "belief term",
 *  and other premise functionality at the start of a derivation */
public class PremisePreUnify extends UnifySubst {

    private Term beliefTerm, beliefTermUnified;

    public PremisePreUnify() {
        super(Deriver.PremiseUnifyVars, null);
    }


    @Nullable
    public Term unified(Term taskTerm, Term beliefTerm, int ttl) {

        clear();

        this.beliefTermUnified = this.beliefTerm = beliefTerm;

        return !transform(beliefTerm, beliefTerm, taskTerm, ttl) ? null : beliefTermUnified;

    }

    @Override
    protected boolean each(Term y) {
        y = y.unneg();
        if (!y.equals(beliefTerm)) {
            if (y.op().conceptualizable) {
                beliefTermUnified = y;
                return false;  //done
            }
        }
        return true; //continue
    }
}
