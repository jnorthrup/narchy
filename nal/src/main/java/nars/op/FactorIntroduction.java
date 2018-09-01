package nars.op;

import nars.NAR;
import nars.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.time.Tense;
import org.jetbrains.annotations.Nullable;

import static nars.Op.CONJ;

public class FactorIntroduction extends Introduction {

    public FactorIntroduction(int capacity, NAR nar) {
        super(capacity, nar);
    }

    @Override
    protected boolean preFilter(Task next) {
        return next.op()==CONJ && Tense.dtSpecial(next.dt()) && next.term().subterms().subs(x->x instanceof Compound) > 1;
    }

    @Override
    protected @Nullable Term newTerm(Task x) {
        Term xx = x.term();
        Term y = Factorize.applyAndNormalize(xx);
        return y != xx ? y : null;
    }
}
