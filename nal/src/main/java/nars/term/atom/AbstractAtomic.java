package nars.term.atom;

import jcog.Util;
import nars.Op;

import java.io.IOException;
import java.util.Arrays;

import static java.lang.System.arraycopy;

/**
 * an Atomic impl which relies on the value provided by toString()
 */
public abstract class AbstractAtomic extends Atomic {


    /*@Stable*/
    private final transient byte[] bytes;
    private final transient int hash;

    protected AbstractAtomic(byte[] raw) {
        this.bytes = raw;
        this.hash = Util.hash(raw);
    }

    protected AbstractAtomic(Op op, String s) {
        this(op.id, s);
    }

    protected AbstractAtomic(Op op, byte[] s) {
        this(op.id, s);
    }
    protected AbstractAtomic(byte opID, String s) {
        this(opID, s.getBytes());
    }
    protected AbstractAtomic(byte opID, byte[] s) {
        this(bytes(opID, s));
    }

    public static byte[] bytes(byte opID, String str) {
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
        return bytes;
    }

    @Override
    public boolean equals(Object u) {
        if (this == u) return true;
        return (u instanceof Atomic) &&
            (hash == u.hashCode()) &&
                Arrays.equals(bytes, ((Atomic) u).bytes());
    }

    @Override public String toString() {
        return new String(bytes, 3, bytes.length-3);
    }


    @Override
    public void appendTo(Appendable w) throws IOException {
        
        if (bytes.length==3+1) {
            
            w.append((char) bytes[3]);
        } else {
            super.appendTo(w);
        }
    }

    @Override
    public final int hashCode() {
        return hash;
    }

}
