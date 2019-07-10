package nars.term.var;

import nars.$;
import nars.Op;
import nars.The;
import nars.io.IO;
import nars.term.Variable;
import nars.term.atom.AbstractAtomic;

/**
 * Unnormalized, labeled variable
 */
public class UnnormalizedVariable extends AbstractAtomic implements Variable, The {

    private final Op type;

    public UnnormalizedVariable(Op type, byte[] label) {
        super(IO.SPECIAL_BYTE, label);
        this.type = type;
    }

    public UnnormalizedVariable(Op type, String label) {
        super(IO.SPECIAL_BYTE, label);
        this.type = type;
    }

    @Override
    public final boolean isNormalized() {
        return false;
    }

    @Override
    public final Op op() {
        return type;
    }


    @Override
    public final int varIndep() {
        return type == Op.VAR_INDEP ? 1 : 0;
    }

    @Override
    public final int varDep() {
        return type == Op.VAR_DEP ? 1 : 0;
    }

    @Override
    public final int varQuery() {
        return type == Op.VAR_QUERY ? 1 : 0;
    }

    @Override
    public final int varPattern() {
        return type == Op.VAR_PATTERN ? 1 : 0;
    }

    @Override
    public final int vars() {
        return 1;
    }

    /** produce a normalized version of this identified by the serial integer
     * @param serial*/
    @Override public Variable normalizedVariable(byte serial) {
        return $.v(type, serial);
    }



















}
