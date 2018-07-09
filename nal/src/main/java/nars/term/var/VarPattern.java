package nars.term.var;

import nars.Op;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;

import static nars.Op.VAR_PATTERN;

/**
 * normalized pattern variable
 */
public class VarPattern extends NormalizedVariable {

    public VarPattern(byte id) {
        super(VAR_PATTERN, id);
    }

    private final static int RANK = Term.opX(VAR_PATTERN, 0);
    @Override public int opX() { return RANK;    }


    @NotNull
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
