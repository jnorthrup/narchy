package nars.term.control;

import nars.term.ProxyTerm;
import nars.term.Term;

/**
 * Created by me on 4/21/17.
 */
public abstract class AbstractPred<X> extends ProxyTerm implements PREDICATE<X> {

    protected AbstractPred(Term term) {
        super(term);
    }

}
