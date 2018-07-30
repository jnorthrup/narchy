package jcog.signal.tensor;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * disallows non-batch accessor methods
 */
abstract public class BatchArrayTensor extends ArrayTensor {

    final AtomicBoolean busy = new AtomicBoolean(false);

    public BatchArrayTensor(int[] shape) {
        super(shape);
    }

    @Override
    public void set(@NotNull float[] raw) {
        throw new UnsupportedOperationException("only batch operations available");
    }

    @Override
    public void set(@NotNull double[] d) {
        throw new UnsupportedOperationException("only batch operations available");
    }

    @Override
    public void set(float v, int cell) {
        throw new UnsupportedOperationException("only batch operations available");
    }






    @Override public float[] get() {
        
        if (busy.compareAndSet(false, true)) {
            try {
                update();
            } finally {
                busy.set(false);
            }
        }
        return data;
    }

    /** triggers the update */
    public abstract void update();
}
