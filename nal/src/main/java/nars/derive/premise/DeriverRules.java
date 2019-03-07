package nars.derive.premise;

import jcog.Util;
import jcog.data.bit.MetalBitSet;
import jcog.decide.MutableRoulette;
import nars.Param;
import nars.control.Cause;
import nars.derive.Derivation;
import nars.derive.PreDerivation;
import nars.term.control.PREDICATE;

import java.io.PrintStream;
import java.util.stream.Stream;

/**
 * compiled derivation rules
 * what -> can
 */
public class DeriverRules {

    public final PREDICATE<Derivation> what;

    /**
     * the causes that this is responsible for, ie. those that may be caused by this
     */
    /*@Stable*/ public final Cause[] causes;

    /**
     * repertoire
     */
    private final DeriveAction[] could;

    public final DeriverPlanner planner;

    static short[] what(PreDerivation p) {
        Derivation d = (Derivation)p;
        d.canCollector.clear();
        d.deriver.rules.what.test(d);
        return Util.toShort(d.canCollector.toArray());
    }

    public DeriverRules(PREDICATE<Derivation> what, DeriveAction[] actions, DeriverPlanner planner) {

        this.what = what;

        assert (actions.length > 0);
        this.could = actions;

        this.causes = Stream.of(actions).flatMap(b -> Stream.of(b.cause)).toArray(Cause[]::new);

        this.planner = planner;
    }



    /**
     * choice id to branch id mapping
     */
    private boolean test(Derivation d, int branch) {
        could[branch].run.test(d);
        return d.use(Param.TTL_BRANCH);
    }


    public void run(Derivation d, short[] can) {

        /**
         * weight vector generation
         */
        short[] could = can;

        float[] maybeHow;
        short[] maybeWhat;

        if (could.length > 1) {

            float[] f = Util.map(choice -> this.could[could[choice]].value(d), new float[could.length]);
            int n = f.length;

            MetalBitSet toRemove = null;
            for (int i = 0; i < n; i++) {
                if (f[i] <= 0) {
                    if (toRemove == null) toRemove = MetalBitSet.bits(n);
                    toRemove.set(i);
                }
            }

            if (toRemove == null) {
                maybeWhat = could; //no change
                maybeHow = maybeWhat.length > 1 ? f : null /* not necessary */;

            } else {
                int r = toRemove.cardinality();
                if (r == n) {
                    return; //all removed; nothing remains
                } /*else if (r == n-1) {
                    //TODO all but one
                } */ else {
                    int fanOut = n - r;

                    maybeHow = new float[fanOut];
                    maybeWhat = new short[fanOut];
                    int xx = 0;
                    int i;
                    for (i = 0; i < n; i++) {
                        if (!toRemove.get(i)) {
                            maybeHow[xx] = f[i];
                            maybeWhat[xx++] = could[i];
                        }
                    }
                }
            }

        } else {

            if (could.length == 1 && this.could[could[0]].value(d) > 0) {
                maybeWhat = could;
            } else {
                return;
            }
            maybeHow = null;
        }


        int fanOut = maybeWhat.length; assert(fanOut > 0);

        if (fanOut == 1) {
            test(d, maybeWhat[0]);
        } else {
            //assert((can.length == maybe.length)):  Arrays.toString(could) + " " + Arrays.toString(can) + " " + Arrays.toString(maybe);
            MutableRoulette.run(maybeHow, d.random, wi -> 0, b -> test(d, maybeWhat[b]));
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

    public void printRecursive(PrintStream p) {
        PremiseDeriverCompiler.print(what, p);
        for (Object x : could)
            PremiseDeriverCompiler.print(x, p);
    }


    public void print(PrintStream p) {
        print(p, 0);
    }

    public void print(PrintStream p, int indent) {
        PremiseDeriverCompiler.print(what, p, indent);
        for (Object x : could)
            PremiseDeriverCompiler.print(x, p, indent);
    }




}
