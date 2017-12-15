package nars.term.transform;

import nars.$;
import nars.Op;
import nars.derive.match.EllipsisMatch;
import nars.index.term.NewCompound;
import nars.index.term.TermContext;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.sub.Subterms;
import nars.term.var.AbstractVariable;
import org.jetbrains.annotations.Nullable;

import static nars.Op.PROD;
import static nars.Op.VAR_QUERY;

/**
 * I = input term type, T = transformable subterm type
 */
public interface CompoundTransform extends TermContext {

    default int dt(Compound c) {
        return c.dt();
    }

    @Nullable
    default Term transform(Compound x, Op op, int dt) {

        Subterms xx = x.subterms();

        Subterms yy = transform(xx, !op.allowsBool);

        if (yy == null) {
            return null;
        } else if (yy != xx || op != x.op()) {
            return the(op, dt, yy);
        } else {
            return x.dt(dt);
        }
    }

    /**
     * returns 'x' unchanged if no changes were applied,
     * returns 'y' if changes
     * returns null if untransformable
     */
    default Subterms transform(Subterms x, boolean boolFilter) {

        int s = x.subs();

        NewCompound y = null;

        for (int i = 0; i < s; i++) {

            Term xi = x.sub(i);

            Term yi = xi.transform(this);

            if (yi == null)
                return null;

            if (yi instanceof EllipsisMatch) {
                EllipsisMatch xe = (EllipsisMatch) yi;
                int xes = xe.subs();

                if (y == null) {
                    y = new NewCompound(PROD, s - 1 + xes /*estimate */); //create anyway because this will signal if it was just empty
                    if (i > 0) x.forEach(y::add, 0, i); //add previously skipped subterms
                }

                if (xes > 0) {
                    for (int j = 0; j < xes; j++) {
                        @Nullable Term k = xe.sub(j).transform(this);
                        if (Term.invalidBoolSubterm(k, boolFilter)) {
                            return null;
                        } else {
                            y.add(k);
                        }
                    }
                }

            } else {

                if (xi != yi && (yi.getClass() != xi.getClass() || !xi.equals(yi))) {

                    if (Term.invalidBoolSubterm(yi, boolFilter)) {
                        return null;
                    }

                    if (y == null) {
                        y = new NewCompound(PROD, s);
                        if (i > 0) x.forEach(y::add, 0, i); //add previously skipped subterms
                    }
                }

                if (y != null)
                    y.add(yi);

            }

        }

        return y != null ? y : x;
    }


    default Term the(Op op, int dt, Subterms t) {
        return op.the(dt, t.arrayShared());
    }

    /**
     * transforms non-compound subterms
     */
    @Override
    default @Nullable Termed apply(Term t) {
        return t;
    }


    /**
     * change all query variables to dep vars
     */
    /**
     * change all query variables to dep vars
     */
    CompoundTransform queryToDepVar = new CompoundTransform() {
        @Override
        public Term apply(Term subterm) {
            if (subterm.op() == VAR_QUERY) {
                return $.varDep((((AbstractVariable) subterm).anonNum()));
            }
            return subterm;
        }
    };

//    CompoundTransform Identity = (parent, subterm) -> subterm;

//    CompoundTransform<Compound,Term> None = new CompoundTransform<Compound,Term>() {
//        @Override
//        public boolean test(Term o) {
//            return true;
//        }
//
//        @Nullable
//        @Override
//        public Term apply(Compound parent, Term subterm) {
//            return subterm;
//        }
//    };

}
