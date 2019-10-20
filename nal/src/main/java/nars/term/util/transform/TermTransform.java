package nars.term.util.transform;

import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;

import java.util.function.Function;
import java.util.function.UnaryOperator;

public interface TermTransform extends UnaryOperator<Term> {

    @Override default   Term apply(Term x) {
        return (x instanceof Compound) ?
                applyCompound((Compound) x)
                :
                applyAtomic((Atomic) x);
    }

    default Term applyAtomic(Atomic a) {
        return a;
    }

    default Term applyCompound(Compound c) {
        return c;
    }

}
