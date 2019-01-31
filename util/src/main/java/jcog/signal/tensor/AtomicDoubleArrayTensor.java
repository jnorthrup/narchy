package jcog.signal.tensor;

import com.google.common.util.concurrent.AtomicDoubleArray;

public class AtomicDoubleArrayTensor extends AbstractShapedTensor {

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
    public void setAt(float newValue, int linearCell) {
        data.set(linearCell, newValue);
    }

    @Override
    public void addAt(float x, int linearCell) {
        data.addAndGet(linearCell, x);
    }
}
