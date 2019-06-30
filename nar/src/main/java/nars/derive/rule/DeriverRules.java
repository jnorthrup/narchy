package nars.derive.rule;

import jcog.data.bit.MetalBitSet;
import jcog.decide.MutableRoulette;
import jcog.pri.Possibilities;
import nars.NAL;
import nars.control.Why;
import nars.derive.model.Derivation;
import nars.term.control.PREDICATE;
import nars.truth.Truth;
import nars.truth.func.TruthFunc;

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
    private final DeriveAction[] branch;

    public final PreDeriver pre;

//    /** structure vector of operator types which must not be anonymized in premise formation */
//    public final int mustAtomize;

    DeriverRules(PREDICATE<Derivation> what, DeriveAction[] actions, PreDeriver pre) {

        this.what = what;

        this.branch = actions; assert (actions.length > 0);

        this.why = Stream.of(actions).flatMap(b -> Stream.of(b.why)).toArray(Why[]::new);

        this.pre = pre;
    }

    public boolean run(Derivation d, final int deriveTTL) {

        short[] maybe = d.deriver.what(d);

        if (maybe.length == 0)
            return false;

        d.preReady();


        float[] pri;
        short[] can;

        if (maybe.length == 1) {
            if (this.branch[maybe[0]].value(d) <= 0)
                return false;

            can = maybe;
            pri = null; //not used

        } else /* could.length > 1 */ {

            int n = maybe.length;
            float[] f = new float[n];
            MetalBitSet toRemove = null;
            for (int choice = 0; choice < n; choice++) {
                float fc = this.branch[maybe[choice]].value(d);
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

        d.ready(maybe, deriveTTL);

        if (can.length == 1) {
            branch[can[0]].test(d);
        } else {

            MutableRoulette.run(pri, d.random, wi -> 0, i -> branch[can[i]].test(d));

            //untested:
//            Possibilities<Derivation,Void> pp = new Possibilities(d);
//            for (int i = 0, canLength = can.length; i < canLength; i++) {
//                short c = can[i];
//                if (branch[c].truth.test(d))
//                    pp.add(new TruthifyPossibility(d, branch[c].conclusion, c, pri[i]));
//            }
//            pp.commit(false, true);
//            pp.execute(d::live, 0.5f);

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
        for (Object x : branch)
            PremiseRuleCompiler.print(x, p);
    }


    public void print(PrintStream p) {
        print(p, 0);
    }

    public void print(PrintStream p, int indent) {
        PremiseRuleCompiler.print(what, p, indent);
        for (Object x : branch)
            PremiseRuleCompiler.print(x, p, indent);
    }


    private static class TruthifyPossibility extends Possibilities.Possibility<Derivation, Void> {

        private final Derivation d;
        private final short c;
        private final float value;
        private final PREDICATE<Derivation> conclusion;
        byte punc;
        Truth t;
        TruthFunc tf;
        boolean single;

        public TruthifyPossibility(Derivation d, PREDICATE<Derivation> cc, short c, float v) {
            this.d = d;
            this.c = c;
            punc = d.concPunc;
            t = d.concTruth;
            tf = d.truthFunction;
            single = d.concSingle;
            this.conclusion = cc;
            this.value = v + ( t != null ? t.conf() : 0 /* biased against questions */);
        }

        @Override
        public Void apply(Derivation dd) {
            dd.concTruth = t;
            dd.concPunc = punc;
            dd.concSingle = single;
            dd.truthFunction = tf;

            dd.use(NAL.derive.TTL_COST_BRANCH);

            conclusion.test(dd);
            return null;
        }

        @Override
        public float value() {
            return value;
        }
    }
}
