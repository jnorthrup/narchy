package nars.derive.premise;

import jcog.Util;
import jcog.decide.MutableRoulette;
import jcog.memoize.HijackMemoize;
import jcog.memoize.Memoize;
import nars.Param;
import nars.control.Cause;
import nars.derive.Derivation;
import nars.term.control.PrediTerm;
import org.eclipse.collections.api.block.function.primitive.IntToFloatFunction;
import org.eclipse.collections.api.block.procedure.primitive.ObjectIntProcedure;

import java.io.PrintStream;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * compiled derivation rules
 * what -> can
 *
 */
public class PremiseDeriver implements Predicate<Derivation> {

    public final PrediTerm<Derivation> what;


    /**
     * the causes that this is responsible for, ie. those that may be caused by this
     */

    /*@Stable*/
    public final Cause[] causes;
    /**
     * TODO move this to a 'CachingDeriver' subclass
     */
    public final Memoize<PremiseKey, short[]> whats;
    /**
     * weight vector generation function
     */
    private final Function<Derivation, float[]> value;
    /**
     * choice id to branch mapping function
     */
    private final ObjectIntProcedure<Derivation> branchChoice;

    /** repertoire */
    private final DeriveAction[] can;

    private static final float[] ONE_CHOICE = new float[]{Float.NaN};

    public PremiseDeriver(DeriveAction[] actions, PrediTerm<Derivation> what) {

        this.what = what;
        this.can = actions;

        assert (actions.length > 0);

        this.causes = Stream.of(actions).flatMap(b -> Stream.of(b.causes)).toArray(Cause[]::new);

        IntToFloatFunction branchThrottle = branch ->
                Util.sum(
                        Cause::amp,
                        can[branch].causes
                );
        this.value = d -> {
            short[] will = d.will;
            int n = will.length;
            if (n == 1)
                return ONE_CHOICE;
            else
                return Util.map(n, choice -> branchThrottle.valueOf(will[choice]), new float[n]);
        };
        this.branchChoice = (d, choice) ->
                actions[d.will[choice]].test(d);

        this.whats = new HijackMemoize<>(k -> PremiseKey.solve(what),
                64 * 1024, 4, false);
    }

    @Override
    public boolean test(Derivation d) {

        if (d.ttl < Param.TTL_MIN)
            return false;


        float[] paths = value.apply(d);
        int fanOut = paths.length;
        assert (fanOut > 0);

        int before = d.now();

        if (fanOut == 1) {


            branchChoice.value(d, 0);

            return d.revertLive(before, Param.TTL_BRANCH);

        } else {

            MutableRoulette.run(paths, d.random,

                    wi -> 0

                    , b -> {

                        int beforeTTL = d.ttl;
                        int subBudget = beforeTTL / fanOut;
                        if (subBudget < Param.TTL_MIN)
                            return false;

                        int dTtl = subBudget;

                        branchChoice.value(d, b);

                        int spent = subBudget - dTtl;

                        return d.revertLive(before, Param.TTL_BRANCH + Math.max(spent, 0));
                    }
            );
            return true;
        }
    }

    /**
     * the conclusions that in which this deriver can result
     */
    public Cause[] causes() {
        return causes;
    }

    public void printRecursive() {
        printRecursive(System.out);
    }

    public void printRecursive(PrintStream out) {
        PremiseDeriverCompiler.print(this, out);
    }


    public void print(PrintStream p) {
        print(p, 0);
    }

    public void print(PrintStream p, int indent) {
        PremiseDeriverCompiler.print(what, p, indent);
        PremiseDeriverCompiler.print(can, p, indent);
    }


    public boolean derivable(Derivation x) {
        return (x.will = whats.apply(new PremiseKey(x))).length > 0;
    }
}
