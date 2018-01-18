package nars.term.transform;

import nars.term.Compound;
import nars.term.Term;


public abstract class VariableTransform implements TermTransform {

    @Override
    public Term transform(Compound t) {
        return t.hasVars() ? TermTransform.super.transform(t) : t;
    }


}
