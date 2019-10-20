package jcog.data.byt;

import jcog.data.pool.MetalPool;

public class RecycledDynBytes extends DynBytes {

    private RecycledDynBytes(int bufferSize) {
        super(bufferSize);
    }

    //private transient MetalPool pool = null;

    static final int MAX_KEY_CAPACITY = 8192;
    //final static ThreadLocal<DequePool<byte[]>> bytesPool = DequePool.threadLocal(()->new byte[MAX_KEY_CAPACITY]);
    static final ThreadLocal<MetalPool<RecycledDynBytes>> bytesPool = MetalPool.threadLocal(()->
            new UnsafeRecycledDynBytes(
            //new RecycledDynBytes(
                    MAX_KEY_CAPACITY));

    public static RecycledDynBytes get() {
        MetalPool<RecycledDynBytes> pool = bytesPool.get();

        RecycledDynBytes r = pool.get();
        //assert(r.pool == null);
        //r.pool = pool;
        return r;
    }

    public static class UnsafeRecycledDynBytes extends RecycledDynBytes {

        private UnsafeRecycledDynBytes(int bufferSize) {
            super(bufferSize);
        }

        @Override
        protected int ensureSized(int extra) {
            return len;
        }
    }


    @Override
    public byte[] compact() {
        return bytes; //dont compact
    }

    @Override
    protected byte[] realloc(byte[] x, int oldLen, int newLen) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() {
        //MetalPool p = pool;
        //if (p==null) throw new WTF("already closed");

        clear();
        //this.pool = null; //not necessary since threadlocal
        bytesPool.get().put(this);
    }


}
