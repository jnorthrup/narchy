package nars.term.util.transform;

import nars.Op;
import nars.subterm.Subterms;
import nars.term.Term;
import nars.term.util.builder.HeapTermBuilder;
import nars.term.util.builder.TermBuilder;

/** bypasses interning */
public abstract class DirectTermTransform extends AbstractTermTransform.NegObliviousTermTransform {

    final static TermBuilder localBuilder = HeapTermBuilder.the;

    @Override
    public Term the(Op op, int dt, Term[] t) {
        return op.the(localBuilder, dt, t);
    }

    @Override
    public Term the(Op op, int dt, Subterms t) {
        return op.the(localBuilder, dt, t);
    }

}
