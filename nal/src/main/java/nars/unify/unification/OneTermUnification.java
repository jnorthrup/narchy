package nars.unify.unification;

import nars.term.Term;
import nars.term.Variable;
import nars.unify.Unify;

public class OneTermUnification extends DeterministicUnification {

    public final Term tx, ty;

    public OneTermUnification(Term tx, Term ty) {
        super();
        this.tx = tx;
        this.ty = ty;
    }

    @Override
    protected boolean equals(DeterministicUnification obj) {
        if (obj instanceof OneTermUnification) {
            OneTermUnification u = (OneTermUnification) obj;
            return tx.equals(u.tx) && ty.equals(u.ty);
        }
        return false;
    }

    @Override
    public Term xy(Variable t) {
        return tx.equals(t) ? ty : null;
    }

    @Override
    public boolean apply(Unify u) {
        return u.putXY((Variable/*HACK*/) tx, ty);
    }
}
