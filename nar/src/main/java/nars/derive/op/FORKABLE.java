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
public final class FORKABLE extends AbstractPred<Derivation> {

    public final short[] can;

    private static final Atomic F = Atomic.the("forkable");


//    public Branchify(int id) {
//        this(RoaringBitmap.bitmapOf(id));
//    }

    public FORKABLE(short id) {
        super($.func(F, SETe.the(Int.the(id))));
        this.can = new short[] { id };
    }

    public FORKABLE(RoaringBitmap can) {
        super($.func(F, $.sete(can)));
        this.can = ArrayUtil.toShort(can.toArray());
    }

    @Override
    public boolean test(/*Pre*/Derivation d) {
        d.canCollector.addAll(can);
        return true;
    }
}
