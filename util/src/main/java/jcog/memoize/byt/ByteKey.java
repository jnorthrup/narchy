package jcog.memoize.byt;

import jcog.Texts;
import jcog.data.byt.DynBytes;
import jcog.pri.PriProxy;
import jcog.pri.UnitPri;

import java.util.Arrays;

abstract public class ByteKey extends UnitPri {


    protected ByteKey() {
        super();
    }

    /** of size equal or greater than length() */
    abstract public byte[] array();

    abstract public int length();

    @Override
    public final boolean equals(Object o) {
        ByteKey that = (ByteKey) o;
        if (hashCode() == that.hashCode()) {
            return equalsBytes(that);
        }
        return false;
    }

    protected final boolean equalsBytes(ByteKey that) {
        int l = length();
        return that.length() == l && Arrays.equals(array(), 0, l, that.array(), 0, l);
    }


    @Override
    abstract public int hashCode();

    @Override
    public String toString() {
        return Texts.i(array(),0, length(), 16) + " [" + Integer.toUnsignedString(hashCode(),32) + "]";
    }



    public final static class ByteKeyInternal<Y> extends ByteKey implements PriProxy<ByteKey,Y> {

        final Y result;

        /*@Stable*/
        public final byte[] key;
        private final int hash;

        protected ByteKeyInternal(byte[] key, int hash, Y result, float pri) {
            super();
            this.hash = hash;
            this.key = key;
            this.result = result;
            pri(pri);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean xEquals(Object y, int kHash) {
            return hash == kHash && equalsBytes((ByteKey)y);
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

        public final DynBytes key;

        protected int hash;

        public ByteKeyExternal(DynBytes key) {
            super();
            this.key = key;
        }

        protected void commit() {
            //TODO optional compression

            hash = key.hashCode();
        }

        public final void close() {
            key.close();
        }

        @Override
        public int hashCode() {
            return hash;
        }

        public <Y> ByteKeyInternal<Y> internal(Y y, float pri) {
            byte[] b =
                    key.arrayCopy();
                    //key instanceof RecycledDynBytes ? key.arrayCopy() : key.compact();

            return new ByteKeyInternal<>(b, hash, y, pri);
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
