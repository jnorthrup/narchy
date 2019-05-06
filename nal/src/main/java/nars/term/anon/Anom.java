package nars.term.anon;

import jcog.Util;
import nars.Op;
import nars.io.IO;
import nars.term.Neg;
import nars.term.Term;

import static nars.Op.ATOM;

/* indexed anonymous target */
public final class Anom extends Intrin {

    private static final byte ANOM_SUBTYPE = 1;
    private static final int ANOM_OPX = Term.opX(ATOM, ANOM_SUBTYPE);
    private static final byte ANOM_HEADER = IO.opAndSubType(ATOM, ANOM_SUBTYPE);

    private final byte[] bytes;


    private Anom(byte i) {
        super(i);
        this.bytes = new byte[] { ANOM_HEADER, i };
    }

    @Override
    public byte[] bytes() {
        return bytes;
    }

    @Override
    public int opX() {
        return ANOM_OPX;
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

    static final Anom[] the = Util.map(0, Byte.MAX_VALUE, Anom[]::new, (i) -> new Anom((byte) i));
    private static final Term[] theNeg = Util.map(0, Byte.MAX_VALUE, Term[]::new, (i) -> Neg.neg(the[i]));
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
