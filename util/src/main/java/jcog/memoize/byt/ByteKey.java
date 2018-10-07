package jcog.memoize.byt;

import jcog.Texts;
import jcog.data.byt.DynBytes;
import jcog.pri.PriProxy;
import jcog.pri.UnitPri;

import java.util.Arrays;

abstract public class ByteKey extends UnitPri {

    protected final int hash;

    protected ByteKey(int hash) {
        super();
        this.hash = hash;
    }

    /** of size equal or greater than length() */
    abstract public byte[] array();

    abstract public int length();

    @Override
    public final boolean equals(Object o) {
        ByteKey that = (ByteKey) o;
        if (hash == that.hash) {
            int l = length();
            if (that.length() == l) {
                return Arrays.equals(array(), 0, l, that.array(), 0, l);
            }
        }
        return false;
    }



    @Override
    public final int hashCode() {
        return hash;
    }

    @Override
    public String toString() {

        return Texts.i(array(),0, length(), 16) + " [" + Integer.toUnsignedString(hash,32) + "]";
    }



    public final static class ByteKeyInternal<Y> extends ByteKey implements PriProxy<ByteKey,Y> {

        final Y result;

        /*@Stable*/
        public final byte[] key;



        protected ByteKeyInternal(byte[] key, int hash, Y result, float pri) {
            super(hash);
            this.key = key;
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
        public byte[] array() {
            return key;
        }

        @Override
        public int length() {
            return key.length;
        }

        @Override
        public String toString() {
            return result + " = $" + Texts.n4(pri()) + " " + super.toString();
        }
    }

    public static class ByteKeyExternal extends ByteKey {

        private final DynBytes key;


        public ByteKeyExternal(DynBytes key) {
            super(key.hashCode());
            this.key = key;
        }

        public final  <Y> PriProxy<?,Y> internal(Y result, float pri) {
            return internal(result, pri, true);
        }

        private <Y> PriProxy<?,Y> internal(Y result, float pri, boolean forceNew) {
            byte[] b = array();
            int l = length();
            if (!forceNew && l == b.length) {
                //keep
            } else {
                b = Arrays.copyOf(b, l);
            }

            return new ByteKeyInternal<>(b, hash, result, pri);
        }

        @Override
        public final byte[] array() {
            return key.arrayDirect();
        }

        @Override
        public final int length() {
            return key.length();
        }
    }

}
