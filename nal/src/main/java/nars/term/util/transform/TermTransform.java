package nars.term.util.transform;

import nars.NAL;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.buffer.TermBuffer;
import nars.term.util.TermException;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

import static nars.term.atom.Bool.Null;

public interface TermTransform extends Function<Term,Term> {

    @Override default /* final */ Term apply(Term x) {
        return (x instanceof Compound) ?
                applyCompound((Compound) x)
                :
                applyAtomic((Atomic) x);
    }

    default Term applyAtomic(Atomic a) {
        return a;
    }
    default Term applyCompound(Compound c) { return c; }

    /** global default transform procedure: can decide semi-optimal transform implementation */
    static Term transform(Compound x, TermTransform transform, @Nullable TermBuffer l, int volMax) {

        try {
            if (l == null)
                l = new TermBuffer();
            else
                l.clear(); //true, (l.sub.termCount() >= 64) /* HACK */);
            if (l.appendCompound(x, transform, volMax))
                return l.term();
            else
                return Null;
        } catch (TermException t) {
            if (NAL.DEBUG)
                throw t;
            //continue below
        } catch (RuntimeException e) {
            throw new TermException(e.toString(), x);
            //return Null;
        }

        return transform.apply(x);
    }
}
