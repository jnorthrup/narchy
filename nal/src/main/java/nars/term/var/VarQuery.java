package nars.term.var;

import nars.Op;

import static nars.Op.VAR_QUERY;

/**
 * normalized query variable
 */
public final class VarQuery extends NormalizedVariable {

    VarQuery(byte id) {
        super(VAR_QUERY, id);
    }

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
