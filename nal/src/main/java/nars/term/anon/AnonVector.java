package nars.term.anon;

import nars.Op;
import nars.term.Term;
import nars.term.sub.Subterms;
import nars.term.sub.TermVector;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Predicate;

import static nars.Op.NEG;

/**
 * a vector which consists purely of AnonID terms
 */
public class AnonVector extends TermVector {

    final short[] subterms;

    /** assumes the array contains only AnonID instances */
    public AnonVector(Term... s) {
        super(s); //TODO optimize this for certain Anom invariants (ie. no variables etc)
        short[] t = subterms = new short[s.length];
        for (int i = 0, sLength = s.length; i < sLength; i++) {
            Term ss = s[i];
            boolean neg;
            if (ss.op()==NEG) {
                ss = ss.unneg();
                neg = true;
            } else {
                neg = false;
            }
            short tt = ((AnonID) ss).anonID();
            if (neg)
                tt = (short) -tt;
            t[i] = tt;
        }

    }

    @Override
    public final Term sub(int i) {
        short tt = subterms[i];
        assert(tt!=0);
        return (tt > 0) ? AnonID.idToTerm(tt) : AnonID.idToTerm((short) -tt).neg();
    }


    @Override
    public int subs() {
        return subterms.length;
    }

//    @Override
//    public boolean unifyLinear(Subterms Y, Unify u) {
//        if (Y instanceof AnonVector) {
//            short[] x = subterms;
//            short[] y = ((AnonVector)Y).subterms;
//            //TODO
//        } else {
//            return super.unifyLinear(Y, u);
//        }
//    }

    int indexOf(AnonID t, boolean neg) {
        short id = t.anonID();
        if (neg)
            id = (short)(-id);
        return ArrayUtils.indexOf(subterms, id);
    }

    @Override
    public int indexOf(Term t) {
        boolean neg = false;
        if (t.op()==NEG) {
            t = t.unneg();
            neg = true;
        }
        if (t instanceof AnonID)
            return indexOf((AnonID) t, neg);
        else
            return -1; //super.indexOf(t);
    }

    @Override
    public boolean contains(Term t) {
        return indexOf(t)!=-1;
    }

    @Override
    public boolean containsRecursively(Term t) {
        if (contains(t))
            return true;
        return (t.op() == NEG) || (anyNeg() && contains(t.neg())); //TODO write absolute matcher in one pass
    }

    private boolean anyNeg() {
        return (structure&NEG.bit) != 0;
//        for (short s : subterms)
//            if (s < 0) return true;
//        return false;
    }

    @Override
    public Iterator<Term> iterator() {
        return new AnonArrayIterator(subterms);
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;

        if (this.hash != obj.hashCode()) return false;

        if (obj instanceof AnonVector) {
            return Arrays.equals(subterms, ((AnonVector) obj).subterms);
        }

        if (obj instanceof Subterms) {


            Subterms ss = (Subterms) obj;
            int s = subterms.length;
            if (ss.subs() != s)
                return false;
            for (int i = 0; i < s; i++) {
                Term y = ss.sub(i);
                if (!(y instanceof AnonID) || !sub(i).equals(y))
                    return false;
            }
            return true;

        }
        return false;
    }

}
