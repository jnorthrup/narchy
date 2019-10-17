package nars.unify.instrument;

import nars.derive.Derivation;
import nars.term.Term;
import nars.term.control.AbstractPred;
import nars.term.control.PREDICATE;

public abstract class InstrumentedDerivationPredicate extends AbstractPred<Derivation> {

    protected InstrumentedDerivationPredicate( PREDICATE<Derivation> inner) {
        super(inner);
    }

    @Override public boolean test(Derivation derivation) {

        PREDICATE p;
        if (ref instanceof PREDICATE) {
            p = (PREDICATE) ref;
        } else {

            Term s0 = sub(0);
            if (s0 instanceof PREDICATE)
                p = (PREDICATE) s0;
            else {
                throw new UnsupportedOperationException();
                
            }
        }

        onEnter(p, derivation);
        Throwable thrown = null;
        boolean result = false;
        long start = System.nanoTime();
        try {
            result = p.test(derivation);
        } catch (Throwable e) {
            thrown = e;
        }
        long end = System.nanoTime();
        onExit(p, derivation, result, thrown,end - start);
        return result;
    }


    protected abstract void onEnter(PREDICATE<Derivation> p, Derivation d);

    protected abstract void onExit(PREDICATE<Derivation> p, Derivation d, boolean returnValue, Throwable thrown, long nanos);

}
