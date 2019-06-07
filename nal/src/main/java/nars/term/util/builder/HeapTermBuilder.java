package nars.term.util.builder;

import nars.Op;
import nars.subterm.Subterms;
import nars.term.Term;
import nars.term.atom.Atom;

/** stateless implementation */
public class HeapTermBuilder extends TermBuilder {

    public final static HeapTermBuilder the = new HeapTermBuilder();

    protected HeapTermBuilder() {

    }

    @Override public Term compound(Op o, int dt, Subterms t) {
        return newCompound(o, dt, o.sortedIfNecessary(dt, t));
    }
    @Override public Term compound(Op o, int dt, Term... u) {
        return newCompound(o, dt, o.sortedIfNecessary(dt, u));
    }

    @Override
    public Atom atom(String id) {
        return new Atom(id);
    }
}
