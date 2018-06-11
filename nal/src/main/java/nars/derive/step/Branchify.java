package nars.derive.step;

import nars.$;
import nars.derive.Derivation;
import nars.term.atom.Atomic;
import nars.term.control.AbstractPred;
import org.roaringbitmap.RoaringBitmap;

/**
 * remembers the possiblity of a derivation branch
 * valid for the premise.  a choice which "can" be tried
 * (ie. according to value rank)
 */
public class Branchify extends AbstractPred<Derivation> {

    private final int id;

    private static final Atomic CAN = Atomic.the("can");

    public Branchify(int id, RoaringBitmap downstream) {
        super($.func(CAN, $.sFast(downstream)));
        this.id = id;
    }

    @Override
    public float cost() {
        return Float.POSITIVE_INFINITY; 
    }

    @Override
    public boolean test(Derivation derivation) {
        derivation.can.add(id);
        return true;
    }
}
