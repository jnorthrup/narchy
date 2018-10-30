package jcog.lab.util;

import org.intelligentjava.machinelearning.decisiontree.RealDecisionTree;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;

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

    public Row best() {
        return o.best();
    }

    public RealDecisionTree tree(int discretization, int maxDepth) {
        return o.tree(discretization, maxDepth);
    }

    public Table data() {
        return o.data;
    }

    public void print() {
        o.print();
    }
}
