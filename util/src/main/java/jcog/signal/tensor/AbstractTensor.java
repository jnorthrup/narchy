package jcog.signal.tensor;

import jcog.signal.Tensor;

import java.io.Serializable;

public abstract class AbstractTensor implements Tensor, TensorFrom, TensorTo, Serializable {
    public static final Tensor Zero = new ArrayTensor(0);

    public void fill(float x) {
        int v = volume();
        for (int i = 0; i < v; i++)
            setAt(x, i);
    }

    @Override
    public abstract int[] stride();

    @Override
    public abstract int[] shape();

    @Override
    public float get(int... cell) {
        return getAt(index(cell));
    }

    @Override
    public abstract float getAt(int linearCell);

    @Override
    public abstract void setAt(float newValue, int linearCell);

    @Override
    public void set(float newValue, int... cell) {
        setAt(newValue, index(cell));
    }

    abstract public float addAt(float x, int linearCell);
}
