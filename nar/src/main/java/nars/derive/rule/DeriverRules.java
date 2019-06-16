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

    public void run(Derivation d, short[] can, final int deriveTTL) {
        d.ready(
                can, deriveTTL
                //Util.lerp(Math.max(d.priDouble, d.priSingle), Param.TTL_MIN, deriveTTL)
        );

        /**
         * weight vector generation
         */
        short[] could = can;

        float[] maybePri;
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
                maybePri = maybeWhat.length > 1 ? f : null /* not necessary */;

            } else {
                int r = toRemove.cardinality();
                if (r == n) {
                    return; //all removed; nothing remains
                } /*else if (r == n-1) {
                    //TODO all but one
                } */ else {
                    int fanOut = n - r;

                    maybePri = new float[fanOut];
                    maybeWhat = new short[fanOut];
                    int xx = 0;
                    for (int i = 0; i < n; i++) {
                        if (toRemove.getNot(i)) {
                            maybePri[xx] = f[i];
                            maybeWhat[xx++] = could[i];
                        }
                    }
                }
            }

        } else {

            if (could.length == 1) {//{ && this.could[could[0]].value(d) > 0) {
                maybeWhat = could;
            } else {
                return;
            }
            maybePri = null; //unnecessary
        }


        int fanOut = maybeWhat.length; //assert(fanOut > 0);

        if (fanOut == 1) {
            test(d, maybeWhat[0]);
        } else {

            Util.normalizeMargin(1f/maybePri.length, 0, maybePri);

//            if (maybePri[0]!=maybePri[1])
//                System.out.println(Arrays.toString(maybePri));

            //assert((can.length == maybe.length)):  Arrays.toString(could) + " " + Arrays.toString(can) + " " + Arrays.toString(maybe);
            MutableRoulette.run(maybePri, d.random, wi -> 0, i -> test(d, maybeWhat[i]));
        }

        d.nar.emotion.premiseTTL_used.recordValue(Math.max(0, deriveTTL - d.ttl)); //TODO handle negative amounts, if this occurrs.  limitation of HDR histogram

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
