package nars.term.control;

import nars.term.Term;

import java.util.function.Function;

public abstract class AbstractTermMatchPred<X> extends AbstractPred<X> {
    public final Function<X, Term> resolve;
    final float resolveCost;

    AbstractTermMatchPred(Term term, Function/*<X, Term>*/ resolve, float resolveCost) {
        super(term);
        this.resolve = resolve;
        this.resolveCost = resolveCost;
    }

    public static float cost(int pathLen) {
        return (float) pathLen * 0.01f;
    }


    @Override
    public final boolean test(X x) {
        Term y = resolve.apply(x);
        return y!=null && match(y);
    }

    protected abstract boolean match(Term y);


}
