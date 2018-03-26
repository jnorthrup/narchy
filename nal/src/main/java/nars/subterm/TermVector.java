package nars.subterm;

import com.google.common.base.Joiner;
import nars.The;
import nars.subterm.util.TermMetadata;
import nars.term.Term;

import java.util.Iterator;

/**
 * what differentiates TermVector from TermContainer is that
 * a TermVector specifically for subterms.  while both
 * can be
 */
public abstract class TermVector extends TermMetadata implements Subterms, The {

    protected transient boolean normalized;



//    private static volatile int SERIAL = 0;
//    public final int serial = SERIAL++;

//    /** copy constructor for fast instantiation */
//    protected TermVector(int hash, int structure,
//                         byte varPattern, byte varDep, byte varQuery, byte varIndep,
//                         short complexity, short volume, boolean normalized) {
//        super(structure, varPattern, varDep, varQuery, varIndep, complexity, volume, hash);
//        this.normalized = normalized;
//    }

    protected TermVector(Term... terms) {
        super(terms);
        this.normalized = Subterms.super.isNormalized();
    }


    protected void equivalentTo(TermVector that) {
        //EQUIVALENCE---
//            //share since array is equal
//            boolean srcXorY = System.identityHashCode(x) < System.identityHashCode(y);
//            if (srcXorY)
//                that.terms = x;
//            else
//                this.terms = y;
        boolean an, bn = that.normalized;
        if (!(an = this.normalized) && bn)
            this.normalized = true;
        else if (an && !bn)
            that.normalized = true;

//        if (normalized ^ that.normalized) {
//            //one of them is normalized so both must be
//            this.normalized = that.normalized = true;
//        }
        //---EQUIVALENCE
    }

    /**
     * if the compound tracks normalization state, this will set the flag internally
     */
    @Override public void setNormalized() {
        normalized = true;
    }

    @Override
    public boolean isNormalized() {
        return normalized;
    }


    @Override
    abstract public Term sub(int i);

    @Override
    public String toString() {
        return '(' + Joiner.on(',').join(arrayShared()) + ')';
    }


    @Override
    public abstract Iterator<Term> iterator();


    @Override abstract public boolean equals(Object obj);
//        return
//            (this == obj)
//            ||
//            (obj instanceof TermContainer) && equalTerms((TermContainer)obj);
//    }


    @Override
    public final int hashCodeSubterms() {
        return hash;
    }

    @Override
    public int hashCode() {
        return hash;
    }

//    public final boolean visit(@NotNull BiPredicate<Term,Compound> v, Compound parent) {
//        int cl = size();
//        for (int i = 0; i < cl; i++) {
//            if (!v.test(term(i), parent))
//                return false;
//        }
//        return true;
//    }

//    @NotNull
//    public TermContainer reverse() {
//        if (size() < 2)
//            return this; //no change needed
//
//        return TermVector.the( Util.reverse( toArray().clone() ) );
//    }

}
