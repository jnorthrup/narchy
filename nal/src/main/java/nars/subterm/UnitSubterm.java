package nars.subterm;

import nars.The;
import nars.term.Term;

/** minimal light-weight wrapper of a single term as a Subterms impl */
public final class UnitSubterm extends AbstractUnitSubterm implements The {

    private final Term the;

    public UnitSubterm(Term the) {
        this.the = the;
    }


    @Override public Term sub() {
        return the;
    }


}
