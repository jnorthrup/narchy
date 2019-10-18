package jcog.learn.search.impl;

import java.util.Comparator;
import java.util.PriorityQueue;

public class OpenSet<F> implements IOpenSet<F> {
    private final PriorityQueue<F> Q;

    public OpenSet(Comparator<F> comp) {
        Q = new PriorityQueue<>(comp);
    }

    @Override
    public void add(F node) {
        this.Q.add(node);
    }

    @Override
    public void remove(F node) {
        this.Q.remove(node);

    }

    @Override
    public F poll() {
        return this.Q.poll();
    }

    @Override
    public F getNode(F node) {
        return this.Q.stream().filter(openSearchNode -> openSearchNode.equals(node)).findFirst().orElse(null);
    }

    @Override
    public int size() {
        return this.Q.size();
    }

}
