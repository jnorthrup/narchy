package nars.term.util.transform;

import nars.term.Compound;
import nars.term.Term;


public abstract class VariableTransform extends TermTransform.NegObliviousTermTransform {

    @Override
    public Term transform(Term x) {
        return x.hasVars() ? super.transform(x) : x;
    }

    @Override
    protected Term transformNonNegCompound(Compound x) {
        return x.hasVars() ? super.transformNonNegCompound(x) : x;
    }

}
