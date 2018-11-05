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

        if (!transform(beliefTerm, beliefTerm, taskTerm, ttl))
            return null;

        return beliefTermUnified;
    }

    @Override
    protected boolean each(Term y) {
        if (y.op().conceptualizable) {
            y = y.unneg();

            if (!y.equals(beliefTerm)) {
                beliefTermUnified = y;
                return false;  //done
            }
        }
        return true; //continue
    }
}
