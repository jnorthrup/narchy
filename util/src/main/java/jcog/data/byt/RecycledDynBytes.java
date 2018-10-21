package jcog.data.byt;

import jcog.data.pool.DequePool;

public class RecycledDynBytes extends DynBytes {

    private RecycledDynBytes(int bufferSize) {
        super(bufferSize);
    }


    static final int MAX_KEY_CAPACITY = 1024;
    //final static ThreadLocal<DequePool<byte[]>> bytesPool = DequePool.threadLocal(()->new byte[MAX_KEY_CAPACITY]);
    final static ThreadLocal<DequePool<RecycledDynBytes>> bytesPool = DequePool.threadLocal(()->
            new RecycledDynBytes(MAX_KEY_CAPACITY));

    public static RecycledDynBytes get() {
        RecycledDynBytes r = bytesPool.get().get();
        r.clear();
        return r;
    }

//    @Override
//    protected byte[] alloc(int bufferSize) {
//        //return ArrayPool.bytes().getMin(bufferSize);
//    }

    @Override
    public byte[] compact() {
        return bytes; //dont compact
    }

    @Override
    protected byte[] realloc(byte[] x, int oldLen, int newLen) {
//        byte[] y = ArrayPool.bytes().getMin(newLen);
//        System.arraycopy(x, 0, y, 0, oldLen);
//        free(x);
//        return y;

        throw new UnsupportedOperationException();

    }

    @Override
    public void close() {
//        if (bytes.length > 0) {
//            free(bytes);
//            bytes = ArrayUtils.EMPTY_BYTE_ARRAY;
//        }

        bytesPool.get().put(this);
    }


}
