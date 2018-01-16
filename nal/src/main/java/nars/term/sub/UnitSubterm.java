package nars.term.sub;

import nars.term.Term;

/** minimal light-weight wrapper of a single term as a Subterms impl */
public final class UnitSubterm extends AbstractUnitSubterm  {

    private final Term the;

    public UnitSubterm(Term the) {
        this.the = the;
    }

    @Override public Term sub() {
        return the;
    }

}
