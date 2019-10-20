package jcog.tree.rtree.point;


import jcog.tree.rtree.HyperPoint;

import java.io.Serializable;
import java.util.Arrays;


/**
 * Created by me on 12/21/16.
 */
public class LongND implements HyperPoint, Serializable, Comparable<LongND> {

    public final long[] coord;
    private final int hash;

    public LongND(LongND copy) {
        this(copy.coord.clone());
    }

    public LongND(long... coord) {
        this.coord = coord;
        this.hash = Arrays.hashCode(coord);
    }

    public static LongND fill(int dims, long value) {
        var a = new long[dims];
        Arrays.fill(a, value);
        return new LongND(a);
    }

    @Override
    public int dim() {
        return coord.length;
    }

    @Override
    public Long coord(int d) {
        return coord[d];
    }

    @Override
    public double distance(HyperPoint h) {
        var p = (LongND) h;
        double sumSq = 0;
        for (var i = 0; i < coord.length; i++) {
            var x = coord[i];
            var y = p.coord[i];
            var xMinY = x - y;
            sumSq += xMinY * xMinY;
        }
        return Math.sqrt(sumSq);
    }

    @Override
    public double distance(HyperPoint p, int i) {
        return Math.abs(coord[i] - ((LongND) p).coord[i]);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LongND)) return false;

        var longND = (LongND) o;
        return hash == longND.hashCode() && Arrays.equals(coord, longND.coord);







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
    public int compareTo(LongND o) {
        return Arrays.compare(coord, o.coord);
    }
}
