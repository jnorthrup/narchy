package nars.term.var;

import nars.$;
import nars.Op;
import nars.term.Term;
import nars.term.Variable;
import nars.term.atom.AtomicConst;
import org.jetbrains.annotations.NotNull;

/**
 * Unnormalized, labeled variable
 */
public class UnnormalizedVariable extends AtomicConst implements Variable {

    public final Op type;

    @Override public int opX() { return Term.opX(op(), 10);    }

    public UnnormalizedVariable(/*@NotNull*/ Op type, String label) {
        super(type, label);
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

    @NotNull
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
        // pattern variable hidden in the count 0
        return 1;
    }

    /** produce a normalized version of this identified by the serial integer
     * @param serial*/
    @Override public Variable normalize(byte serial) {
        return $.v(type, serial);
    }


//    @Override
//    public Term evalSafe(TermContext index, int remain) {
//        throw new UnsupportedOperationException();
//    }

//    @Override
//    public void append(ByteArrayDataOutput out) {
//        out.writeByte(SPECIAL_OP);
//        String s = toString();
//        out.writeShort(s.length());
//        out.write(s.getBytes()); //HACK
//        //byte[] b = bytes();
//        //out.writeShort(b.length);
//
//    }


}
