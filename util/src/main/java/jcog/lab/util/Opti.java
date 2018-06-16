package jcog.lab.util;

import jcog.io.arff.ARFF;
import org.eclipse.collections.api.list.ImmutableList;
import org.intelligentjava.machinelearning.decisiontree.RealDecisionTree;

/** simple Optimization<X> wrapper */
public class Opti<X> {

    final Optimization<X, X> o;

    public Opti(Optimization<X, X> o) {
        this.o = o;
    }

    public Opti<X> run() {
        o.run();

        return this;
    }

    public ImmutableList best() {
        return o.best();
    }

    public RealDecisionTree tree(int discretization, int maxDepth) {
        return o.tree(discretization, maxDepth);
    }

    public ARFF data() {
        return o.data;
    }

    public void print() {
        o.print();
    }
}
