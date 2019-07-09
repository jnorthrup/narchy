package nars.task.util;

import jcog.Skill;
import jcog.data.bit.MetalBitSet;
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

    final double[] bestVal;

    /** re-cycled for each input */
    private transient double[] v;
    /** re-cycled for each input */
    private transient MetalBitSet better;
    /** re-cycled for each input */
    private transient MetalBitSet worse;

    public Extreme(ToDoubleFunction<? super Y>... eval) {
        this.eval = eval;
        this.bestVal = new double[eval.length];
        Arrays.fill(bestVal, Double.NEGATIVE_INFINITY);

        v = new double[eval.length];
        better = MetalBitSet.bits(eval.length);
        worse = MetalBitSet.bits(eval.length);
    }


    /** return null to invalidate */
    abstract protected Y the(X x);

    @Override
    public final void accept(X incoming) {
        if (best == incoming) return; //already here


        Y incomingDerived = the(incoming);
        if (incomingDerived == null) return; //invalidated

        better.clear(); worse.clear();
        int n = eval.length;
        for (int i = 0; i < n; i++) {
            double vi = eval[i].applyAsDouble(incomingDerived);
            if (vi!=vi)
                return; //invalidated
            v[i] = vi;

            double ei = this.bestVal[i];

            if (vi > ei) better.set(i);
            else if (vi < ei) worse.set(i);
            //else: equal
        }
        boolean take;
        int B = better.cardinality();
        if (B == n) {
            take = true; //wins all
        } else {
            int W = worse.cardinality();
            if (W == n) {
                take = false; //loses all
            } else {
                //evaluate
                //TODO refine and abstract pareto frontier calculation
                take = (B > W);
            }
        }

        if (take) {
            best = incoming;
            System.arraycopy(this.v, 0, this.bestVal, 0, n);
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

}
