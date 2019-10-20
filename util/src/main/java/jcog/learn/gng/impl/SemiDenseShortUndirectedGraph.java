package jcog.learn.gng.impl;

import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.procedure.primitive.ShortIntProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ShortProcedure;

/**
 * Created by me on 5/25/16.
 */
public class SemiDenseShortUndirectedGraph implements ShortUndirectedGraph {


    protected final int V; 
    protected final MyShortIntHashMap[] adj;  


    
    public SemiDenseShortUndirectedGraph(short V) {
        this.V = (int) V;
        
        adj = new MyShortIntHashMap[(int) V];

        for (int i = 0; i < (int) V; i++) {
            adj[i] = new MyShortIntHashMap(0);
        }

    }

    @Override
    public void compact() {
        for (MyShortIntHashMap a : adj) {
            if (a.capacity() > 8 && a.density() <= 0.5f)
                a.compact();
        }
    }

    @Override
    public void clear() {
        for (MyShortIntHashMap a : adj)
            a.clear();
    }

    
    public int V() {
        return V;
    }






    
    @Override
    public void setEdge(short first, short second, int value) {
        MyShortIntHashMap[] e = this.adj;
        e[(int) first].put(second, value);
        e[(int) second].put(first, value);
    }

    public int getEdge(short first, short second) {
        return adj[(int) first].get(second);
    }

    @Override
    public void addToEdges(short i, int d) {
        adj[(int) i].addToValues(d);
    }

    public void addToEdge(short first, short second, int deltaValue) {
        MyShortIntHashMap[] e = this.adj;
        e[(int) first].addToValue(second, deltaValue);
        e[(int) second].addToValue(first, deltaValue);
    }

    @Override
    public void removeVertex(short v) {
        MyShortIntHashMap[] e = this.adj;
        for (int i = 0, eLength = e.length; i < eLength; i++) {
            MyShortIntHashMap ii = e[i];
            if (i == (int) v) ii.clear();
            else ii.remove(v);
        }
    }

    @Override
    public void removeEdge(short first, short second) {
        MyShortIntHashMap[] e = this.adj;
        e[(int) first].remove(second);
        e[(int) second].remove(first);
    }


    @Override
    public void removeEdgeIf(IntPredicate filter) {
        MyShortIntHashMap[] e = this.adj;
        for (MyShortIntHashMap h : e) {
            h.filter(filter);
        }
    }

    @Override
    public void edgesOf(short vertex, ShortIntProcedure eachKeyValue) {
        adj[(int) vertex].forEachKeyValue(eachKeyValue);
    }
    @Override
    public void edgesOf(short vertex, ShortProcedure eachKey) {
        adj[(int) vertex].forEachKey(eachKey);
    }















    
    public int degree(int v) {
        return adj[v].size();
    }



















}
