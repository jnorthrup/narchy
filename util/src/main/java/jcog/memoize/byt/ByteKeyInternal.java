package jcog.memoize.byt;

import jcog.Texts;
import jcog.pri.PriProxy;

public final class ByteKeyInternal<Y> extends ByteKey implements PriProxy<ByteKey,Y> {

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
    public jcog.memoize.byt.ByteKeyInternal<Y> x() {
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
