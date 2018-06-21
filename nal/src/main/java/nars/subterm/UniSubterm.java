package nars.subterm;

import nars.The;
import nars.term.Term;

/** minimal light-weight wrapper of a single term as a Subterms impl */
public final class UniSubterm extends AbstractUnitSubterm implements The {

    private final Term the;

    public UniSubterm(Term the) {
        this.the = the;
        //testIfInitiallyNormalized();
    }


    @Override public Term sub() {
        return the;
    }


}
