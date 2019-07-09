package nars.task.util;

import jcog.Skill;
import jcog.Util;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;

/** utility for finding the most extreme TaskRegion by reducing an iteration through a comparator.
 *  caches the components of current winner for fast compare.
 *
 * @param X the target type sought
 * @param Y is an extracted derived object (can be = X)
 *
 * */
@Skill("Multi-objective_optimization")
abstract public class Extreme<X,Y> implements Consumer<X>, Supplier<X> {

    private @Nullable X best = null;

    /** evaluation criteria */
    final ToDoubleFunction<? super Y>[] eval;

    /** weights should be ordered largest first for earliest fail opportunity */
    final float[] evalWeight;
    private float totalWeight = Float.NaN;

    final double[] vBest;

    /** re-cycled for each input */
    private transient double[] v;

//    /** re-cycled for each input */
//    private transient MetalBitSet better;
//    /** re-cycled for each input */
//    private transient MetalBitSet worse;

    public Extreme(ToDoubleFunction<? super Y>... eval) {
        this.eval = eval;

        int n = eval.length;
        this.vBest = new double[n];
        this.evalWeight = new float[n];
        Arrays.fill(vBest, Double.NEGATIVE_INFINITY);
        Arrays.fill(evalWeight, 1f);

        v = new double[n];
//        better = MetalBitSet.bits(n);
//        worse = MetalBitSet.bits(n);
    }


    /** return null to invalidate */
    abstract protected Y the(X x);

    @Override
    public final void accept(X incoming) {
        if (best == incoming) return; //already here


        Y incomingDerived = the(incoming);
        if (incomingDerived == null) return; //invalidated


        if (totalWeight!=totalWeight)
            totalWeight = Util.sum(evalWeight);

//        better.clear(); worse.clear();
        double score = 0, weightRemain = totalWeight;
        int n = eval.length;
        for (int i = 0; i < n; i++) {
            double vi = eval[i].applyAsDouble(incomingDerived);
            if (vi!=vi)
                return; //invalidated
            v[i] = vi;

            double ei = this.vBest[i];

            double w = evalWeight[i];
            if (vi > ei) score += w;
            else if (vi < ei) score -= w;
            //else: equal

            weightRemain -= w;
            if (Math.abs(score) > Math.abs(weightRemain + Float.MIN_NORMAL))
                break; //early exit due to impossible to reach positive
        }

        if (score > 0) {
            best = incoming;
            System.arraycopy(this.v, 0, this.vBest, 0, n);
        }
    }

    @Nullable final public X get() {
        return best;
    }

    public final boolean isEmpty() {
        return best ==null;
    }

    public final boolean accepted(X m) {
        accept(m);
        return best == m;
    }

    public void weights(float... w) {
        if (w.length!=eval.length)
            throw new ArrayIndexOutOfBoundsException(w.length);

        System.arraycopy(w, 0, evalWeight, 0, w.length);
        invalidateWeights();
    }

    public void weight(int eval, float weight) {
        evalWeight[eval] = weight;
        invalidateWeights();
    }

    private void invalidateWeights() {
        totalWeight = Float.NaN; //invalidate
    }


}
