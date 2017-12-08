package nars.term.anon;

import nars.Op;
import nars.term.Term;
import nars.term.var.AbstractVariable;

/**
 * indicates the term has an anonymous, canonical identifier (16-bit short)
 */
public interface AnonID extends Term {

    short anonID();

    default byte anonNum() {
        return (byte) (anonID() & 0xff);
    }

    short ATOM_MASK = 0 << 8;
    short VARDEP_MASK = 1 << 8;
    short VARINDEP_MASK = 2 << 8;
    short VARQUERY_MASK = 3 << 8;
    short VARPATTERN_MASK = 4 << 8;

    static short termToId(Op o, byte id) {
        short mask;
        switch (o) {
            case ATOM:
                mask = ATOM_MASK;
                break;
            case VAR_DEP:
                mask = VARDEP_MASK;
                break;
            case VAR_INDEP:
                mask = VARINDEP_MASK;
                break;
            case VAR_QUERY:
                mask = VARQUERY_MASK;
                break;
            case VAR_PATTERN:
                mask = VARPATTERN_MASK;
                break;
            default:
                throw new UnsupportedOperationException();
        }
        return (short) (mask | id);
    }


    static Term idToTerm(short i) {
        byte num = (byte) (i & 0xff);
        int m = ((i & 0xff00));
        if (m == ATOM_MASK) {
            return Anom.the[num];
        } else {
            Op o;
            switch (m) {
                case VARDEP_MASK:
                    o = Op.VAR_DEP;
                    break;
                case VARINDEP_MASK:
                    o = Op.VAR_INDEP;
                    break;
                case VARQUERY_MASK:
                    o = Op.VAR_QUERY;
                    break;
                case VARPATTERN_MASK:
                    o = Op.VAR_PATTERN;
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
            return AbstractVariable.the(o, num);
        }
    }


}
