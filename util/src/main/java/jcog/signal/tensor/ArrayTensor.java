package jcog.signal.tensor;

import jcog.Texts;
import jcog.signal.Tensor;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.block.procedure.primitive.IntFloatProcedure;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Arrays;

import static java.lang.System.arraycopy;

/**
 * float tensor - see: https:
 */
public class ArrayTensor implements
        Tensor,
        TensorFrom/* source, getters, suppliers */,
        TensorTo /* target, setters, consumers */,
        Serializable {

    public static final Tensor Zero = new ArrayTensor(0);

    public final float[] data;
    public final int[] shape;
    transient private final int[] stride;

    public ArrayTensor(float... oneD) {
        this.shape = new int[]{oneD.length};
        this.stride = ArrayUtils.EMPTY_INT_ARRAY;
        this.data = oneD;
    }

    public ArrayTensor(int dim, float... data) {
        this.shape = new int[]{dim};
        this.stride = ArrayUtils.EMPTY_INT_ARRAY;
        this.data = data;
    }

    public ArrayTensor(int... shape) {
        int size = shape[0];
        if (shape.length > 1) {
            this.stride = Tensor.stride(shape);
        } else {
            this.stride = ArrayUtils.EMPTY_INT_ARRAY;
        }

        this.shape = shape;
        this.data = new float[size];
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
    public String toString() {
        return Arrays.toString(shape) + '<' + Texts.n4(data) + '>';
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj ||
                (obj instanceof ArrayTensor && (
                        Arrays.equals(data, ((ArrayTensor) obj).data) &&
                                Arrays.equals(shape, ((ArrayTensor) obj).shape)
                ));
    }

    @Override
    public float get(int... cell) {
        return getAt(index(cell));
    }

    @Override
    public float getAt(int linearCell) {
        return data[linearCell];
    }


    @Override
    public void set(float newValue, int linearCell) {
        data[linearCell] = newValue;
    }

    @Override
    public void set(float newValue, int... cell) {
        set(newValue, index(cell));
    }

    @Override
    public float[] snapshot() {
        return data.clone();
    }


    @Override
    public void forEach(IntFloatProcedure each, int start, int end) {
        float[] d = data;
        for (int i = start; i < end; i++) {
            each.value(i, d[i]);
        }
    }


    public void set(@NotNull float[] raw) {
        int d = data.length;
        assert (d == raw.length);
        arraycopy(raw, 0, data, 0, d);
    }

    /**
     * downsample 64 to 32
     */
    public void set(@NotNull double[] d) {
        assert (data.length == d.length);
        for (int i = 0; i < d.length; i++)
            data[i] = (float) d[i];
    }

    public void fill(float v) {
        Arrays.fill(data, v);
    }

    public static ArrayTensor vector(float... data) {
        return new ArrayTensor(data.length, data);
    }


}
