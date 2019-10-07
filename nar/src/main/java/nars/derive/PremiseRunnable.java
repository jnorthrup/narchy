package nars.derive;

import nars.derive.action.PremiseAction;
import nars.truth.MutableTruth;

import java.util.function.Predicate;

/** recycled slot describing potential deriver action.
 *  local to Derivation
 * */
public class PremiseRunnable {

    public final MutableTruth truth = new MutableTruth();

    public transient float pri;
    public transient byte punc;

    public transient boolean single;
    public transient PremiseAction action;


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


}
