package jcog.math.tensor;

import jcog.Texts;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.block.procedure.primitive.IntFloatProcedure;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Arrays;

import static java.lang.System.arraycopy;

/**
 * float tensor - see: https://github.com/lessthanoptimal/DeepBoof/blob/master/modules/main/src/main/java/deepboof/Tensor.java
 */
public class ArrayTensor implements
        Tensor,
        TensorFrom/* source, getters, suppliers */,
        TensorTo /* target, setters, consumers */,
        Serializable {

    public final float[] data;
    public final int[] shape;
    transient private final int[] stride;

    public ArrayTensor(float... oneD) {
        this.shape = new int[] { oneD.length };
        this.stride = ArrayUtils.EMPTY_INT_ARRAY;
        this.data = oneD;
    }
    public ArrayTensor(int dim, float... data) {
        this.shape = new int[] { dim };
        this.stride = ArrayUtils.EMPTY_INT_ARRAY;
        this.data = data;
    }
    public ArrayTensor(int... shape) {
        int size = shape[0];
        if (shape.length > 1) {
            this.stride = new int[size - 1];
            int striding = shape[0];
            for (int i = 1, dimsLength = shape.length; i < dimsLength; i++) {
                size *= shape[i];
                this.stride[i-1] = striding;
                striding *= shape[i];
            }
        } else {
            this.stride = ArrayUtils.EMPTY_INT_ARRAY;
        }

        this.shape = shape; //TODO intern shape for fast compare
        this.data = new float[size];
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
                    Arrays.equals(data, ((ArrayTensor)obj).data) &&
                    Arrays.equals(shape, ((ArrayTensor)obj).shape)
                ));
    }

    @Override
    public float get(int... cell) {
        return get(index(cell));
    }

    @Override
    public float get(int linearCell) {
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
        return get().clone();
    }

    @Override
    public float[] get() {
        return data;
    }

    //    /** inverse of index; sets entries in the coords array according to the current index */
//    public int[] coord(int index, int[] coord) {
//        throw new UnsupportedOperationException();
////        for (int s = shape.length-1; s > 0; s--) {
////            int ss = stride[s-1];
////            int x = index % ss;
////            coord[s] = x;
////            index/= ss;
////        }
////        coord[0] = index;
////        return coord;
//    }

    @Override
    public void forEach(IntFloatProcedure each, int start, int end) {
        float[] d = get();
        for (int i = start; i < end; i++ ) {
            each.value(i, d[i]);
        }
    }

    @Override
    public int index(int... coord) {
        int f = coord[0];
        for (int s = 1, iLength = shape.length; s < iLength; s++) {
            f += stride[s-1] * coord[s];
        }
        return f;
    }

    public void set(@NotNull float[] raw) {
        int d = data.length;
        assert(d == raw.length);
        arraycopy(raw, 0, data, 0, d);
    }

    /** downsample 64 to 32 */
    public void set(@NotNull double[] d) {
        assert(data.length == d.length);
        for (int i = 0; i < d.length; i++)
            data[i] = (float)d[i];
    }

    public void fill(float v) {
        Arrays.fill(data, v);
    }

    public static ArrayTensor vector(float... data) {
        return new ArrayTensor(data.length, data);
    }

//
//    public void copyTo(float[] target, int targetOffset, int... subset) {
//        //TODO
//    }

}
