//package jcog.learn.gng;
//
//import com.google.common.primitives.Doubles;
//import jcog.learn.Discretize1D;
//import jcog.learn.gng.impl.Centroid;
//import org.jetbrains.annotations.NotNull;
//
///**
// * convenience class for discretizing points in a 1D (linear) space
// * with growing neural gasnet
// *
// * this is overkill for 1D.  use ordinary Quantilers
// */
//public class Gasolinear implements Discretize1D {
//
//    NeuralGasNet<Gasolinear.Sorted1DCentroid> net;
//
//    boolean needsSort;
//    private double min, max;
//
//    public static class Sorted1DCentroid extends Centroid {
//        public int order = -1;
//
//        public Sorted1DCentroid(int id) {
//            super(id, 1);
//        }
//    }
//
//    @Override
//    public String toString() {
//        return min + ".." + max + ":\n" + net;
//    }
//
//    @Override public void reset(int nodes, double min, double max) {
//        this.min = min;
//        this.max = max;
//        this.net = new NeuralGasNet<>(1, nodes) {
//            @Override
//            public Sorted1DCentroid put(double... x) {
//                Sorted1DCentroid y = super.put(x);
//                needsSort = true;
//                return y;
//            }
//
//            @NotNull
//            @Override
//            public Gasolinear.Sorted1DCentroid newCentroid(int i, int dims) {
//
//
//
//                return (Sorted1DCentroid) new Sorted1DCentroid(i).randomizeUniform(min, max);
//            }
//        };
//
//        for (int i = 0; i < nodes; i++) {
//            net.centroids[i].setEntry(0, (max - min) * (i / (nodes - 1f)) + min);
//        }
//
//        needsSort = true;
//    }
//
//
//    public static Gasolinear of(int nodes, double... points) {
//        double min = Doubles.min(points);
//        double max = Doubles.max(points);
//        Gasolinear g = new Gasolinear();
//        g.reset(nodes, min, max);
//        g.net.setWinnerUpdateRate(2f / points.length, 0.05f / points.length);
//        for (double x : points)
//            g.put(x /* 1D */);
//
//
//        return g;
//    }
//
//    @Override
//    public double value(int v) {
//        synchronized (net) {
//            ensureSorted();
//            return net.centroids[v].getEntry(0);
//        }
//    }
//
//    @Override
//    public void put(double value) {
//        synchronized (net) {
//            net.put(value);
//            ensureSorted();
//        }
//    }
//
//    public synchronized int index(double x) {
//        synchronized (net) {
//            int i = net.put(x).order;
//            ensureSorted();
//            return i;
//        }
//    }
//
//
//    private void ensureSorted() {
//        if (!needsSort)
//            return;
//
//        Centroid[] l = net.centroids.clone();
//        java.util.Arrays.sort(l, (a, b) -> Doubles.compare(a.getEntry(0), b.getEntry(0)));
//        int i = 0;
//        for (Centroid m : l) {
//            ((Sorted1DCentroid) m).order = i++;
//        }
//        needsSort = false;
//    }
//
//
//}
