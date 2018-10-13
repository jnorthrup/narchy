package jcog.data.byt;

import jcog.data.pool.ArrayPool;
import org.apache.commons.lang3.ArrayUtils;

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
        free(x);
        return y;
    }

    @Override
    public void close() {
        if (bytes.length > 0) {
            free(bytes);
            bytes = ArrayUtils.EMPTY_BYTE_ARRAY;
        }
    }

    @Override
    protected void free(byte[] b) {
        if (bytes.length > 0) {
            ArrayPool.bytes().put(b);
        }
    }
}
