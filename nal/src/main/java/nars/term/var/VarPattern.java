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

    final static int RANK = Term.opX(VAR_PATTERN, 0);
    @Override public int opX() { return RANK;    }


    /** special case: pattern variables contribute no structure currently */
    @Override public final int structure() {
        return 0;
    }


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
        return 0;
    }

    @Override
    public final int varDep() {
        return 0;
    }

    @Override
    public final int varIndep() {
        return 0;
    }

    @Override
    public final int varQuery() {
        return 0;
    }

    @Override
    public final int varPattern() {
        return 1;
    }


}
