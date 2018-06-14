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

import static jcog.pri.Prioritized.EPSILON;

/**
 * compiled derivation rules
 * what -> can
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
     * repertoire
     */
    private final DeriveAction[] could;


    public PremiseDeriver(DeriveAction[] actions, PrediTerm<Derivation> what) {

        this.what = what;
        this.could = actions;

        assert (actions.length > 0);

        this.causes = Stream.of(actions).flatMap(b -> Stream.of(b.cause)).toArray(Cause[]::new);


        this.whats = new HijackMemoize<>(k -> PremiseKey.solve(what),
                64 * 1024, 4, false);
    }

    /**
     * choice id to branch id mapping
     */
    final boolean test(Derivation d, int branch) {
        return could[branch].test(d);
    }

    @Override
    public boolean test(Derivation d) {


        /**
         * weight vector generation
         */
        short[] can = d.will;

        int fanOut;
        float[] maybe;
        if (can.length > 1) {
            maybe = Util.remove(
                            Util.map(choice -> this.could[can[choice]].value(d), new float[can.length]),
                            w -> w <= 0
                    );
            fanOut = maybe.length;
        } else {
            if (can.length == 1 && this.could[can[0]].value(d) > EPSILON) {
                fanOut = 1;
            } else {
                fanOut = 0;
            }
            maybe = null;
        }

        if (fanOut > 0) {

            int totalTTL = d.ttl;
            int maxTTL = Param.TTL_MAX_BRANCH;
            int minTTL = Param.TTL_MIN_BRANCH;
            assert(totalTTL > minTTL);


            switch (fanOut) {
                case 1: {
                    d.setTTL(Math.min(maxTTL, totalTTL));
                    return test(d, can[0]);
                }
                default: {

                    @Deprecated int before = d.now(); assert(d.now()==0);

                    final int[] ttlRemain = {totalTTL};
                    /**  depth  vs. breadth factor:  1 = fairly distributed among banches, >1..fanOut = depth concentrated */
                    float depth = 2;
                    int branchTTL = Util.clamp(Math.round(ttlRemain[0] / (fanOut / depth )), minTTL, maxTTL);

                    d.ttl = 0;

                    MutableRoulette.run(maybe, d.random, wi -> 0, b -> {

                            d.ttl += Math.min(ttlRemain[0], branchTTL);

                            int ttlBefore = d.ttl;

                            test(d, can[b]);

                            int ttlAfter = d.ttl;
                            int spent = Param.TTL_BRANCH + (ttlBefore - ttlAfter);

                            ttlRemain[0] -= spent;


                            if (ttlRemain[0] <= 0) {
                                return false;
                            } else {
                                d.revertLive(before);
                                return true;
                            }
                        }
                    );
                }
            }
        }
        return true;
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
        PremiseDeriverCompiler.print(could, p, indent);
    }


    public boolean derivable(Derivation x) {
        return (x.will = whats.apply(new PremiseKey(x))).length > 0;
    }
}
