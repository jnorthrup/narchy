package jcog.tree.rtree.point;


import jcog.tree.rtree.HyperPoint;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Arrays;


/**
 * Created by me on 12/21/16.
 */
public class FloatND implements HyperPoint, Serializable, Comparable<FloatND> {

    public final float[] coord;
    private final int hash;

    public FloatND(FloatND copy) {
        this(copy.coord.clone());
    }


    public FloatND(float... coord) {
        this.coord = coord;
        this.hash = Arrays.hashCode(coord);
    }

    public static FloatND fill(int dims, float value) {
        float[] a = new float[dims];
        Arrays.fill(a, value);
        return new FloatND(a);
    }

    @Override
    public int dim() {
        return coord.length;
    }

    @Override
    public Float coord(int d) {
        return coord[d];
    }

    @Override
    public double distance(HyperPoint h) {
        FloatND p = (FloatND) h;
        float sumSq = 0;
        for (int i = 0; i < coord.length; i++) {
            float x = coord[i];
            float y = p.coord[i];
            float xMinY = x - y;
            sumSq += xMinY * xMinY;
        }
        return Math.sqrt(sumSq);
    }

    @Override
    public double distance(HyperPoint p, int i) {
        return Math.abs(coord[i] - ((FloatND) p).coord[i]);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FloatND)) return false;

        FloatND floatND = (FloatND) o;
        return hash == floatND.hashCode() && Arrays.equals(coord, floatND.coord);







    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public String toString() {
        return '(' + Arrays.toString(coord) + ')';
    }


    @Override
    public int compareTo(@NotNull FloatND o) {
        return Arrays.compare(coord, o.coord);
    }
}
