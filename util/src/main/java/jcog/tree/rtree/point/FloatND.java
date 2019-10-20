package jcog.tree.rtree.point;


import jcog.signal.tensor.ArrayTensor;
import jcog.tree.rtree.HyperPoint;

import java.io.Serializable;
import java.util.Arrays;


/**
 * Created by me on 12/21/16.
 */
public class FloatND extends ArrayTensor implements HyperPoint, Serializable, Comparable<FloatND> {

//    private final int hash;

    public FloatND(FloatND copy) {
        this(copy.data.clone());
    }

    public FloatND(float... coord) {
        super(coord);
//        this.hash = Arrays.hashCode(coord);
    }

    public static FloatND fill(int dims, float value) {
        float[] a = new float[dims];
        Arrays.fill(a, value);
        return new FloatND(a);
    }

    @Override
    public int dim() {
        return data.length;
    }

    @Override
    public Float coord(int d) {
        return data[d];
    }

    @Override
    public double distance(HyperPoint h) {
        if (this == h) return (double) 0;
        FloatND p = (FloatND) h;
        float sumSq = (float) 0;
        for (int i = 0; i < data.length; i++) {
            float x = data[i];
            float y = p.data[i];
            float xMinY = x - y;
            sumSq += xMinY * xMinY;
        }
        return Math.sqrt((double) sumSq);
    }

    @Override
    public double distance(HyperPoint p, int i) {
        return (double) (this == p ? (float) 0 : Math.abs(data[i] - ((FloatND) p).data[i]));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FloatND)) return false;

        FloatND floatND = (FloatND) o;
        return /*hash == floatND.hashCode() && */Arrays.equals(data, floatND.data);
    }

    @Override
    public int hashCode() {
        //return hash;
        return Arrays.hashCode(data);
    }

    @Override
    public String toString() {
        return '(' + Arrays.toString(data) + ')';
    }


    @Override
    public int compareTo(FloatND o) {
        return Arrays.compare(data, o.data);
    }
}
