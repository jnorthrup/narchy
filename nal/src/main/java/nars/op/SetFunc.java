package nars.op;

import nars.$;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Functor;
import nars.term.Term;
import nars.term.Terms;
import nars.term.atom.Int;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import static nars.Op.INT;
import static nars.Op.Null;
import static nars.util.time.Tense.DTERNAL;

public class SetFunc {

    public static final Functor union = new Functor.BinaryFunctor("union") {

        @Override
        public boolean validOp(Op o) {
            return o.commutative;
        }

        @Nullable
        @Override public Term apply(Term a, Term b) {

            return union(a.op(), a.subterms(), b.subterms() );
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

            return intersect(a.op(), a.subterms(), b.subterms());
    //        else
    //            return null;
        }

    };

    public static Term intersect(/*@NotNull*/ Op o, Subterms a, Subterms b) {
        if (a instanceof Term && a.equals(b))
            return (Term) a;


        Set<Term> cc = Subterms.intersect(a, b);
        if (cc == null) return Null;

        int ssi = cc.size();
        if (ssi == 0) return Null;

        Term[] c = cc.toArray(new Term[ssi]);
        if (ssi > 1)
            Arrays.sort(c);

        return o.the(c);
    }

    public static Term union(/*@NotNull*/ Op o, Subterms a, Subterms b) {
        boolean bothTerms = a instanceof Term && b instanceof Term;
        if (bothTerms && a.equals(b))
            return (Term) a;

        TreeSet<Term> t = new TreeSet<>();
        a.copyInto(t);
        b.copyInto(t);
        if (bothTerms) {
            int as = a.subs();
            int bs = b.subs();
            int maxSize = Math.max(as, bs);
            if (t.size() == maxSize) {
                //the smaller is contained by the larger other
                //so return an input value rather than constructing a duplicate
                return (Term) (as > bs ? a : b);
            }
        }
        return o.the(DTERNAL, t);
    }

    /**
     * input: a compound of >1 items
     * output: a product containing the inputs, sorted according to (the most) natural ordering
     */
    public static final Functor sort = new Functor.UnaryBidiFunctor("sort") {

        @Override
        protected Term compute(Term x) {
            if (x.subs() < 2)
                return Null; //invalid

            if (x.hasAny(Op.varBits))
                return null; //incomputable

            return $.p( Terms.sorted(x.subterms().arrayShared()) );
        }

    };

}
