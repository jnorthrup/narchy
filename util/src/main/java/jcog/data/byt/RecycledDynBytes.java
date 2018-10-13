package jcog.data.byt;

import jcog.data.pool.ArrayPool;

public class RecycledDynBytes extends DynBytes {

    public RecycledDynBytes(int bufferSize) {
        super(bufferSize);
    }


    @Override public final byte[] arrayCopyClose() {
        byte[] b = arrayCopy();
        close();
        return b;
    }

    @Override
    protected byte[] alloc(int bufferSize) {
        return ArrayPool.bytes().getMin(bufferSize);
    }

    @Override
    protected byte[] realloc(byte[] x, int oldLen, int newLen) {
        byte[] y = ArrayPool.bytes().getMin(newLen);
        System.arraycopy(x, 0, y, 0, oldLen);
        return y;
    }

    @Override
    protected void free(byte[] b) {
        ArrayPool.bytes().put(b);
    }
}
