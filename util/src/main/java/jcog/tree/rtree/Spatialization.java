package jcog.tree.rtree;

import jcog.tree.rtree.split.AxialSplitLeaf;
import jcog.tree.rtree.split.LinearSplitLeaf;
import jcog.tree.rtree.split.QuadraticSplitLeaf;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class Spatialization<T> {

    public final Split<T> split;
    public final Function<T, HyperRegion> region;
    public final short max;       // max entries per node
    public final short min;       // least number of entries per node

    public Spatialization(@Deprecated final Function<T, HyperRegion> region, DefaultSplits split, final int min, final int max) {
        this(region, split.get(), min, max);
    }

    public Spatialization(@Deprecated final Function<T, HyperRegion> region, final Split<T> split, final int min, final int max) {
        this.max = (short) max;
        this.min = (short) min;
        this.region = region;
        this.split = split;
    }

    public HyperRegion region(@NotNull T t) {
        return region.apply(t);
    }

    public Node<T, T> newLeaf() {
        return new Leaf(max);
    }

    public Branch<T> newBranch() {
        return new Branch<>(max);
    }

    public Node<T, ?> split(T t, Leaf<T> leaf) {
        return split.split(t, leaf, this);
    }

    public double perimeter(T c) {
        return region(c).perimeter();
    }

    /** called when add encounters an equivalent (but different) instance */
    public void merge(T existing, T incoming) {

    }

    /**
     * Different methods for splitting nodes in an RTree.
     */
    public enum DefaultSplits {
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
