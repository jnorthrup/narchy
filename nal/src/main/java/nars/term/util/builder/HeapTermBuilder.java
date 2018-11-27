package nars.term.util.builder;

import nars.Op;
import nars.subterm.Subterms;
import nars.term.Term;
import nars.term.util.TermBuilder;

/** stateless implementation */
public class HeapTermBuilder extends TermBuilder {

    public final static HeapTermBuilder the = new HeapTermBuilder();


    @Override
    public Term compound(Op o, int dt, Term[] u) {
        return theCompound(o, dt, o.sortedIfNecessary(dt, u));
    }

    @Override
    public Subterms subterms(Op inOp, Term... u) {
        return theSubterms(u);
    }
}
