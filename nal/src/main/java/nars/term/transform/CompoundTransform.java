package nars.term.transform;

import nars.$;
import nars.Op;
import nars.derive.match.EllipsisMatch;
import nars.index.term.NewCompound;
import nars.index.term.TermContext;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.container.TermContainer;
import nars.term.var.Variable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.Op.VAR_QUERY;

/**
 * I = input term type, T = transformable subterm type
 */
public interface CompoundTransform extends TermContext {


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
    CompoundTransform queryToDepVar = new CompoundTransform() {
        @Override
        public Term apply(Term subterm) {
            if (subterm.op() == VAR_QUERY) {
                return $.varDep((((Variable) subterm).id()));
            }
            return subterm;
        }
    };

    default int dt(Compound c) {
        return c.dt();
    }

    @Nullable
    default Term transform(Compound x, Op op, int dt) {

        boolean boolFilter = !op.allowsBool;

        TermContainer ss = x.subterms(); //for faster access, generally

        int s = ss.subs();

        NewCompound target = null;

        for (int i = 0; i < s; i++) {

            Term xi = ss.sub(i);

            Term yi = xi.transform(this);

            if (yi == null)
                return null;

            if (yi instanceof EllipsisMatch) {
                EllipsisMatch xx = (EllipsisMatch) yi;
                int xxs = xx.subs();

                if (target == null) {
                    target = new NewCompound(op, s - 1 + xxs /*estimate */); //create anyway because this will signal if it was just empty
                    for (int j = 0; j < i; j++)
                        target.add(ss.sub(j)); //add the pre-existing ones
                }

                if (xxs > 0) {
                    for (int j = 0; j < xxs; j++) {
                        @Nullable Term k = xx.sub(j).transform(this);
                        if (Term.invalidBoolSubterm(k, boolFilter)) {
                            return null;
                        } else {
                            target.add(k);
                        }
                    }
                }

            } else {

                if (xi != yi && (yi.getClass() != xi.getClass() || !x.equals(yi))) {

                    if (Term.invalidBoolSubterm(yi, boolFilter)) {
                        return null;
                    }

                    if (target == null) {
                        target = new NewCompound(op, s);
                        for (int j = 0; j < i; j++)
                            target.add(ss.sub(j)); //add the pre-existing ones
                    }
                }

                if (target!=null)
                    target.add(yi);

            }

        }


        if (target!=null || op != x.op()) {
            return op.the(dt, ((target!=null) ? target : ss).arrayShared());
        }

        return x.dt(dt);
    }

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
