package nars.term.anon;

import nars.term.Term;
import nars.term.container.Subterms;
import nars.term.container.TermVector;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.Iterator;

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
            t[i] = ((AnonID) s[i]).anonID();
        }

        normalized = true;
    }

    @Override
    public final Term sub(int i) {
        return AnonID.idToTerm(subterms[i]);
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

    public int indexOf(AnonID t) {
        return ArrayUtils.indexOf(subterms, t.anonID());
    }

    @Override
    public int indexOf(Term t) {
        if (t instanceof AnonID)
            return indexOf((AnonID) t);
        else
            return -1; //super.indexOf(t);
    }

    @Override
    public boolean contains(Term t) {
        if (t instanceof AnonID)
            return indexOf((AnonID) t) != -1;
        else
            return false; //super.contains(t);
    }

    @Override
    public boolean containsRecursively(Term t) {
        return contains(t); //since it will be flat
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
