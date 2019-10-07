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

    private final int op;

//    public UnnormalizedVariable(Op type, byte[] label) {
//        super(IO.SPECIAL_BYTE, label);
//        this.type = type.id;
//    }

    public UnnormalizedVariable(Op op, String label) {
        super(IO.SPECIAL_BYTE, label);
        this.op = op.id;
    }

    @Override
    public final boolean isNormalized() {
        return false;
    }

    @Override
    @Deprecated public final Op op() {
        return Op.ops[op];
    }

    @Override
    public int opID() {
        return op;
    }

    @Override
    public final int varIndep() {
        return op == Op.VAR_INDEP.id ? 1 : 0;
    }

    @Override
    public final int varDep() {
        return op == Op.VAR_DEP.id ? 1 : 0;
    }

    @Override
    public final int varQuery() {
        return op == Op.VAR_QUERY.id ? 1 : 0;
    }

    @Override
    public final int varPattern() {
        return op == Op.VAR_PATTERN.id ? 1 : 0;
    }

    @Override public final int complexity() {
        return 0;
    }

    @Override
    public float voluplexity() {
        return 0.5f;
    }

    @Override
    public final int vars() {
        return 1;
    }

    /** produce a normalized version of this identified by the serial integer
     * @param serial*/
    @Override public Variable normalizedVariable(byte serial) {
        return $.v(op(), serial);
    }



















}
