package nars.term.sub;

import jcog.Util;
import nars.term.Term;

import java.util.function.Consumer;
import java.util.function.Predicate;

/** minimal light-weight wrapper of a single term as a Subterms impl */
public class UnitSubterm extends AbstractUnitSubterm  {

    private final Term the;

    public UnitSubterm(Term the) {
        this.the = the;
    }

    @Override public Term sub() {
        return the;
    }

}
