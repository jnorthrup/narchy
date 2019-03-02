package nars.unify;

import com.google.common.collect.Iterables;
import jcog.TODO;
import jcog.data.list.FasterList;
import jcog.data.set.ArrayHashSet;
import nars.term.Term;
import nars.term.Variable;
import nars.term.util.transform.AbstractTermTransform;
import nars.term.util.transform.TermTransform;
import nars.unify.mutate.Termutator;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * immutable and memoizable unification result (map of variables to terms) useful for substitution
 */
abstract public class Unification {


    protected Unification() {
    }


    abstract public Iterable<Term> apply(Term x);

    /**
     * indicates unsuccessful unification attempt.
     * TODO distinguish between deterministically impossible and those which stopped before exhausting permutations
     */
    static final Unification NotUnified = new Unification() {
        @Override
        public Iterable<Term> apply(Term x) {
            return List.of();
        }
    };

    /**
     * does this happen in any cases besides .equals, ex: conj seq
     */
    static final PossibleUnification Self = new PossibleUnification() {

        @Override
        protected boolean equals(PossibleUnification obj) {
            return this==obj;
        }

        @Override
        void apply(Unify y) {

        }

        @Override
        public Term subst(Term x) {
            return x;
        }
    };

    /** an individual solution */
    abstract static class PossibleUnification extends Unification {

        public PossibleUnification() {
            super();
        }

        @Override
        public final boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj instanceof PossibleUnification)
                return equals((PossibleUnification)obj);
            return false;
        }

        abstract protected boolean equals(PossibleUnification obj);

        @Override
        public final Iterable<Term> apply(Term x) {
            return List.of(subst(x));
        }

        abstract public Term subst(Term t);

        /** sets the mappings in a target unify */
        abstract void apply(Unify y);
    }

    public static class OneTermUnification extends PossibleUnification {

        public final Term tx, ty;

        public OneTermUnification(Term tx, Term ty) {
            super();
            this.tx = tx;
            this.ty = ty;
        }

        @Override
        protected boolean equals(PossibleUnification obj) {
            if (obj instanceof OneTermUnification) {
                OneTermUnification u = (OneTermUnification) obj;
                return tx.equals(u.tx) && ty.equals(u.ty);
            }
            return false;
        }

        @Override
        public Term subst(Term t) {
            return t.replace(tx, ty);
        }

        @Override
        void apply(Unify u) {
            boolean applied = u.putXY((Variable/*HACK*/)tx, ty);
            assert(applied);
        }
    }

    public static class MapUnification extends PossibleUnification {

        final Map<Term,Term> xy;
        private final TermTransform transform;

        //TODO
        int matchStructure = Integer.MAX_VALUE;

        public MapUnification(Unify parent) {
            super();
            this.xy = new UnifiedMap(4);
            this.transform = parent.transform();
        }

        @Override
        protected boolean equals(PossibleUnification obj) {
            if (obj instanceof MapUnification) {
                MapUnification u = (MapUnification) obj;
                if (u.matchStructure!=matchStructure)
                    return false;
                return xy.equals(u.xy);
            }
            return false;
        }

        @Override
        void apply(Unify u) {
            xy.forEach((tx,ty)->{
                boolean applied = u.putXY((Variable/*HACK*/)tx, ty);
                assert(applied);
            });
        }

        public void put(Term x, Term y) {
            xy.put(x, y);
//            if (x.op() != VAR_PATTERN)
//                matchStructure |= (x.structure() & ~Op.Variable);
        }

        public void putIfAbsent(Term x, Term y) {
            xy.putIfAbsent(x, y);
//            if (x.op() != VAR_PATTERN)
//                matchStructure |= (x.structure() & ~Op.Variable);
        }

        @Override public Term subst(Term x) {

            return AbstractTermTransform.applyBest(x, transform);
        }


        @Override
        public String toString() {
            return "unification(" + xy + ")";
        }

        public MapUnification putIfAbsent(FasterList<Term> xyPairs) {
            for (int i = 0, n = xyPairs.size(); i < n; ) {
                putIfAbsent(xyPairs.get(i++),xyPairs.get(i++));
            }
            return this;
        }
    }


    /** not thread-safe */
    public static class PermutingUnification extends Unification {

        private final PossibleUnification start;
        public final ArrayHashSet<PossibleUnification> fork = new ArrayHashSet();

        public final Termutator[] termutes;
        private final Unify u;

        public PermutingUnification(Unify x, PossibleUnification start, Termutator[] termutes) {
            super();
            this.start = start;
            this.termutes = termutes;
            this.u = x.emptyClone((yy)->{
                Unification z = yy.unification(false);
                if (z instanceof PossibleUnification) {
                    if (fork.add((PossibleUnification) z)) {
                        //TODO calculate max possible permutations from Termutes, and set done
                    }
                } else
                    throw new TODO("recursive or-other 2nd-layer termute");
                return true;
            });
        }

        public void discover(int ttl) {
            u.clear();
            u.setTTL(ttl);
            start.apply(u);
            u.tryMatches(termutes);
        }

        @Override
        public Iterable<Term> apply(Term x) {
            switch (fork.size()) {
                case 0:
                    return List.of();
                case 1:
                    return fork.first().apply(x);
                default:
                    return Iterables.transform(shuffle(fork, u.random), a -> a.subst(x)); //HACK could be better
            }
        }

        static private MutableList<PossibleUnification> shuffle(ArrayHashSet<PossibleUnification> fork, Random rng) {
            return fork.list.clone().shuffleThis(rng);
        }
    }

}
