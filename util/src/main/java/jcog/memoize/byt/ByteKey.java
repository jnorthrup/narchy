package jcog.memoize.byt;

import jcog.data.byt.DynBytes;
import jcog.pri.PriProxy;
import jcog.pri.Priority;

import java.util.Arrays;

public class ByteKey {

    /*@Stable*/
    protected final byte[] key;
    protected final int hash;

    public ByteKey(byte[] key, int hash) {
        this.key = key;
        this.hash = hash;
    }

    public ByteKey(DynBytes key) {
        this(key.array(), key.hashCode());
    }

    @Override
    public final boolean equals(Object o) {
        ByteKey that = (ByteKey) o;

        return hash == that.hash && Arrays.equals(key, that.key);
    }

    protected final ByteKeyInternal intern(Object result, float pri) {
        return new ByteKeyInternal<>(key, hash, result, pri);
    }

    @Override
    public final int hashCode() {
        return hash;
    }

    static class ByteKeyInternal<Y> extends ByteKey implements Priority, PriProxy<ByteKey,Y> {

        final Y result;
        private volatile float pri;

        public ByteKeyInternal(byte[] key, int hash, Y result, float pri) {
            super(key, hash);
            this.result = result;
            this.pri = pri;
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
        public float priSet(float p) {
            return pri = p;
        }

        @Override
        public float pri() {
            return pri;
        }
    }

}
