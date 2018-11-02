package nars.term.atom;

import com.google.common.io.ByteArrayDataOutput;
import jcog.Util;
import nars.Op;

import java.io.IOException;

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
        this.hash = Util.hashByteString(raw);
    }

    AbstractAtomic(Op op, String s) {
        this(bytes(op, s));
    }

    protected static byte[] bytes(Op op, String str) {
        return bytes(op.id, str);
    }

    protected static byte[] bytes(byte opID, String str) {
        
        

        byte[] stringbytes = str.getBytes();
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
        return Atomic.equals(this, u);
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
    public float voluplexity() {
        return 1;
    }

    @Override
    public int hashCode() {
        return hash;
    }

}
