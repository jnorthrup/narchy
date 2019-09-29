package nars.derive;

import jcog.Texts;
import jcog.data.list.FasterList;
import jcog.decide.MutableRoulette;
import nars.$;
import nars.NAL;
import nars.Op;
import nars.derive.action.PatternPremiseAction;
import nars.derive.action.PremiseAction;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.control.AND;
import nars.term.control.AbstractPred;
import nars.term.control.PREDICATE;
import nars.truth.MutableTruth;
import nars.truth.PreciseTruth;

import java.util.function.Predicate;

/** recycled slot describing potential deriver action */
public class PremiseActionable  implements Predicate<Derivation> {

    public final MutableTruth truth = new MutableTruth();

    public transient float pri;
    private transient byte punc;

    private transient boolean single;
    public transient PremiseAction action;



    public static void runDirect(int valid, int lastValid, Derivation d) {


        PremiseActionable[] post = d.post;
        if (valid == 1) {//optimized 1-option case
            //while (post[lastValid].run()) { }
            post[lastValid].test(d);
        } else {//


            float[] pri = new float[valid];
            for (int i = 0; i < valid; i++)
                pri[i] = post[i].pri;
            MutableRoulette.run(pri, d.random, wi -> 0, i -> post[i].test(d));

            //alternate roulette:
            //  int j; do { j = Roulette.selectRoulette(valid, i -> post[i].pri, d.random);   } while (post[j].run());
        }

    }

//    public static <X> PREDICATE<X> andSeq(FasterList<PREDICATE<X>> seq) {
//        //TODO
//        return new PREDICATE() {
//
//        };X x) -> {
//            for (int i = 0, programSize = seq.size(); i < programSize; i++) {
//                PREDICATE<X> si = seq.get(i);
//                if (!si.test(x))
//                    return false;
//            }
//            return true;
//        }
//    }


    public float pri(PremiseAction a, Derivation d) {
        float p = a.pri(d);
        if (p > Float.MIN_NORMAL) {
            this.action = a;
            this.truth.set( d.truth );
            this.punc = d.punc;
            this.single = d.single;
            return this.pri = p;
        } else {
            this.action = null;
            this.pri = 0;
            return 0;
        }
    }

    @Override public final boolean test(Derivation d) {
        d.apply(truth, punc, single);

        if (NAL.TRACE) {
            System.out.println("$" + Texts.n4(action.pri(d)) + " " + action + "\t" + d);
        }
        action.run(d);

        return d.use(NAL.derive.TTL_COST_BRANCH);
    }

    static final Atom PREMISE_ACTION = Atomic.atom(PremiseActionableInit.class.getSimpleName());

    public final void compile(FasterList<PREDICATE<Derivation>> tgt) {


        if (action instanceof PatternPremiseAction.TruthifyDeriveAction) {
            tgt.add(new PremiseActionableInit(this));

            PREDICATE<Derivation> aa = ((PatternPremiseAction.TruthifyDeriveAction) action).action;
            if (aa instanceof AND)
                aa.subterms().addAllTo(tgt);
            else
                tgt.add(aa);

            //TODO compiling FORKs?

        } else {
            tgt.add(action);
        }

    }

    private static final class PremiseActionableInit extends AbstractPred<Derivation> {
        private final PreciseTruth _truth;
        private final byte punc;
        private final boolean single;

        public PremiseActionableInit(PremiseActionable p) {
            this(p.punc, p.single, p.truth.clone());
        }

        public PremiseActionableInit(byte punc, boolean single, PreciseTruth t) {
            super($.funcFast(PremiseActionable.PREMISE_ACTION,
                $.the(single),
                t!=null ? $.p($.quote(t.freq()), $.quote(t.conf()), Op.puncAtom(punc)) : Op.puncAtom(punc) ));
            this.punc = punc;
            this.single = single;
            this._truth = t;
        }

        @Override
        public boolean test(Derivation d) {
            d.apply(_truth, punc, single);
            return true;
        }
    }
}
