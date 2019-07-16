package nars.derive.op;

import jcog.util.ArrayUtil;
import nars.$;
import nars.derive.model.Derivation;
import nars.term.atom.Atomic;
import nars.term.control.AbstractPred;
import org.roaringbitmap.RoaringBitmap;

/**
 * remembers the possiblity of a derivation branch
 * valid for the premise.  a choice which "can" be tried
 * (ie. according to value rank)
 */
public final class Branchify extends AbstractPred<Derivation> {

    public final short[] can;

    private static final Atomic CAN = Atomic.the("can");


    public Branchify(RoaringBitmap can) {
        super($.func(CAN, $.sete(can)));
        this.can = ArrayUtil.toShort(can.toArray());
    }

    @Override
    public boolean test(/*Pre*/Derivation d) {
        d.canCollector.addAll(can);
        return true;
    }
}
