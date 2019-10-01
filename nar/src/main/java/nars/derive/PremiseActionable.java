package nars.derive;

import jcog.decide.MutableRoulette;
import nars.NAL;
import nars.derive.action.PremiseAction;
import nars.derive.action.PremisePatternAction;
import nars.truth.MutableTruth;

import java.util.function.Predicate;

/** recycled slot describing potential deriver action */
public class PremiseActionable  implements Predicate<Derivation> {

    public final MutableTruth truth = new MutableTruth();

    public transient float pri;
    public transient byte punc;

    public transient boolean single;
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

        PremiseAction a = this.action;

        if (NAL.TRACE)
            a.trace(d);

        if (a instanceof PremisePatternAction.TruthifyDeriveAction)
            d.ready(truth, punc, single);

        a.run(d);

        return d.use(NAL.derive.TTL_COST_BRANCH);
    }



}
