package jcog.signal.tensor;

import com.google.common.util.concurrent.AtomicDoubleArray;

public class AtomicDoubleArrayTensor extends AbstractMutableTensor {

    private final AtomicDoubleArray data;

    public AtomicDoubleArrayTensor(int linearShape) {
        this(new int[] { linearShape });
    }

    public AtomicDoubleArrayTensor(int[] shape) {
        super(shape);
        this.data = new AtomicDoubleArray(super.volume());
    }
    @Override
    public int volume() {
        return data.length();
    }
    @Override
    public float getAt(int linearCell) {
        return (float) data.get(linearCell);
    }

    @Override
    public void setAt(int linearCell, float newValue) {
        data.set(linearCell, (double) newValue);
    }

    @Override
    public float addAt(float x, int linearCell) {
        return (float) data.addAndGet(linearCell, (double) x);
    }
}
