package nars.term.var;

import nars.Op;

import static nars.Op.VAR_PATTERN;

/**
 * normalized pattern variable
 */
public class VarPattern extends NormalizedVariable {

    VarPattern(byte id) {
        super(VAR_PATTERN, id);
    }

    @Override
    public final Op op() {
        return VAR_PATTERN;
    }


    /**
     * pattern variable hidden in the count 0
     */
    @Override
    public final int vars() {
        return 1;
    }

    @Override
    public final int varPattern() {
        return 1;
    }


}
