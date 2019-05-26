package nars.term.util.transform;

import nars.Op;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.buffer.TermBuffer;
import nars.term.var.ellipsis.Fragment;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

import static nars.NAL.term.LAZY_COMPOUND_MIN_INTERN_VOL;
import static nars.Op.FRAG;
import static nars.Op.NEG;
import static nars.time.Tense.DTERNAL;

public interface TermTransform extends Function<Term,Term> {

    @Override default Term apply(Term x) {
        return (x instanceof Compound) ?
                applyCompound((Compound) x)
                :
                applyAtomic((Atomic) x);
    }



    default Term applyAtomic(Atomic a) {
        return a;
    }
    default Term applyCompound(Compound c) { return c; }



}
