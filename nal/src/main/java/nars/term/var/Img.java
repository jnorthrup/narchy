package nars.term.var;

import nars.Op;
import nars.term.Term;
import nars.term.anon.AnonID;
import nars.term.atom.Bool;
import nars.unify.Unify;

import java.io.IOException;

/** the / and \ Image operators */
public final class Img extends AnonID {

    private final String str;
    private final char symChar;
    private final int rank;
    private final byte[] bytes;

    public Img(byte sym) {
        super(Op.IMG, sym);
        this.str = String.valueOf((char) sym);
        this.symChar = (char) sym;
        this.rank = Term.opX(Op.IMG, sym);
        this.bytes = new byte[] { Op.IMG.id, sym };
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
    public boolean unify(Term y, Unify u) {
        return y == this;
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
