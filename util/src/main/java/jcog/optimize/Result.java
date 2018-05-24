package jcog.optimize;

import jcog.io.arff.ARFF;
import org.eclipse.collections.api.list.ImmutableList;
import org.intelligentjava.machinelearning.decisiontree.RealDecisionTree;

import java.util.List;

/** result = t in tweaks(subject) { eval(subject + tweak(t)) } */
public class Result<X> {


    final List<Tweak<X,?>> tweaks;
    public final ARFF data;


    public Result(ARFF data, List<Tweak<X, ?>> tweaks) {
        this.data = data;
        this.tweaks = tweaks;
    }

    public ImmutableList best() {
        double bestScore = Double.NEGATIVE_INFINITY;
        ImmutableList best = null;
        for (ImmutableList e : data.data) {
            double s = ((Number) e.get(0)).doubleValue();
            if (s > bestScore) {
                best = e;
                bestScore = s;
            }
        }
        return best;
    }

    public void print() {
        data.print();
    }

    public RealDecisionTree tree(int discretization, int maxDepth) {
        return data.isEmpty() ? null :
            new RealDecisionTree(data.toFloatTable(),
                0 /* score */, maxDepth, discretization);
    }


}
