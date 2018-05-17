package nars.term.atom;

import com.google.common.io.ByteArrayDataOutput;
import jcog.Util;
import nars.Op;

import java.io.IOException;

import static java.lang.System.arraycopy;

/**
 * an Atomic impl which relies on the value provided by toString()
 */
public abstract class AtomicConst implements Atomic {


    /*@Stable*/
    final transient byte[] bytesCached;
    protected final transient int hash;

    protected AtomicConst(byte[] raw) {
        this.bytesCached = raw;
        this.hash = (int) Util.hashELF(raw, 1); //Util.hashWangJenkins(s.hashCode());
    }

    protected AtomicConst(Op op, String s) {
        this(bytes(op, s));
    }

    protected static byte[] bytes(Op op, String str) {
        return bytes(op.id, str);
    }

    protected static byte[] bytes(byte opID, String str) {
        //if (s == null) s = toString(); //must be a constant method
        //int slen = str.length(); //TODO will this work for UTF-16 containing strings?

        byte[] stringbytes = str.getBytes();
        int slen = stringbytes.length;

        byte[] sbytes = new byte[slen + 3];
        sbytes[0] = opID; //(op != null ? op : op()).id;
        sbytes[1] = (byte) (slen >> 8 & 0xff);
        sbytes[2] = (byte) (slen & 0xff);
        arraycopy(stringbytes, 0, sbytes, 3, slen);
        return sbytes;
    }

    @Override
    public void append(ByteArrayDataOutput out) {
        out.write(bytesCached);
    }

    @Override
    public final byte[] bytes() {
        return bytesCached;
    }

    @Override
    public boolean equals(Object u) {
        return Atomic.equals(this, u);
    }

    @Override public String toString() {
        return new String(bytesCached, 3, bytesCached.length-3);
    }


    @Override
    public void append(Appendable w) throws IOException {
        //TODO 2-char special case
        if (bytesCached.length==3+1) {
            //special case single char ASCII
            w.append((char)bytesCached[3]);
        } else {
            Atomic.super.append(w);
        }
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
