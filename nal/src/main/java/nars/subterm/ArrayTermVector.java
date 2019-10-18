package nars.subterm;

import jcog.data.iterator.ArrayIterator;
import nars.term.Term;

import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.IntStream;

/**
 * Holds a vector or tuple of terms.
 * Useful for storing a fixed number of subterms
 */
public class ArrayTermVector extends TermVector {

    /*@NotNull*/
    /*@Stable*/
    private final Term[] terms;

    public ArrayTermVector(/*@NotNull */Term... terms) {
        super(terms);
        this.terms = terms;
        normalized = normalized(this);
    }

    @Override
    public String toString() {
        return Subterms.toString(terms);
    }

    @Override
    public boolean equalTerms(Term[] c) {
        return Arrays.equals(terms, c);
    }

    @Override
    public final boolean equals(/*@NotNull*/ Object obj) {
        if (this == obj) return true;

        if (!(obj instanceof Subterms))
            return false;

        Subterms that = (Subterms) obj;

        //HACK
        //if (!(that instanceof TermVector) && !(that instanceof AnonVector) && !(that instanceof RemappedSubterms.HashCachedRemappedSubterms) && !(that instanceof RemappedSubterms.RepeatedSubterms))
            if (subs()!=that.subs()) //check before computing hashCode
                return false;

        if (hash != that.hashCodeSubterms()) {
            return false;
        }

        if (obj instanceof ArrayTermVector) {

            ArrayTermVector v = (ArrayTermVector) obj;
//            if (terms == v.terms)
//                return true;
//            int n = terms.length;
//            if (v.terms.length != n)
//                return false;
//            for (int i = 0; i < terms.length; i++) {
//                Term a = terms[i];
//                Term b = v.terms[i];
//                if (a == b) continue;
//                if (a.equals(b)) {
//                    v.terms[i] = a; //share copy
//                } else {
//                    return false; //mismatch
//                }
//            }
            return Arrays.equals(terms, v.terms);

        } else {
            final Term[] x = this.terms;
            int s = x.length;
            if (s != that.subs())
                return false;

            for (int i = 0; i < s; i++) {
                if (!x[i].equals(that.sub(i))) {
                    return false;
                }
            }
            return true;

        }

//        if (obj instanceof TermVector)
//            equivalentTo((TermVector) obj);


    }


    @Override
    /*@NotNull*/ public final Term sub(int i) {
        return terms[i];
    }


    @Override
    public final Term[] arrayClone() {
        return terms.clone();
    }

    @Override
    public final Term[] arrayShared() {
        return terms;
    }

    @Override
    public final int subs() {
        return terms.length;
    }

    @Override
    public final Iterator<Term> iterator() {
        return ArrayIterator.iterator(terms);
    }

}
