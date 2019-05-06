package nars.term.util.transform;

import nars.Op;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.compound.LazyCompoundBuilder;
import nars.term.var.ellipsis.EllipsisMatch;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

import static nars.NAL.term.LAZY_COMPOUND_MIN_CONSTANT_INTERN_SPAN;
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

    default boolean apply(Term x, LazyCompoundBuilder out) {

        if (x instanceof Compound) {
            return transformCompound((Compound) x, out);
        } else {
            @Nullable Term y = applyAtomic((Atomic) x);
            if (y == null || y == Bool.Null)
                return false;
            else {
                if (y.op() == FRAG) {
                    Subterms s = y.subterms();
                    if (s.subs() > 0) {
                        Subterms s2 = s.transformSubs(this, null);
                        if (s2 != s) {
                            if (s2 == null)
                                return false;
                            y = new EllipsisMatch(s2);
                        }
                    }
                }
                out.append(y);
                if (y != x)
                    out.changed();
                return true;
            }
        }
    }

    default Term applyAtomic(Atomic a) {
        return a;
    }
    default Term applyCompound(Compound c) { return c; }

    default boolean transformCompound(Compound x, LazyCompoundBuilder out) {
        int c = out.change(), u = out.uniques();
        int p = out.pos();

        Op o = x.op();
        if (o == NEG) {

            out.negStart();

            if (!apply(x.sub(0), out))
                return false;

            out.compoundEnd(NEG);

        } else {
            out.compoundStart(o, o.temporal ? x.dt() : DTERNAL);

            if (!transformSubterms(x.subterms(), out))
                return false;

            out.compoundEnd(o);
        }

        if (out.change()==c && out.pos() - p >= LAZY_COMPOUND_MIN_CONSTANT_INTERN_SPAN) {
            //unchanged constant; rewind and pack the exact Term as an interned symbol
            out.rewind(p, u);
            out.appendAtomic(x);
        }
        return true;
    }

    default boolean transformSubterms(Subterms s, LazyCompoundBuilder out) {
        out.subsStart((byte) s.subs());
        if (s.ANDwithOrdered(this::apply, out)) {
            out.subsEnd();
            return true;
        }
        return false;
    }

}
