package nars.derive;

import nars.Op;
import nars.term.Term;
import nars.unify.UnifySubst;
import org.jetbrains.annotations.Nullable;

public class UnifyPremise extends UnifySubst {

    private Term beliefTerm, beliefTermUnified;

    public UnifyPremise() {
        super(Premise.var == Op.VAR_QUERY.bit ? Op.VAR_QUERY : null, null);
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
