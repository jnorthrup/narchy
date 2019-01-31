package jcog.signal.tensor;

import jcog.Texts;
import jcog.signal.buffer.CircularFloatBuffer;
import org.eclipse.collections.api.block.procedure.primitive.IntFloatProcedure;

import java.util.Arrays;

import static java.lang.System.arraycopy;

/**
 * float tensor - see: https:
 */
public class ArrayTensor extends AbstractShapedTensor
        /* source, getters, suppliers */
        /* target, setters, consumers */ {

    public final float[] data;

    public ArrayTensor(CircularFloatBuffer b, int start, int end) {
        this(new float[end-start]);
        b.readFully(data, start, end-start);
    }

    public ArrayTensor(float[] oneD) {
        super(new int[]{oneD.length});
        this.data = oneD;
    }


    /** 1D */
    public ArrayTensor(int length) {
        this(new int[] { length } );
    }

    public ArrayTensor(int[] shape) {
        super(shape);
        this.data = new float[super.volume()];
    }

    @Override
    public int volume() {
        return data.length;
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

    /** optimized case */
    @Override public float[] toFloatArrayShared() {
        return data;
    }

    @Override
    public float getAt(int linearCell) {
        return data[linearCell];
    }


    @Override
    public void setAt(float newValue, int linearCell) {
        data[linearCell] = newValue;
    }


    @Override public void fill(float v) {
        Arrays.fill(data, v);
    }

    @Override
    public float[] snapshot() {
        return data.clone();
    }

    @Override
    public void addAt(float x, int linearCell) {
        data[linearCell] += x;
    }

    @Override
    public void forEach(IntFloatProcedure each, int start, int end) {
        float[] d = data;
        for (int i = start; i < end; i++) {
            each.value(i, d[i]);
        }
    }

    public void set(float[] raw) {
        if (data == raw)
            return;
        int d = data.length;
        assert (d == raw.length);
        arraycopy(raw, 0, data, 0, d);
    }

    /**
     * downsample 64 to 32
     */
    public void set(double[] d) {
        assert (data.length == d.length);
        for (int i = 0; i < d.length; i++)
            data[i] = (float) d[i];
    }

}
