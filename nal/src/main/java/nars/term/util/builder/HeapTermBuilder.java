package nars.term.util.builder;

import nars.Op;
import nars.term.Term;

/** stateless implementation */
public class HeapTermBuilder extends TermBuilder {

    public final static HeapTermBuilder the = new HeapTermBuilder();

    protected HeapTermBuilder() {

    }

    @Override
    public Term compound(Op o, int dt, Term... u) {
        return theCompound(o, dt, o.sortedIfNecessary(dt, u));
    }


}
