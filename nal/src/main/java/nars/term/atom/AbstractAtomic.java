package nars.term.atom;

import jcog.Util;
import nars.Op;

import java.io.IOException;
import java.util.Arrays;

import static java.lang.System.arraycopy;

/**
 * an Atomic impl which relies on the value provided by toString()
 */
public abstract class AbstractAtomic implements Atomic {


    /*@Stable*/
    private final transient byte[] bytesCached;
    final transient int hash;

    protected AbstractAtomic(byte[] raw) {
        this.bytesCached = raw;
        this.hash = //Util.hashByteString(raw);
                    Util.hash(raw);
    }

    AbstractAtomic(Op op, String s) {
        this(bytes(op, s));
    }

    protected static byte[] bytes(Op op, String str) {
        return bytes(op.id, str);
    }

    protected static byte[] bytes(byte opID, String str) {
        return bytes(opID, str.getBytes());
    }

    public static byte[] bytes(byte opID, byte[] stringbytes) {
        int slen = stringbytes.length;

        byte[] sbytes = new byte[slen + 3];
        sbytes[0] = opID;
        sbytes[1] = (byte) (slen >> 8 & 0xff);
        sbytes[2] = (byte) (slen & 0xff);
        arraycopy(stringbytes, 0, sbytes, 3, slen);
        return sbytes;
    }


    @Override
    public final byte[] bytes() {
        return bytesCached;
    }

    @Override
    public boolean equals(Object u) {
        if (this == u) return true;
        if (u instanceof Atomic) {
            if (hashCode() == u.hashCode())
                if (Arrays.equals(bytes(), ((Atomic) u).bytes()))
                    return true;
        }
        return false;
    }

    @Override public String toString() {
        return new String(bytesCached, 3, bytesCached.length-3);
    }


    @Override
    public void appendTo(Appendable w) throws IOException {
        
        if (bytesCached.length==3+1) {
            
            w.append((char)bytesCached[3]);
        } else {
            Atomic.super.appendTo(w);
        }
    }

    @Override
    public int hashCode() {
        return hash;
    }

}
