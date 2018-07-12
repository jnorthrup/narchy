package jcog.memoize.byt;

import jcog.Texts;
import jcog.data.byt.DynBytes;
import jcog.pri.Pri;
import jcog.pri.PriProxy;

import java.util.Arrays;

public class ByteKey extends Pri {

    /*@Stable*/
    protected final byte[] key;
    protected final int hash;

    public ByteKey(byte[] key, int hash) {
        this.key = key;
        this.hash = hash;
    }

    public ByteKey(DynBytes key) {
        this(
                //key.arrayDeflate(),
                key.array(),
                key.hashCode());
    }

    @Override
    public final boolean equals(Object o) {
        ByteKey that = (ByteKey) o;

        return hash == that.hash && Arrays.equals(key, that.key);
    }

    @Override
    public final int hashCode() {
        return hash;
    }

    @Override
    public String toString() {

        return Texts.i(key,16) + " [" + Integer.toUnsignedString(hash,32) + "]";
    }

    static class ByteKeyInternal<Y> extends ByteKey implements PriProxy<ByteKey,Y> {

        final Y result;

        public ByteKeyInternal(byte[] key, int hash, Y result, float pri) {
            super(key, hash);
            this.result = result;
            pri(pri);
        }

        @Override
        public ByteKey.ByteKeyInternal<Y> x() {
            return this;
        }

        @Override
        public Y get() {
            return result;
        }



        @Override
        public String toString() {
            return result + " = $" + Texts.n4(pri()) + " " + super.toString();
        }
    }

}
