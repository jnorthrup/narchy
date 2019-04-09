package nars.term.var;

import nars.Op;
import nars.term.Term;

import static nars.Op.VAR_QUERY;

/**
 * normalized query variable
 */
public final class VarQuery extends NormalizedVariable {

    public VarQuery(byte id) {
        super(VAR_QUERY, id);
    }

    private final static int RANK = Term.opX(VAR_QUERY, 0);
    @Override public int opX() { return RANK;    }

    @Override
    public final Op op() {
        return VAR_QUERY;
    }

    @Override
    public final int vars() {
        return 1;
    }

    @Override
    public final int varQuery() {
        return 1;
    }

}
