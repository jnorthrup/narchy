package nars.term.pred;

import nars.$;
import nars.term.ProxyTerm;
import nars.term.Term;

/**
 * Created by me on 4/21/17.
 */
public abstract class AbstractPred<X> extends ProxyTerm<Term> implements PrediTerm<X> {

    protected AbstractPred(String x) {
        this($.$safe(x));
    }

    protected AbstractPred(Term term) {
        super(term);
    }

}
