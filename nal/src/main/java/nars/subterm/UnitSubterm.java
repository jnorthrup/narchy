package nars.subterm;

import nars.The;
import nars.term.Term;

/** minimal light-weight wrapper of a single term as a Subterms impl */
public final class UnitSubterm extends AbstractUnitSubterm implements The {

    private final Term the;

    boolean normalized;

    public UnitSubterm(Term the) {
        this.the = the;
        this.normalized = super.isNormalized();
    }

    @Override public Term sub() {
        return the;
    }

    @Override
    public boolean isNormalized() {
        return normalized;
    }

    @Override
    public void setNormalized() {
        normalized = true;
    }
}
