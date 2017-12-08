package nars.term.anon;

import jcog.Util;
import nars.Op;
import nars.The;
import nars.term.Term;
import nars.term.compound.CachedCompound;
import nars.term.container.Subterms;
import nars.term.container.TermVector;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Consumer;

/**
 * a vector which consists purely of Anom terms
 */
public class AnomVector extends TermVector {

    final byte[] subterms;

    public AnomVector(Term... s) {
        super(s); //TODO optimize this for certain Anom invariants (ie. no variables etc)
        byte[] t = subterms = new byte[s.length];
        for (int i = 0, sLength = s.length; i < sLength; i++) {
            t[i] = (byte) ((Anom) s[i]).id;
        }

        normalized = true;

    }

    @Override
    public final Term sub(int i) {
        return Anom.the[subterms[i]];
    }

    @Override
    public int subs() {
        return subterms.length;
    }


    public int indexOf(Anom t) {
        return ArrayUtils.indexOf(subterms, (byte)(t.id));
    }

    @Override
    public int indexOf(Term t) {
        if (t instanceof Anom)
            return indexOf((Anom) t);
        else
            return -1; //super.indexOf(t);
    }

    @Override
    public boolean contains(Term t) {
        if (t instanceof Anom)
            return indexOf((Anom) t) != -1;
        else
            return false; //super.contains(t);
    }

    @Override
    public boolean containsRecursively(Term t) {
        return contains(t); //since it will be flat
    }

    @Override
    public Iterator<Term> iterator() {
        return new AnomArrayIterator(subterms);
    }

    public static boolean valid(Term[] xx) {
        return Util.and(Anom.class::isInstance, xx);
    }

    @Override
    public void forEach(Consumer<? super Term> action, int start, int stop) {
        for (int i = start; i < stop; i++)
            action.accept(Anom.the[subterms[i]]);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;

        if (this.hash != obj.hashCode()) return false;

        if (obj instanceof AnomVector) {
            return Arrays.equals(subterms, ((AnomVector) obj).subterms);
        }

        if (obj instanceof Subterms) {


            Subterms ss = (Subterms) obj;
            int s = subterms.length;
            if (ss.subs() != s)
                return false;
            for (int i = 0; i < s; i++) {
                Term y = ss.sub(i);
                if (!(y instanceof Anom) || !sub(i).equals(y))
                    return false;
            }
            return true;

        }
        return false;
    }

    public Term reverse(Op o, Anon anon) {
        int n = subterms.length;
        Term[] yy = new Term[n];
        for (int i = 0; i < n; i++)
            yy[i] = anon.rev.get(subterms[i]);
        return new CachedCompound(o, The.subterms(yy));
    }
}
