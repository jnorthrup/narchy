package nars.term.var;

import nars.$;
import nars.IO;
import nars.Op;
import nars.The;
import nars.term.Term;
import nars.term.Variable;
import nars.term.atom.AtomicConst;

/**
 * Unnormalized, labeled variable
 */
public class UnnormalizedVariable extends AtomicConst implements Variable, The {

    private final Op type;

    @Override public int opX() { return Term.opX(op(), 10);    }

    public UnnormalizedVariable(Op type, String label) {
        super(bytes(IO.SPECIAL_OP, label));
        this.type = type;
    }


    @Override
    public final int complexity() {
        return 0;
    }

    @Override
    public float voluplexity() {
        return 0.5f;
    }

    @Override
    public boolean isNormalized() {
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
    @Override public Variable normalize(byte serial) {
        return $.v(type, serial);
    }



















}
