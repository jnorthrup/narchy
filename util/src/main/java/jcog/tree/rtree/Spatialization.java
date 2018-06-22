package jcog.tree.rtree;

import jcog.tree.rtree.split.AxialSplitLeaf;
import jcog.tree.rtree.split.LinearSplitLeaf;
import jcog.tree.rtree.split.QuadraticSplitLeaf;
import org.eclipse.collections.api.tuple.primitive.IntDoublePair;

import java.util.function.Function;

public class Spatialization<T> {

    public static final double EPSILON = Math.sqrt(Float.MIN_NORMAL);
    public static final float EPSILONf = (float) Math.sqrt(Float.MIN_NORMAL);
    public static final float sqrtEPSILONf = (float) Math.sqrt(Math.sqrt(Float.MIN_NORMAL));

    public final Split<T> split;
    public final Function<T, HyperRegion> bounds;
    public final short max;       
    public final short min;       

    public Spatialization(@Deprecated final Function<T, HyperRegion> bounds, final Split<T> split, final int min, final int max) {
        if (min<2)
            throw new UnsupportedOperationException("min split must be >=2");
        if (max < min)
            throw new UnsupportedOperationException("min split must be < max split");
        this.max = (short) max;
        this.min = (short) min;
        this.bounds = bounds;
        this.split = split;
    }

    public HyperRegion bounds(/*@NotNull*/ T t) {
        return bounds.apply(t);
    }

    public Leaf<T> newLeaf() {
        return new Leaf<>(max);
    }

    public Branch<T> newBranch() {
        return new Branch<>(max);
    }

    public Branch<T> newBranch(Leaf<T> a, Leaf<T> b) {
        return new Branch<>(max, a, b);
    }

    public Node<T, ?> split(T t, Leaf<T> leaf) {
        return split.split(t, leaf, this);
    }





    /** existing may be the same instance, or .equals() to the incoming */
    protected void merge(T existing, T incoming) {

    }

    public final Leaf<T> transfer(Leaf<T> leaf, IntDoublePair[] sortedMbr, int from, int to) {
        final Leaf<T> l = newLeaf();
        final T[] ld = leaf.data;
        final Node<T, ?> nl = l;
        for (int i = from; i < to; i++) {
            nl.add(ld[sortedMbr[i].getOne()], leaf, this, new boolean[] { false });
        }
        return l;
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
