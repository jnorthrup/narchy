package nars.term.anon;

import jcog.Util;
import nars.Op;
import nars.io.IO;
import nars.term.Neg;
import nars.term.Term;

import static nars.Op.ATOM;

/* indexed anonymous target */
public final class Anom extends IntrinAtomic {

    private final byte[] bytes;


    private Anom(byte i) {
        super(i);
        this.bytes = new byte[] {IO.opAndSubType(ATOM, (byte) 1), i };
    }

    @Override
    public byte[] bytes() {
        return bytes;
    }

    @Override
    public /**/ Op op() {
        return ATOM;
    }

    @Override
    public String toString() {
        return '_' +  Integer.toString(i);
    }




    //    @Override
//    public int compareTo(Term yy) {
//        if (this == yy) return 0;
//
//        Term y = yy.target();
//        if (y instanceof Anom) {
//            return Integer.compare(id, ((Int) y).id);
//        } else {
//            return +1;
////            int vc = Integer.compare(y.volume(), this.volume());
////            if (vc != 0)
////                return vc;
////
////            int oc = Integer.compare(this.opX(), y.opX());
////            assert (oc != 0);
////            return oc;
//        }
//    }

    /** intrinsic anom */
    private static final Anom[] the =
        Util.map(0, Byte.MAX_VALUE, Anom[]::new, i -> new Anom((byte) i));

    /** intrinsic anoms negated */
    private static final Neg.NegIntrin[] theNeg =
        Util.map(0, Byte.MAX_VALUE, Neg.NegIntrin[]::new, i -> new Neg.NegIntrin(the[i]));

    static {
        the[0] = null;
        theNeg[0] = null;
    }

    public static Anom the(int i) {
        return the[i];
    }

    @Override
    public final Term neg() {
        return theNeg[i];
    }
}
