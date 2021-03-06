package jcog.memoize.byt;

import jcog.Texts;
import jcog.pri.PriProxy;
import jcog.pri.UnitPri;

public final class ByteKeyInternal<Y> extends UnitPri implements ByteKey, PriProxy<ByteKey,Y> {

    final Y result;

    /*@Stable*/
    public final byte[] key;
    private final int hash;

    protected ByteKeyInternal(byte[] key, int hash, Y result, float pri) {
        super(pri);
        this.hash = hash;
        this.key = key;
        this.result = result;
    }

    @Override
    public final int hashCode() {
        return hash;
    }

    @Override
    public final boolean equals(Object obj) {
        return ByteKey.equals(this, (ByteKey) obj);
    }

    @Override
    public final ByteKeyInternal<Y> x() {
        return this;
    }

    @Override
    public Y get() {
        return result;
    }

    @Override
    public final byte[] array() {
        return key;
    }

    @Override
    public final int length() {
        return key.length;
    }

    @Override
    public String toString() {
        return result + " = $" + Texts.INSTANCE.n4(pri()) + ' ' + super.toString();
    }
}
