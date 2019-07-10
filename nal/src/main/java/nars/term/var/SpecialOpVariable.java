package nars.term.var;

import nars.Op;
import nars.term.Variable;

public final class SpecialOpVariable extends UnnormalizedVariable {

    private final Variable v;

    public SpecialOpVariable(Variable v, Op overridingType) {
        super(overridingType,
                overridingType.ch + v.toString() //TODO byte[] optimize
        );
        assert(v.op()!=overridingType);
        this.v = v;
    }

    @Override
    public String toString() {
        return op().ch + v.toString();
    }
}
