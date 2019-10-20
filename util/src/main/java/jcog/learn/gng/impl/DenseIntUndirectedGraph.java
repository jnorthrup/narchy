package jcog.learn.gng.impl;

import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.procedure.primitive.ShortIntProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ShortProcedure;

import java.util.Arrays;

/**
 * WARNING not finished yet, doesnt seem to work the same as SemiDense which was the original implementation
 */
public class DenseIntUndirectedGraph implements ShortUndirectedGraph {

    public static final int CLEAR_VALUE =
            
            0;

    public final int[][] data;

    public DenseIntUndirectedGraph(short dim) {
        this.data = new int[(int) dim][(int) dim];
        clear();
    }

    @Override
    public String toString() {
        return Arrays.deepToString(data);
    }

    @Override
    public void clear() {
        for (int[] x : data)
            Arrays.fill(x, CLEAR_VALUE);
    }

    @Override
    public void setEdge(short x, short y, int value) {
        data[(int) x][(int) y] = value;
        data[(int) y][(int) x] = value;
    }

    private void addToEdge(short x, short y, int d) {
        int e = data[(int) x][(int) y];
        if (e!=CLEAR_VALUE) {
            data[(int) x][(int) y] += d;
            data[(int) y][(int) x] += d;
        }
    }

    @Override
    public void edgesOf(short vertex, ShortIntProcedure eachKeyValue) {
        int[] a = data[(int) vertex];
        for (short i = (short) 0; (int) i < a.length; i++) {
            int aa = a[(int) i];
            if (aa != CLEAR_VALUE) {
                eachKeyValue.value(i, aa);
            }
        }
    }

    @Override
    public void edgesOf(short vertex, ShortProcedure eachKey) {
        int[] a = data[(int) vertex];
        for (short i = (short) 0; (int) i < a.length; i++) {
            int aa = a[(int) i];
            if (aa != CLEAR_VALUE) {
                eachKey.value(i);
            }
        }
    }


    @Override
    public void removeEdgeIf(IntPredicate filter) {
        int l = data.length;
        for (short i = (short) 0; (int) i < l; i++) {
            if (filter.accept((int) i)) {
                for (short j = (short) 0; (int) j < l; j++) {
                    setEdge(i, j, CLEAR_VALUE);
                }
            }
        }
    }

    @Override
    public void addToEdges(short x, int d) {
        for (short i = (short) 0; (int) i < data.length; i++) {
            addToEdge(x, i, d);
        }
    }


    @Override
    public void removeVertex(short v) {
        Arrays.fill(data[(int) v], CLEAR_VALUE);
        for (int[] ee : data) {
            ee[(int) v] = CLEAR_VALUE;
        }
    }

    @Override
    public void removeEdge(short first, short second) {
        setEdge(first, second, CLEAR_VALUE);
    }
}
