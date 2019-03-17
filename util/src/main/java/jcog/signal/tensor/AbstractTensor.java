package jcog.signal.tensor;

import jcog.signal.Tensor;

import java.io.Serializable;

public abstract class AbstractTensor implements Tensor, TensorFrom, Serializable {

    public static final Tensor Zero = new ArrayTensor(0);


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

}
