package nars.util.term.transform;

import nars.term.Compound;
import nars.term.Term;


public abstract class VariableTransform implements TermTransform.NegObliviousTermTransform {

    @Override
    public final Term transformCompound(Compound t) {
        return hasVars(t) ? TermTransform.NegObliviousTermTransform.super.transformCompound((Compound)t) : t;
    }

    protected boolean hasVars(Compound t) {
        return t.hasVars();
    }

    @Override
    public boolean eval() {
        return false;
    }
}
