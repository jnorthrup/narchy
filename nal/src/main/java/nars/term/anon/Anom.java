package nars.term.anon;

import jcog.Util;
import nars.Op;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.AtomicConst;
import nars.term.atom.Int;

import static nars.Op.ATOM;

/* indexed anonymous term */
public final class Anom extends Int implements AnonID {

    final static int MAX_ANOM = 127;
    final static int ANOM = Term.opX(ATOM, 0);

    Anom(byte i) {
        super(i, AtomicConst.bytes(ATOM, i));
    }

    @Override
    public int opX() {
        return ANOM;
    }

    @Override
    public /**/ Op op() {
        return ATOM;
    }

    @Override
    public short anonID() {
        return (short) id; //since ATOM_MASK is zero, it is just the lowest 8 bits of the 'id' int
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public final boolean equals(Object obj) {
        return this == obj;
    }

    //optimized compareTo for frequent Anom->Anom cases
    @Override
    public int compareTo(Termed yy) {
        if (this == yy) return 0;

        Term y = yy.term();
        if (y instanceof Anom) {
            return Integer.compare(id, ((Anom) y).id);
        } else {
            int vc = Integer.compare(y.volume(), this.volume());
            if (vc != 0)
                return vc;

            int oc = Integer.compare(this.opX(), y.opX());
            assert (oc != 0);
            return oc;
        }
        //return super.compareTo(yy);
    }

    static Anom[] the = Util.map(0, MAX_ANOM, (i) -> new Anom((byte) i), Anom[]::new);

    public static Anom the(int i) {
        return the[i];
    }


}
