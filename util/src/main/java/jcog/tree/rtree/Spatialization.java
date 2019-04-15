package jcog.tree.rtree;

import jcog.tree.rtree.split.AxialSplitLeaf;
import jcog.tree.rtree.split.LinearSplitLeaf;
import jcog.tree.rtree.split.QuadraticSplitLeaf;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class Spatialization<X> {

    public static final double EPSILON =
            Math.pow(Float.MIN_NORMAL, 1f/4); //E-10
            //Math.pow(Float.MIN_NORMAL, 1f/3); //E-15?
            //Math.pow(Float.MIN_NORMAL, 1/2); //E-19
            //Float.MIN_NORMAL; //E-38
    public static final float EPSILONf = (float)EPSILON;

    private final Split<X> split;
    private final Function<X, HyperRegion> bounds;
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
        return new Branch<>(max, new Node[] { a, b });
    }

    public Node<X> split(X x, Leaf<X> leaf) {
        return split.split(x, leaf, this);
    }

    public Leaf<X> transfer(X[] sortedMbr, int from, int to) {
        return new Leaf<>(this, sortedMbr, from, to);
    }

    public double epsilon() {
        return EPSILON;
    }

    /** if a merge is possible, either a or b or a new task will be returned.  null otherwise
     *  existing and incoming will not be the same instance.
     *  default implementation: test for equality and re-use existing item
     * */
    @Nullable
    public X merge(X existing, X incoming) {
        return existing.equals(incoming) ? existing : null;
    }

    /** one-way merge for containment test.
     * container and content will not be the same instance.
     * default implementation simply tests for equality */
    boolean mergeContain(X container, X content) {
        return container.equals(content);
    }

    public HyperRegion mbr(X[] data) {
       //return HyperRegion.mbr(this, data);
        HyperRegion bounds = bounds(data[0]);
        for (int k = 1; k < data.length; k++) {
            X kk = data[k];
            if (kk == null)
                break; //null terminator
            bounds = bounds.mbr(bounds(kk));
        }
        return bounds;
    }

    public boolean mergeCanStretch() {
        return false;
    }

//    public HyperRegion mbr(Node<X>[] data) {
//        return HyperRegion.mbr(data);
//    }


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
