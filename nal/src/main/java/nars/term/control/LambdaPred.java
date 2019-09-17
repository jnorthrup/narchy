package nars.term.control;

import nars.$;
import nars.term.Term;

import java.util.function.Predicate;


public final class LambdaPred<X> extends AbstractPred<X> {

    private final Predicate<X> test;

    public LambdaPred(Predicate<X> p) {
        this($.identity(p), p);
    }

    public LambdaPred(Term term, Predicate<X> p) {
        super(term);
        this.test = p;
    }

    @Override
    public boolean test(X x) {
        return test.test(x);
    }
}
