package nars.term.transform;

import nars.Op;
import nars.term.Compound;
import nars.term.Term;


public abstract class VariableTransform implements CompoundTransform {

    @Override
    public Term transform(Compound t) {
        return t.hasAny(Op.varBits) ? CompoundTransform.super.transform(t) : t;
    }

}
