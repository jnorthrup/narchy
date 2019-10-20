package nars.term.functor;

import nars.Idempotent;
import nars.eval.Evaluation;
import nars.subterm.Subterms;
import nars.term.Functor;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.IdempotentBool;
import org.jetbrains.annotations.Nullable;

import static nars.term.atom.IdempotentBool.True;

/** Functor template for a binary functor with bidirectional parameter cases */
public abstract class BinaryBidiFunctor extends Functor implements Idempotent {

    public BinaryBidiFunctor(String name) {
        this(fName(name));
    }

    BinaryBidiFunctor(Atom atom) {
        super(atom);
    }

    @Override
    public final @Nullable Term apply(Evaluation e, Subterms terms) {
        int s = terms.subs();
        switch (s) {
            case 2:
                return apply2(e, terms.sub(0), terms.sub(1));
            case 3:
                return apply3(e, terms.sub(0), terms.sub(1), terms.sub(2));
            default:
                return IdempotentBool.Null;
        }
    }

    Term apply2(Evaluation e, Term x, Term y) {
		return x.op().var || y.op().var ? null : compute(e, x, y);
    }

    protected abstract Term compute(Evaluation e, Term x, Term y);

    protected Term apply3(Evaluation e, Term x, Term y, Term xy) {
        boolean xVar = x.op().var;
        boolean yVar = y.op().var;
        if (xy.op().var) {

            if (xVar || yVar) {
                return null;
            } else {
                Term XY = compute(e, x, y);
                if (XY!=null) {
                    return e.is(xy, XY) ? null : IdempotentBool.Null;
                } else {
                    return null;
                }
            }
        } else {
            if (xVar && !yVar) {
                return computeXfromYandXY(e, x, y, xy);
            } else if (yVar && !xVar) {
                return computeYfromXandXY(e, x, y, xy);
            } else if (!yVar /*&& !xVar*/) {

                Term XY = compute(e, x, y);
                if (XY == null)
                    return null;
                //assert(XY!=null): "functor " + this + " " + x + "," + y + ", " + xy + " -> compute=null";

				//true, keep
				//false?
				return XY.equals(xy) ? True : IdempotentBool.Null;
            } else {
                return computeFromXY(e, x, y, xy);
            }
        }
    }

    protected abstract Term computeFromXY(Evaluation e, Term x, Term y, Term xy);

    protected abstract Term computeXfromYandXY(Evaluation e, Term x, Term y, Term xy);

    protected abstract Term computeYfromXandXY(Evaluation e, Term x, Term y, Term xy);
}
