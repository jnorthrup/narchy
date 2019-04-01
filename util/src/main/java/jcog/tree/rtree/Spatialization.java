package jcog.tree.rtree;

import jcog.tree.rtree.split.AxialSplitLeaf;
import jcog.tree.rtree.split.LinearSplitLeaf;
import jcog.tree.rtree.split.QuadraticSplitLeaf;

import java.util.function.Function;

public class Spatialization<X> {

    public static final double EPSILON =
            //Math.pow(Float.MIN_NORMAL, 1f/4); //E-10
            Math.pow(Float.MIN_NORMAL, 1f/3); //E-15?
            //Math.pow(Float.MIN_NORMAL, 1/2); //E-19
            //Float.MIN_NORMAL; //E-38
    public static final float EPSILONf = (float)EPSILON;

    public final Split<X> split;
    public final Function<X, HyperRegion> bounds;
    public final short max;       


    public Spatialization(@Deprecated final Function<X, HyperRegion> bounds, final Split<X> split, final int max) {
        this.max = (short) max;
        this.bounds = bounds;
        this.split = split;
    }

    public HyperRegion bounds(/*@NotNull*/ X x) {
        return bounds.apply(x);
    }

    @Deprecated public final Leaf<X> newLeaf() {
        return newLeaf(max);
    }
    public Leaf<X> newLeaf(int capacity) {
        return new Leaf<>(capacity);
    }

    public Branch<X> newBranch(Leaf<X> a, Leaf<X> b) {
        return new Branch<>(max, a, b);
    }

    public Node<X> split(X x, Leaf<X> leaf) {
        return split.split(x, leaf, this);
    }


    /** existing may be the same instance, or .equals() to the incoming */
    protected void onMerge(X existing, X incoming) {
        //default: do nothing
    }

    public Leaf<X> transfer(X[] sortedMbr, int from, int to) {
        return new Leaf<>(this, sortedMbr, from, to);
    }

    public double epsilon() {
        return EPSILON;
    }


    /**
     * Different methods for splitting nodes in an RTree.
     */
    @Deprecated public enum DefaultSplits {
        AXIAL {
            @Override
            public <T> Split<T> get() {
                return new AxialSplitLeaf<>();
            }
        },
        LINEAR {
            @Override
            public <T> Split<T> get() {
                return new LinearSplitLeaf<>();
            }
        },
        QUADRATIC {
            @Override
            public <T> Split<T> get() {
                return new QuadraticSplitLeaf<>();
            }
        };

        abstract public <T> Split<T> get();

    }
}
