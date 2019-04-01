package nars.derive.op;

import nars.term.Term;
import nars.term.util.conj.Conj;
import nars.unify.constraint.TermMatch;

import javax.annotation.Nullable;

import static nars.Op.CONJ;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;

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
            return i == XTERNAL || i == 0 || (i == DTERNAL && !Conj.isSeq(u));
        }
        return false;
    }

    @Override
    public boolean testSuper(Term x) {
        return x.has(CONJ);
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
