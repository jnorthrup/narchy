package jcog.tree.rtree.point;


import jcog.Util;
import jcog.tree.rtree.HyperPoint;

import java.io.Serializable;
import java.util.Arrays;

import static jcog.tree.rtree.Spatialization.EPSILON;


/**
 * Created by me on 12/21/16.
 */
public class DoubleND implements HyperPoint, Serializable {

    public final double[] coord;

    public DoubleND(DoubleND copy) {
        this(copy.coord.clone());
    }

    public DoubleND(int length) {
        this(new double[length]);
    }

    public DoubleND(double[] coord) {
        this.coord = coord;
    }

    public static DoubleND fill(int dims, double value) {
        var a = new double[dims];
        Arrays.fill(a, value);
        return new DoubleND(a);
    }

    @Override
    public int dim() {
        return coord.length;
    }

    @Override
    public Double coord(int d) {
        return coord[d];
    }

    @Override
    public double distance(HyperPoint h) {
        var p = (DoubleND) h;
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
        return Math.abs(coord[i] - ((DoubleND) p).coord[i]);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        var p = (DoubleND) obj;
        return Util.equals(coord, p.coord, EPSILON);
    }

    @Override
    public int hashCode() {
        
        return Arrays.hashCode(coord);
    }

    @Override
    public String toString() {
        return '(' + Arrays.toString(coord) + ')';
    }










}
