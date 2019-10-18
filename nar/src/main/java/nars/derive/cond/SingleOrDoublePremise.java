package nars.derive.cond;

import nars.$;
import nars.derive.PreDerivation;
import nars.derive.util.PuncMap;
import nars.term.atom.Atomic;
import nars.term.control.AbstractPred;


public final class SingleOrDoublePremise extends AbstractPred<PreDerivation> {

    private static final Atomic S = Atomic.atom("SinglePremise");
    private static final Atomic D = Atomic.atom("DoublePremise");

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
