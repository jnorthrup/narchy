package nars.util.term.transform;

import nars.term.Compound;
import nars.term.Term;


public abstract class VariableTransform implements TermTransform {

    @Override
    public Term transform(Term x) {
        return x.hasVars() ? TermTransform.super.transform(x) : x;
    }

    @Override
    public Term transformCompound(Compound x) {
        return x.hasVars() ? TermTransform.super.transformCompound((Compound)x) : x;
    }

    @Override
    public boolean eval() {
        return false;
    }
}
