package nars.term.atom;

import jcog.Util;
import nars.Op;
import nars.term.sub.TermMetadata;

import static java.lang.System.arraycopy;

/**
 * an Atomic impl which relies on the value provided by toString()
 */
public abstract class AtomicConst implements Atomic {


    public final transient byte[] bytesCached;
    protected final transient int hash;

    protected AtomicConst(byte[] raw) {
        this.bytesCached = raw;
        this.hash = (int) Util.hashELF(raw, 1); //Util.hashWangJenkins(s.hashCode());
    }

    protected AtomicConst(Op op, String s) {
        this(bytes(op, s));
    }

    static byte[] bytes(Op op, String str) {
        //if (s == null) s = toString(); //must be a constant method
        //int slen = str.length(); //TODO will this work for UTF-16 containing strings?

        byte[] stringbytes = str.getBytes();
        int slen = stringbytes.length;

        byte[] sbytes = new byte[slen + 3];
        sbytes[0] = op.id; //(op != null ? op : op()).id;
        sbytes[1] = (byte) (slen >> 8 & 0xff);
        sbytes[2] = (byte) (slen & 0xff);
        arraycopy(stringbytes, 0, sbytes, 3, slen);
        return sbytes;
    }

    public static byte[] bytes(Op op, byte c) {
        //if (s == null) s = toString(); //must be a constant method
        //int slen = str.length(); //TODO will this work for UTF-16 containing strings?

        byte[] sbytes = new byte[4];
        sbytes[0] = op.id; //(op != null ? op : op()).id;
        sbytes[1] = 0;
        sbytes[2] = 1;
        sbytes[3] = c;
        return sbytes;
    }


    @Override
    public final byte[] bytes() {
        return bytesCached;
    }

    @Override
    public boolean equals(Object u) {
        return (this == u) ||
               ((u instanceof Atomic) && Atomic.equals(this, (Atomic)u));
    }

    @Override public String toString() {
        return new String(bytesCached, 3, bytesCached.length-3);
    }


    @Override
    public int complexity() {
        return 1;
    }

    @Override
    public float voluplexity() {
        return 1;
    }

    @Override
    public int hashCode() {
        return hash;
    }

}
