package nars.derive.rule;

import jcog.Util;
import jcog.data.bit.MetalBitSet;
import jcog.decide.MutableRoulette;
import nars.NAL;
import nars.control.Why;
import nars.derive.model.Derivation;
import nars.term.control.PREDICATE;

import java.io.PrintStream;
import java.util.stream.Stream;

/**
 * compiled derivation rules
 * what -> can
 * TODO subclass to Weighted deriver runner; and make a non-weighted subclass
 */
public class DeriverRules {

    public final PREDICATE<Derivation> what;

    /**
     * the causes that this is responsible for, ie. those that may be caused by this
     */
    /*@Stable*/ public final Why[] why;

    /**
     * repertoire
     */
    private final DeriveAction[] could;

    public final PreDeriver pre;

//    /** structure vector of operator types which must not be anonymized in premise formation */
//    public final int mustAtomize;

    DeriverRules(PREDICATE<Derivation> what, DeriveAction[] actions, PreDeriver pre) {

        this.what = what;

        assert (actions.length > 0);
        this.could = actions;

        this.why = Stream.of(actions).flatMap(b -> Stream.of(b.why)).toArray(Why[]::new);

        this.pre = pre;

//        this.mustAtomize = mustAtomize;

    }

    /**
     * choice id to branch id mapping
     */
    private boolean test(Derivation d, int branch) {
        could[branch].run.test(d);
        return d.use(NAL.derive.TTL_COST_BRANCH);
    }

    public boolean run(Derivation d, final int deriveTTL) {

        short[] maybe = d.deriver.what(d);

        if (maybe.length == 0)
            return false;

        d.preReady();


        /**
         * weight vector generation
         */
        float[] pri;
        short[] can;

        if (maybe.length == 1) {
            if (this.could[maybe[0]].value(d) <= 0)
                return false;

            can = maybe;
            pri = null; //not used

        } else /* could.length > 1 */ {

            int n = maybe.length;
            float[] f = new float[n];
            MetalBitSet toRemove = null;
            for (int choice = 0; choice < n; choice++) {
                float fc = this.could[maybe[choice]].value(d);
                if (fc <= 0) {
                    if (toRemove == null) toRemove = MetalBitSet.bits(n);
                    toRemove.set(choice);
                }
                f[choice] = fc;
            }

            if (toRemove == null) {
                can = maybe; //all
                pri = maybe.length > 1 ? f : null;
            } else {
                int r = toRemove.cardinality();
                if (r == n)
                    return false; //all removed; nothing remains
                 /*else if (r == n-1) {*/
                    //TODO all but one

                int nn = n - r;

                pri = new float[nn];
                can = new short[nn];
                int nc = 0;
                for (int i = 0; i < n; i++) {
                    if (toRemove.getNot(i)) {
                        pri[nc] = f[i];
                        can[nc++] = maybe[i];
                    }
                }
            }

        }

        d.ready(maybe,
            deriveTTL
            //Util.lerp(Math.max(d.priDouble, d.priSingle), Param.TTL_MIN, deriveTTL)
        );

        int fanOut = can.length; //assert(fanOut > 0);

        if (fanOut == 1) {
            test(d, can[0]);
        } else {

            Util.normalizeMargin(1f / pri.length, 0, pri);

//            if (maybePri[0]!=maybePri[1])
//                System.out.println(Arrays.toString(maybePri));

            //assert((can.length == maybe.length)):  Arrays.toString(could) + " " + Arrays.toString(can) + " " + Arrays.toString(maybe);
            MutableRoulette.run(pri, d.random, wi -> 0, i -> test(d, can[i]));
        }

        return true;
    }

    /**
     * the conclusions that in which this deriver can result
     */
    public Why[] causes() {
        return why;
    }

    public void printRecursive() {
        printRecursive(System.out);
    }

    public void printRecursive(PrintStream p) {
        PremiseRuleCompiler.print(what, p);
        for (Object x : could)
            PremiseRuleCompiler.print(x, p);
    }


    public void print(PrintStream p) {
        print(p, 0);
    }

    public void print(PrintStream p, int indent) {
        PremiseRuleCompiler.print(what, p, indent);
        for (Object x : could)
            PremiseRuleCompiler.print(x, p, indent);
    }




}
