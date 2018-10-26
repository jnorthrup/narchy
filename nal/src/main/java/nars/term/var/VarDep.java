package nars.term.var;

import nars.Op;
import nars.term.Term;

import static nars.Op.VAR_DEP;

/**
 * normalized dep var
 */
public final class VarDep extends NormalizedVariable {

    public VarDep(byte id) {
        super(VAR_DEP, id);
    }

    private final static int RANK = Term.opX(VAR_DEP, (short)0);
    @Override public int opX() { return RANK;    }


    @Override
    public Op op() {
        return VAR_DEP;
    }

    @Override
    public final int vars() {
        return 1;
    }

    @Override
    public final int varDep() {
        return 1;
    }


}
