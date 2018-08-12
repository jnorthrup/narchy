package nars.term.util.transform;

import nars.term.Compound;
import nars.term.Term;


public abstract class VariableTransform implements TermTransform.NegObliviousTermTransform {

    @Override
    public Term transform(Term x) {
        return x.hasVars() ? TermTransform.NegObliviousTermTransform.super.transform(x) : x;
    }

    @Override
    public Term transformCompound(Compound x) {
        return x.hasVars() ? TermTransform.NegObliviousTermTransform.super.transformCompound((Compound)x) : x;
    }

}
