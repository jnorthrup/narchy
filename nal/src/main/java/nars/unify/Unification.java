package nars.unify;

import com.google.common.collect.Iterables;
import jcog.TODO;
import jcog.data.iterator.ArrayIterator;
import nars.term.Term;
import nars.term.util.map.TermRadixTree;
import nars.unify.mutate.Termutator;

import java.util.List;

import static nars.Op.VAR_PATTERN;

/**
 * immutable and memoizable unification result (map of variables to terms) useful for substitution
 *
 */
abstract public class Unification  {

    //abstract public Term xy(Term x);

    abstract public Iterable<Term> apply(Term x);

    /** indicates unsuccessful unification attempt.
     * TODO distinguish between deterministically impossible and those which stopped before exhausting permutations */
    static Unification ununifiable = new Unification() {
//        @Override
//        public Term xy(Term x) {
//            return Null;
//        }

        @Override
        public Iterable<Term> apply(Term x) {
            return List.of();
        }
    };

    public static class OnlyUnification extends Unification {

        public final Term x, y;
        final TermRadixTree<Term> xy;
        int matchStructure = 0;

        public OnlyUnification(Term x, Term y) {
            this.xy = new TermRadixTree();
            this.x = x;
            this.y = y;
        }

        public void put(Term x, Term y) {
            xy.put(x, y);
            if (x.op()!=VAR_PATTERN)
                matchStructure |= x.structure();
        }
        public void putIfAbsent(Term x, Term y) {
            xy.putIfAbsent(x, y);
            if (x.op()!=VAR_PATTERN)
                matchStructure |= x.structure();
        }
        public Term transformOnly(Term x) {
            Term y = xy.get(x);
            return y!=null ? y : x;
        }

        @Override
        public Iterable<Term> apply(Term x) {
            return List.of(transformOnly(x));
        }
    }

    public static Unification extract(Term x, Term y, Unify u) {

        if (!u.unify(x, y, false)) {
            u.clear();
            return ununifiable;
        } else {
            if (u.termutes.isEmpty()) {
                OnlyUnification xy = new OnlyUnification(x, y);
                u.clear(xy::putIfAbsent);
                return xy;
            } else {
                //PermutingUnification...
                throw new TODO();
            }
        }
    }

    public static class PermutingUnification extends OnlyUnification {

        public final OnlyUnification[] alternates;

        public final Termutator[] termutes;

        private PermutingUnification(Term x, Term y, Termutator[] termutes, OnlyUnification[] alternates) {
            super(x,y);
            this.alternates = alternates;
            this.termutes = termutes;
        }


        @Override
        public Iterable<Term> apply(Term x) {
            switch(alternates.length) {
                case 0: return List.of();
                case 1: return alternates[0].apply(x);

                default:
                    //HACK could be better
                    return Iterables.transform(ArrayIterator.iterable(alternates), a -> a.transformOnly(x));
            }
        }
    }


    //TODO 1-element simple substitution.  compact and fast
    //public static class ReplacementUnification = new Unification();
}
