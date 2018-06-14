package nars.derive.premise;

import jcog.Util;
import jcog.decide.MutableRoulette;
import jcog.memoize.HijackMemoize;
import jcog.memoize.Memoize;
import nars.Param;
import nars.control.Cause;
import nars.derive.Derivation;
import nars.term.control.PrediTerm;

import java.io.PrintStream;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * compiled derivation rules
 * what -> can
 */
public class PremiseDeriver implements Predicate<Derivation> {

    private static final float[] ONE_CHOICE = new float[]{Float.NaN};
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
     * repertoire
     */
    private final DeriveAction[] can;


    public PremiseDeriver(DeriveAction[] actions, PrediTerm<Derivation> what) {

        this.what = what;
        this.can = actions;

        assert (actions.length > 0);

        this.causes = Stream.of(actions).flatMap(b -> Stream.of(b.cause)).toArray(Cause[]::new);


        this.whats = new HijackMemoize<>(k -> PremiseKey.solve(what),
                64 * 1024, 4, false);
    }

    /**
     * choice id to branch id mapping
     */
    final boolean test(Derivation d, int branch) {
        return can[branch].test(d);
    }

    @Override
    public boolean test(Derivation d) {

        if (d.ttl < Param.TTL_MIN)
            return false;


        /**
         * weight vector generation
         */
        short[] will = d.will;
        int fanOut = will.length;

        float[] paths;
        if (fanOut > 1) {
            paths = Util.remove(
                            Util.map(fanOut, choice -> can[will[choice]].value(d), new float[fanOut]),
                            w -> w <= 0
                    );
        } else
            paths = null;

        switch (fanOut) {
            case 0:
                return true;
            case 1:
                return test(d, will[0]);
            default: {

                int before = d.now();

                MutableRoulette.run(paths, d.random,

                        wi -> 0

                        , b -> {

                            int beforeTTL = d.ttl;
                            int subBudget = beforeTTL / fanOut;
                            if (subBudget < Param.TTL_MIN)
                                return false;

                            int dTtl = subBudget;

                            test(d, will[b]);

                            int spent = subBudget - dTtl;

                            return d.revertLive(before, Param.TTL_BRANCH + Math.max(spent, 0));
                        }
                );
                return true;
            }
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
