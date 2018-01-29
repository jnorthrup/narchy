package jcog.optimize;

import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.tuple.primitive.DoubleObjectPair;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.intelligentjava.machinelearning.decisiontree.FloatTable;
import org.intelligentjava.machinelearning.decisiontree.RealDecisionTree;

/** result = t in tweaks(subject) { eval(subject + tweak(t)) } */
public class Result<X> {

    final FastList<DoubleObjectPair<double[]>> experiments;
    final Tweaks<X> tweaks;
    final FloatFunction<X> eval;

    public Result(FastList<DoubleObjectPair<double[]>> experiments, FloatFunction<X> eval, Tweaks<X> tweaks) {
        experiments.sortThisByDouble(DoubleObjectPair::getOne);
        this.experiments = experiments;
        this.eval = eval;
        this.tweaks = tweaks;
    }

    public DoubleObjectPair<double[]> best() {
        return experiments.getLast();
    }

    public void print() {
        if (!experiments.isEmpty()) {
            DoubleObjectPair<double[]> optimal = best();
            System.out.println(eval + " score=" + optimal.getOne());
            double[] p = optimal.getTwo();
            for (int i = 0; i < p.length; i++) {
                System.out.println(tweaks.get(i).id + ' ' + p[i]);
            }
        } else {
            System.out.println("(no experiments completed)");
        }

    }

    public RealDecisionTree tree(int discretization, int maxDepth) {
        if (experiments.isEmpty())
            return null;


        FloatTable<String> data = new FloatTable<>(
                ArrayUtils.add(
                        tweaks.stream().map(Tweak::toString).toArray(String[]::new), "score")
        );

        int cols = data.cols.length;

        for (DoubleObjectPair<double[]> exp : experiments) {
            float[] r = new float[cols];
            int i = 0;
            for (double x : exp.getTwo()) {
                r[i++] = (float) x;
            }
            r[i] = (float) exp.getOne();
            data.add(r);
        }

        RealDecisionTree rt = new RealDecisionTree(data, cols - 1 /* score */, maxDepth, discretization);

        return rt;

    }


}
