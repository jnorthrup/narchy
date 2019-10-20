package nars.term.functor;

import nars.Op;
import nars.eval.Evaluation;
import nars.subterm.Subterms;
import nars.term.Functor;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.IdempotentBool;
import org.jetbrains.annotations.Nullable;

import static nars.term.atom.IdempotentBool.False;
import static nars.term.atom.IdempotentBool.True;

/** (potentially) reversible function */
public abstract class UnaryBidiFunctor extends Functor {

    public UnaryBidiFunctor(Atom atom) {
        super(atom);
    }

    public UnaryBidiFunctor(String atom) { super(atom); }

    @Override
    public final @Nullable Term apply(Evaluation e, Subterms terms) {
        int s = terms.subs();
        switch (s) {
            case 1:
                return apply1(terms.sub(0));
            case 2:
                return apply2(e, terms.sub(0), terms.sub(1));
            default:
                return IdempotentBool.Null;
        }
    }

    Term apply1(Term x) {
		return x.op().var ? null : compute(x);
    }

    protected abstract Term compute(Term x);

    /** override for reversible functions, though it isnt required */
    protected Term uncompute(Term x, Term y) {
        return null;
    }

    protected Term apply2(Evaluation e, Term x, Term y) {
        boolean xVar = x.op().var;
        if (y.op().var) {

            if (xVar) {
                return null;
            } else {
                Term XY = compute(x);
                if (XY!=null) {
                    return e.is(y, XY) ?
                            null : IdempotentBool.Null;
                } else {
                    return null;
                }
            }
        } else {
            if (x.hasAny(Op.Variable)) {
                Term X = uncompute(x, y);
                if (X!=null) {
                    return e.is(x, X) ?
                        null : IdempotentBool.Null;
                } else {
                    return null;
                }
            } else {

                Term yActual = compute(x);
                if (yActual == null)
                    return null;
                //else
                    //return Equal.the(y,yActual);
                    //return yActual;
				return y.equals(yActual) ? True : False;
            }
        }
    }
}
