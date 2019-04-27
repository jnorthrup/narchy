package nars.term.functor;

import nars.Op;
import nars.eval.Evaluation;
import nars.subterm.Subterms;
import nars.term.Functor;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.Bool;
import org.jetbrains.annotations.Nullable;

import static nars.term.atom.Bool.True;

/** UnaryBidiFunctor with constant-like parameter */
public abstract class UnaryParametricBidiFunctor extends Functor {

    public UnaryParametricBidiFunctor(Atom atom) {
        super(atom);
    }

    public UnaryParametricBidiFunctor(String atom) { super(atom); }

    @Nullable
    @Override
    public final Term apply(Evaluation e, Subterms terms) {
        int s = terms.subs();
        switch (s) {
            case 2:
                return apply1(terms.sub(0), terms.sub(1));
            case 3:
                return apply2(e, terms.sub(0), terms.sub(1), terms.sub(2));
            default:
                return Bool.Null;
        }
    }

    Term apply1(Term x, Term parameter) {
        return !x.op().var ? compute(x, parameter) : null;
    }

    protected abstract Term compute(Term x, Term parameter);

    /** override for reversible functions, though it isnt required */
    protected Term uncompute(Evaluation e, Term x, Term param, Term y) {
        return null;
    }

    Term apply2(Evaluation e, Term x, Term param, Term y) {
        boolean xVar = x.op().var;
        if (y.op().var) {

            if (xVar) {
                return null;
            } else {
                Term XY = compute(x, param);
                if (XY!=null) {
                    return e.is(y, XY) ? null : Bool.Null;
                } else {
                    return null;
                }
            }
        } else {
            if (x.hasAny(Op.Variable)) {
                Term X = uncompute(e, x, param, y);
                if (X!=null) {
                    return e.is(x, X) ? null : Bool.Null;
                } else {
                    return null;
                }
            } else {

                Term XY = compute(x, param);
                if (XY != null) {
                    return XY.equals(y) ? True  : Bool.Null;
                } else {
                    return null;
                }
            }
        }
    }
}
