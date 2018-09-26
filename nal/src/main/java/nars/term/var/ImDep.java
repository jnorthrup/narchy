package nars.term.var;

import nars.Op;
import nars.term.Term;
import nars.term.anon.AnonID;
import nars.term.atom.Bool;
import nars.unify.Unify;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

import static nars.Op.VAR_DEP;

/** the / and \ Image operators */
public final class ImDep extends AnonID {

    private final String str;
    private final char symChar;
    private final int rank;
    private final byte[] bytes;

    public ImDep(byte id, byte sym) {
        super(VAR_DEP /* HACK */, id);
        this.str = String.valueOf((char) sym);
        this.symChar = (char) sym;
        this.rank = Term.opX(Op.IMG, (short) id);
        this.bytes = new byte[] { Op.IMG.id, id };;
    }

    @Override
    public Term concept() {
        return Bool.Null;
    }

    @Override
    public Op op() {
        return Op.IMG;
    }

    @Override
    public int opX() {
        return rank;
    }

    @Override
    public @Nullable NormalizedVariable normalize(byte vid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean unify(Term y, Unify u) {
        return y == this;
    }

    @Override
    public boolean unifyReverse(Term x, Unify u) {
        return false;
    }

    @Override
    public final void appendTo(Appendable w) throws IOException {
        w.append(symChar);
    }

    @Override
    public final String toString() {
        return str;
    }

    @Override
    public byte[] bytes() {
        return bytes;
    }
}
