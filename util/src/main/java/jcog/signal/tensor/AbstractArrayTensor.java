package jcog.signal.tensor;

import jcog.signal.Tensor;
import jcog.util.ArrayUtils;

import java.io.Serializable;

public abstract class AbstractArrayTensor implements Tensor, TensorFrom, TensorTo, Serializable {
    public static final Tensor Zero = new ArrayTensor(0);
    public final int[] shape;
    protected transient final int[] stride;

    protected AbstractArrayTensor(int[] shape) {

        if (shape.length > 1) {
            this.stride = Tensor.stride(shape);


        } else {

            this.stride = ArrayUtils.EMPTY_INT_ARRAY;
        }

        this.shape = shape;
    }

    public static ArrayTensor vector(float... data) {
        return new ArrayTensor(data);
    }

    public void fill(float x) {
        int v = volume();
        for (int i = 0; i < v; i++)
            set(x, i);
    }

    @Override
    public int[] stride() {
        return stride;
    }

    @Override
    public int[] shape() {
        return shape;
    }

    @Override
    public float get(int... cell) {
        return getAt(index(cell));
    }

    @Override
    public abstract float getAt(int linearCell);

    @Override
    public abstract void set(float newValue, int linearCell);

    @Override
    public void set(float newValue, int... cell) {
        set(newValue, index(cell));
    }

    abstract public void add(float x, int linearCell);

}
