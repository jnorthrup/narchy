package nars.unify;

import nars.term.Term;
import nars.term.Variable;

public class OneTermUnification extends DeterministicUnification {

    public final Term tx, ty;

    public OneTermUnification(Term tx, Term ty) {
        super();
        this.tx = tx;
        this.ty = ty;
    }

    @Override
    protected boolean equals(DeterministicUnification obj) {
        if (obj instanceof nars.unify.OneTermUnification) {
            nars.unify.OneTermUnification u = (nars.unify.OneTermUnification) obj;
            return tx.equals(u.tx) && ty.equals(u.ty);
        }
        return false;
    }

    @Override
    public Term xy(Term t) {
        if (tx.equals(t)) return ty;
        else return null;
    }

    @Override
    void apply(Unify u) {
        boolean applied = u.putXY((Variable/*HACK*/) tx, ty);
        assert (applied);
    }
}
