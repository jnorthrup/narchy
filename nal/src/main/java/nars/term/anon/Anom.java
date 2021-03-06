package nars.term.anon;

import jcog.Util;
import nars.Op;
import nars.io.IO;
import nars.term.Neg;
import nars.term.Term;

import java.util.function.IntFunction;

import static nars.Op.ATOM;

/* indexed anonymous target */
public final class Anom extends IntrinAtomic {

    private final byte[] bytes;


    private Anom(byte i) {
        super((short) i);
        this.bytes = new byte[] {IO.opAndEncoding(ATOM, (byte) 1), i };
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
        return '_' +  Integer.toString((int) i);
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
        Util.map(0, (int) Byte.MAX_VALUE, Anom[]::new, new IntFunction<Anom>() {
            @Override
            public Anom apply(int i) {
                return new Anom((byte) i);
            }
        });

    /** intrinsic anoms negated */
    private static final Neg.NegIntrin[] theNeg =
        Util.map(0, (int) Byte.MAX_VALUE, Neg.NegIntrin[]::new, new IntFunction<Neg.NegIntrin>() {
            @Override
            public Neg.NegIntrin apply(int i) {
                return new Neg.NegIntrin(the[i]);
            }
        });

    static {
        the[0] = null;
        theNeg[0] = null;
    }

    public static Anom the(int i) {
        return the[i];
    }

    @Override
    public final Term neg() {
        return theNeg[(int) i];
    }
}
