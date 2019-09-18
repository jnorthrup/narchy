package nars.derive.op;

import nars.$;
import nars.derive.PreDerivation;
import nars.term.atom.Atomic;
import nars.term.control.AbstractPred;


public class SingleOrDoublePremise extends AbstractPred<PreDerivation> {

    final static Atomic D = Atomic.atom("DoublePremise");
    final static Atomic S = Atomic.atom("SinglePremise");

    private final PuncMap requires;
    private final boolean singleOrDouble;

    public SingleOrDoublePremise(boolean singleOrDouble) {
        this(PuncMap.All, singleOrDouble);
    }

    public SingleOrDoublePremise(PuncMap requires, boolean singleOrDouble) {
        super($.func(singleOrDouble ? S : D, requires));
        this.requires = requires;
        this.singleOrDouble = singleOrDouble;
    }

    @Override
    public boolean test(PreDerivation d) {
        return !requires.test(d) || singleOrDouble == !d.hasBeliefTruth();
    }

    @Override
    public float cost() {
        return 0.005f;
    }
}
