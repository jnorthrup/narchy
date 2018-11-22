package nars.derive.op;

import nars.term.Term;
import nars.unify.constraint.TermMatch;

import javax.annotation.Nullable;

import static nars.Op.CONJ;
import static nars.time.Tense.DTERNAL;

public final class ConjParallel extends TermMatch {

    public static final ConjParallel the = new ConjParallel();

    private ConjParallel() {
        super();
    }

    @Override
    public boolean test(Term t) {
        Term u = t.unneg();
        if (u.op()==CONJ) {
            int i = u.dt();
            return i == 0 || i == DTERNAL;
        }
        return false;
    }

    @Override
    public boolean testSuper(Term x) {
        return x.hasAny(CONJ);
    }

    @Nullable
    @Override
    public Term param() {
        return null;
    }

    @Override
    public float cost() {
        return 0.1f;
    }
}
