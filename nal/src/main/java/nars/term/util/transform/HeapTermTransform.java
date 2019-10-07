package nars.term.util.transform;

import nars.Op;
import nars.subterm.Subterms;
import nars.term.Term;
import nars.term.util.builder.HeapTermBuilder;
import nars.term.util.builder.TermBuilder;

/** bypasses interning */
public abstract class HeapTermTransform extends RecursiveTermTransform.NegObliviousTermTransform {

    final static TermBuilder termBuilder = HeapTermBuilder.the;

    @Override
    public final Term compound(Op op, int dt, Term[] t) {
        return op.the(termBuilder, dt, t);
    }

    @Override
    public final Term compound(Op op, int dt, Subterms t) {
        return op.the(termBuilder, dt, t);
    }

}
