package nars.util.term.builder;

import nars.Op;
import nars.subterm.Subterms;
import nars.term.Term;
import nars.util.term.TermBuilder;

/** stateless implementation */
public class HeapTermBuilder extends TermBuilder {

    public final static HeapTermBuilder the = new HeapTermBuilder();

    HeapTermBuilder() {
    }

    @Override
    public Term compound(Op o, int dt, Term[] u) {
        return theCompound(o, dt, u);
    }

    @Override
    public Subterms subterms(Op inOp, Term... s) {
        return theSubterms(s);
    }
}
