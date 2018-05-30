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
        this.data = new int[dim][dim]; 
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
        data[x][y] = value;
        data[y][x] = value;
    }

    private void addToEdge(short x, short y, int d) {
        int e = data[x][y];
        if (e!=CLEAR_VALUE) {
            data[x][y] += d;
            data[y][x] += d;
        }
    }

    @Override
    public void edgesOf(short vertex, ShortIntProcedure eachKeyValue) {
        int[] a = data[vertex];
        for (short i = 0; i < a.length; i++) {
            int aa = a[i];
            if (aa != CLEAR_VALUE) {
                eachKeyValue.value(i, aa);
            }
        }
    }

    @Override
    public void edgesOf(short vertex, ShortProcedure eachKey) {
        int[] a = data[vertex];
        for (short i = 0; i < a.length; i++) {
            int aa = a[i];
            if (aa != CLEAR_VALUE) {
                eachKey.value(i);
            }
        }
    }


    @Override
    public void removeEdgeIf(IntPredicate filter) {
        int l = data.length;
        for (short i = 0; i < l; i++) {
            if (filter.accept(i)) {
                for (short j = 0; j < l; j++) {
                    setEdge(i, j, CLEAR_VALUE);
                }
            }
        }
    }

    @Override
    public void addToEdges(short x, int d) {
        for (short i = 0; i < data.length; i++) {
            addToEdge(x, i, d);
        }
    }


    @Override
    public void removeVertex(short v) {
        Arrays.fill(data[v], CLEAR_VALUE);
        for (int[] ee : data) {
            ee[v] = CLEAR_VALUE;
        }
    }

    @Override
    public void removeEdge(short first, short second) {
        setEdge(first, second, CLEAR_VALUE);
    }
}
