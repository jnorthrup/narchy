package nars.subterm;

import nars.Idempotent;
import nars.term.Term;

/** minimal light-weight wrapper of a single target as a Subterms impl */
public final class UniSubterm extends AbstractUnitSubterm implements Idempotent {

    private final Term the;

    public UniSubterm(Term the) {
        this.the = the;
        //testIfInitiallyNormalized();
    }


    @Override public Term sub() {
        return the;
    }


}
