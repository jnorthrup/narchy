package jcog.memoize.byt;

import jcog.data.byt.DynBytes;

import static jcog.data.byt.RecycledDynBytes.tmpKey;

public class ByteKeyExternal extends ByteKey {

    public final DynBytes key;

    protected int hash;

    public ByteKeyExternal() {
        this(tmpKey());
    }

    public ByteKeyExternal(DynBytes key) {
        super();
        this.key = key;
    }

    protected void commit() {
        //TODO optional compression

        hash = key.hashCode();
    }

    public void close() {
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
