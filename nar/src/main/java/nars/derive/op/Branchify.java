package nars.derive.op;

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

    public final RoaringBitmap can;

    private static final Atomic CAN = Atomic.the("can");


    public Branchify(RoaringBitmap can) {
        super($.func(CAN, $.sete(can)));
        this.can = can;
    }

    @Override
    public boolean test(/*Pre*/Derivation derivation) {
        derivation.canCollector.or(can);
        return true;
    }
}
