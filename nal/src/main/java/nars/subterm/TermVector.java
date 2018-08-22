package nars.subterm;

import nars.The;
import nars.subterm.util.SubtermMetadataCollector;
import nars.subterm.util.TermMetadata;
import nars.term.Term;

import java.util.Iterator;

import static nars.Op.NEG;

/**
 * what differentiates TermVector from TermContainer is that
 * a TermVector specifically for subterms.  while both
 * can be
 */
public abstract class TermVector extends TermMetadata implements Subterms, The {

    private transient boolean normalized;

    protected TermVector(SubtermMetadataCollector s) {
        super(s);
    }

    protected TermVector(Term... terms) {
        super(terms);
    }

    protected void testIfInitiallyNormalized() {
        if (vars() == 0 || testIfInitiallyNormalized(this))
            setNormalized();
    }

    @Override
    public boolean containsNeg(Term x) {
        return x.op() == NEG ? contains(x.unneg()) : (hasAny(NEG) && contains(x.neg()));
    }

    protected void equivalentTo(TermVector that) {


        boolean an, bn = that.normalized;
        if (!(an = this.normalized) && bn)
            this.normalized = true;
        else if (an && !bn)
            that.normalized = true;


    }

    /**
     * if the compound tracks normalization state, this will set the flag internally
     */
    @Override
    public void setNormalized() {
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
        return Subterms.toString(this);
    }



    @Override
    public abstract Iterator<Term> iterator();


    @Override
    abstract public boolean equals(Object obj);


    @Override
    public final int hashCodeSubterms() {
        return hash;
    }

    @Override
    public final int hashCode() {
        return hash;
    }


}
