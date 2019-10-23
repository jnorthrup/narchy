package nars.subterm;

import jcog.data.iterator.ArrayIterator;
import jcog.util.ArrayUtil;
import nars.term.Term;

import java.util.Iterator;

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
    public final boolean equals(/*@NotNull*/ Object obj) {
        if (this == obj) return true;

        if (!(obj instanceof Subterms))
            return false;

        Subterms that = (Subterms) obj;

//        //HACK
//        //if (!(that instanceof TermVector) && !(that instanceof AnonVector) && !(that instanceof RemappedSubterms.HashCachedRemappedSubterms) && !(that instanceof RemappedSubterms.RepeatedSubterms))
//            if (subs()!=that.subs()) //check before computing hashCode
//                return false;

        if (hash != that.hashCodeSubterms())
            return false;

        if (obj instanceof ArrayTermVector)
            return ArrayUtil.equalArraysDirect(terms, ((ArrayTermVector) obj).terms);
        else
            return equalTerms(that);


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
