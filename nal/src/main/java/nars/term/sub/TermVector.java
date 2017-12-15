package nars.term.sub;

import com.google.common.base.Joiner;
import jcog.Util;
import nars.Op;
import nars.Param;
import nars.term.Term;

import java.util.Iterator;

/**
 * what differentiates TermVector from TermContainer is that
 * a TermVector specifically for subterms.  while both
 * can be
 */
public abstract class TermVector implements Subterms {

    /**
     * normal high-entropy "content" hash
     */
    public final int hash;
    /**
     * bitvector of subterm types, indexed by Op.id and OR'd into by each subterm
     * low-entropy, use 'hash' for normal hash operations.
     */
    public final int structure;
    /**
     * stored as volume+1 as if this termvector were already wrapped in its compound
     */
    public final short volume;
    /**
     * stored as complexity+1 as if this termvector were already wrapped in its compound
     */
    public final short complexity;

    public final byte varPattern;
    public final byte varDep;
    public final byte varQuery;
    public final byte varIndep;

    protected transient boolean normalized;

    private static volatile int SERIAL = 0;
    public final int serial = SERIAL++;

    /** copy constructor for fast instantiation */
    protected TermVector(int hash, int structure,
                         byte varPattern, byte varDep, byte varQuery, byte varIndep,
                         short complexity, short volume, boolean normalized) {
        this.hash = hash;
        this.structure = structure;
        this.varPattern = varPattern;
        this.varDep = varDep;
        this.varQuery = varQuery;
        this.varIndep = varIndep;
        this.complexity = complexity;
        this.volume = volume;
        this.normalized = normalized;
    }

    protected TermVector(Term... terms) {

        assert (terms.length <= Param.COMPOUND_SUBTERMS_MAX);

//         if (Param.DEBUG) {
//             for (Term x : terms)
//                 if (x == null) throw new NullPointerException();
//         }

        int structure = 0;
        int vol = 1;
        int varPattern = 0, varQuery = 0, varDep = 0, varIndep = 0;
        int hash = 1;
        for (Term x : terms) {
            int xstructure = x.structure();
            structure |= xstructure;
            if (Op.hasAny(xstructure, Op.VAR_DEP.bit|Op.VAR_QUERY.bit|Op.VAR_INDEP.bit)) {
                varDep += x.varDep();
                varIndep += x.varIndep();
                varQuery += x.varQuery();
            }
            varPattern += x.varPattern();
            vol += x.volume();
            hash = Util.hashCombine(hash, x.hashCode());

        }
        this.hash = hash;
        this.structure = structure;
        this.varPattern = (byte)varPattern;
        this.varDep = (byte)varDep;
        this.varQuery = (byte)varQuery;
        this.varIndep = (byte)varIndep;

        int varTot = varPattern + varQuery + varDep + varIndep;
        final int cmp = vol - varTot;
        this.complexity = (short) (cmp);
        this.volume = (short) (vol);

        this.normalized = varTot == 0;
    }

    @Override
    public int varQuery() {
        return varQuery;
    }

    @Override
    public int varDep() {
        return varDep;
    }

    @Override
    public int varIndep() {
        return varIndep;
    }

    @Override
    public int varPattern() {
        return varPattern;
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
        if (!(an = this.normalized))
            this.normalized = true;
        if (an && !bn)
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
    public void setNormalized() {
        normalized = true;
    }

    @Override
    public boolean isNormalized() {
        return normalized;
    }




    @Override
    public int structure() {
        return structure;
    }

    @Override
    abstract public Term sub(int i);

    @Override
    public final int volume() {
        return volume;
    }

    /**
     * report the term's syntactic complexity
     *
     * @return the complexity value
     */
    @Override
    public final int complexity() {
        return complexity;
    }

    @Override
    public String toString() {
        return '(' + Joiner.on(',').join(arrayClone()) + ')';
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
