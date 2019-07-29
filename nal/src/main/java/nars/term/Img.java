package nars.term;

import nars.Op;
import nars.term.anon.IntrinAtomic;
import nars.unify.Unify;

import java.io.IOException;

/** the / and \ Image operators */
public final class Img extends IntrinAtomic  {

    private final String str;
    private final char symChar;
    private final byte[] bytes;

    public Img(byte sym) {
        super(Op.IMG, sym);
        this.str = String.valueOf((char) sym);
        this.symChar = (char) sym;
        this.bytes = new byte[] { Op.IMG.id, sym };
    }

    @Override
    public Term concept() {
        throw new UnsupportedOperationException();
    }


    @Override
    public Op op() {
        return Op.IMG;
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
