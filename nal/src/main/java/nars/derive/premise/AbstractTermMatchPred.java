package nars.derive.premise;

import nars.term.Term;
import nars.term.control.AbstractPred;

import java.util.function.Function;

public abstract class AbstractTermMatchPred<X> extends AbstractPred<X> {
    public final Function<X, Term> resolve;
    protected final float resolveCost;

    public AbstractTermMatchPred(Term term, Function/*<X, Term>*/ resolve, float resolveCost) {
        super(term);
        this.resolve = resolve;
        this.resolveCost = resolveCost;
    }

    static float cost(int pathLen) {
        return pathLen * 0.01f;
    }


    @Override
    public final boolean test(X x) {
        Term y = resolve.apply(x);
        return y!=null && match(y);
    }

    protected abstract boolean match(Term y);


}
