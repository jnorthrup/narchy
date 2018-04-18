package nars.derive.step;

import nars.$;
import nars.derive.Derivation;
import nars.term.control.AbstractPred;
import org.roaringbitmap.RoaringBitmap;

/**
 * remembers the possiblity of a derivation branch
 * valid for the premise.  a choice which "can" be tried
 * (ie. according to value rank)
 */
public class Branchify extends AbstractPred<Derivation> {

    public final int id;

//        /**
//         * global cause channel ID's that this leads to
//         */
//        private final RoaringBitmap downstream;

    public Branchify(int id, RoaringBitmap downstream) {
        super($.func("can", /*$.the(id),*/ $.sFast(downstream)));
        this.id = id;
//            this.downstream = downstream;
    }

    @Override
    public float cost() {
        return Float.POSITIVE_INFINITY; //post-condition: must be the last element in any sequence
    }

    @Override
    public boolean test(Derivation derivation) {
        derivation.can.add(id);
        return true;
    }
}
