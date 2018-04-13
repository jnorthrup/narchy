package nars.op;

import nars.Op;
import nars.term.Functor;
import nars.term.Term;
import nars.term.Terms;
import nars.term.atom.Int;
import org.jetbrains.annotations.Nullable;

import static nars.Op.INT;

public class SetFunc {

    public static final Functor union = new Functor.BinaryFunctor("union") {

        @Override
        public boolean validOp(Op o) {
            return o.commutative;
        }

        @Nullable
        @Override public Term apply(Term a, Term b) {

            return Terms.union(a.op(), a.subterms(), b.subterms() );
        }

    };

    /**
     * all X which are in the first term AND not in the second term
     */
    public static final Functor differ = new Functor.BinaryFunctor("differ") {

        @Override
        public boolean validOp(Op o) {
            return o.commutative;// || o == INT;
        }

        @Override
        public Term apply(Term a, Term b) {
            return Op.differenceSet(a.op(), a, b);
        }
    };



    public static final Functor intersect = new Functor.BinaryFunctor("intersect") {

        @Override
        public boolean validOp(Op o) {
            return o.commutative;
        }

        @Override
        public Term apply(Term a, Term b) {
    //        Op aop = a.op();
    //        if (b.op() == aop)

            if (a instanceof Int.IntRange && b.op()==INT) {
               return ((Int.IntRange)a).intersect(b);
            }

            return Terms.intersect(a.op(), a.subterms(), b.subterms());
    //        else
    //            return null;
        }

    };
}
