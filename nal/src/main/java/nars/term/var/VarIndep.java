package nars.term.var;

import nars.Op;

import static nars.Op.VAR_INDEP;


/**
 * normalized indep var
 */
public final class VarIndep extends NormalizedVariable {

    VarIndep(byte id) {
        super(VAR_INDEP, id);
    }

    @Override
    public final Op op() {
        return VAR_INDEP;
    }

    @Override
    public final int vars() {
        return 1;
    }

    @Override
    public final int varIndep() {
        return 1;
    }


}
