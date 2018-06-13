package org.oakgp.select;

import jcog.decide.MutableRoulette;
import org.eclipse.collections.api.block.function.primitive.IntToFloatFunction;
import org.oakgp.node.Node;
import org.oakgp.rank.Ranking;

import java.util.Random;

public abstract class WeightSelector implements NodeSelector, IntToFloatFunction {
    protected final Random random;

    protected Ranking living;

    /** population size, cached for repeated use */
    protected int n;

    MutableRoulette roulette =  null;

    public WeightSelector(Random random) {
        this.random = random;
    }

    @Override
    public void reset(Ranking living) {
        this.living = living;
        //TODO with RankSelector if the size is the same as existing roulette instance, the weights can be re-used since they are constant
        this.roulette = new MutableRoulette(n = living.size(), this, random);
    }

    @Override
    public abstract float valueOf(int i);

    @Override
    public Node get() {
        return living.get(roulette.next()).id;

//        int size = r.size();
//        long s = 0;
//        for (int i = 1; i <= size; i++) {
//            s += i;
//        }
//        long sum = s;
//
//        final double r = random.nextFloat();
//        double p = 0;
//        for (int i = 0; i < size; i++) {
//            p += (size - i) / sum;
//            if (r < p) {
//                return r.get(i).id;
//            }
//        }
//
//        return r.top().id;
    }
}
