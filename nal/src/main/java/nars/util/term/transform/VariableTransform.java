package nars.util.term.transform;

import nars.term.Compound;
import nars.term.Term;


public abstract class VariableTransform implements TermTransform.NegObliviousTermTransform {

    @Override
    public Term transformCompound(Compound t) {
        return t.hasVars() ? TermTransform.NegObliviousTermTransform.super.transformCompound((Compound)t) : t;
    }


    @Override
    public boolean eval() {
        return false;
    }
}
