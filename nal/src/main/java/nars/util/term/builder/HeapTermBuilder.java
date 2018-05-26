package nars.util.term.builder;

import nars.Op;
import nars.subterm.Subterms;
import nars.term.Term;
import nars.util.term.TermBuilder;

public class HeapTermBuilder extends TermBuilder {

    @Override
    protected Term newCompound(Op o, int dt, Term[] u) {
        return compoundInstance(o, dt, u);
    }

    @Override
    public Subterms newSubterms(Op inOp, Term... s) {
        return subtermsInstance(s);
    }
}
