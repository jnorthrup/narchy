package nars.derive.op;

import nars.term.Term;
import nars.unify.op.TermMatch;

import javax.annotation.Nullable;

import static nars.Op.CONJ;
import static nars.time.Tense.DTERNAL;

public final class ConjSimultaneous extends TermMatch {

    public static final ConjSimultaneous the = new ConjSimultaneous();

    private ConjSimultaneous() {
        super();
    }

    @Override
    public boolean test(Term t) {
        Term u = t.unneg();
        if (u.op()==CONJ) {
            switch (u.dt()) {
                case 0:
                case DTERNAL:
                    return true;
            }
        }
        return false;
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
