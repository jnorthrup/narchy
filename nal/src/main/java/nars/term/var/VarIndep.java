package nars.term.var;

import nars.Op;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;

import static nars.Op.VAR_INDEP;


/**
 * normalized indep var
 */
public final class VarIndep extends NormalizedVariable {

    public VarIndep(byte id) {
        super(VAR_INDEP, id);
    }

    private final static int RANK = Term.opX(VAR_INDEP, 0);
    @Override public int opX() { return RANK;    }


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
