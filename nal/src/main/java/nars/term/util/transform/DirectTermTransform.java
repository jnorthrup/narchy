package nars.term.util.transform;

import nars.Op;
import nars.subterm.Subterms;
import nars.term.Term;
import nars.term.compound.CachedCompound;
import nars.term.util.builder.HeapTermBuilder;
import nars.term.util.builder.TermBuilder;

/** bypasses interning and */
public abstract class DirectTermTransform extends TermTransform.NegObliviousTermTransform {

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
