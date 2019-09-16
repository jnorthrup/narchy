package nars.derive.op;

import jcog.util.ArrayUtil;
import nars.$;
import nars.derive.Derivation;
import nars.term.atom.Atomic;
import nars.term.atom.Int;
import nars.term.control.AbstractPred;
import org.roaringbitmap.RoaringBitmap;

import static nars.Op.SETe;

/**
 * remembers the possiblity of a derivation branch
 * valid for the premise.  a choice which "can" be tried
 * (ie. according to value rank)
 */
public final class Branchify extends AbstractPred<Derivation> {

    public final short[] can;

    private static final Atomic CAN = Atomic.the("can");


//    public Branchify(int id) {
//        this(RoaringBitmap.bitmapOf(id));
//    }

    public Branchify(short id) {
        super($.func(CAN, SETe.the(Int.the(id))));
        this.can = new short[] { id };
    }

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
