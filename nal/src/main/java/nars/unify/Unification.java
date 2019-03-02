package nars.unify;

import com.google.common.collect.Iterables;
import jcog.TODO;
import jcog.data.list.FasterList;
import jcog.data.set.ArrayHashSet;
import nars.term.Term;
import nars.term.Variable;
import nars.term.atom.Atomic;
import nars.term.util.transform.AbstractTermTransform;
import nars.unify.mutate.Termutator;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static nars.term.atom.Bool.False;
import static nars.term.atom.Bool.True;

/**
 * immutable and memoizable unification result (map of variables to terms) useful for substitution
 */
abstract public class Unification {

    public final Term sourceX, sourceY;

    protected Unification(Term sourceX, Term sourceY) {
        this.sourceX = sourceX;
        this.sourceY = sourceY;
    }

    //abstract public Term xy(Term x);

    abstract public Iterable<Term> apply(Term x);

    /**
     * indicates unsuccessful unification attempt.
     * TODO distinguish between deterministically impossible and those which stopped before exhausting permutations
     */
    static final Unification NotUnified = new Unification(True,False) {
        @Override
        public Iterable<Term> apply(Term x) {
            return List.of();
        }
    };

    /**
     * does this happen in any cases besides .equals, ex: conj seq
     */
    static final PossibleUnification Self = new PossibleUnification(True,True) {

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

        public PossibleUnification(Term sourceX, Term sourceY) {
            super(sourceX, sourceY);
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

    public static class UniUnification extends PossibleUnification {

        public final Term tx, ty;

        public UniUnification(Term x, Term y, Term tx, Term ty) {
            super(x, y);
            this.tx = tx;
            this.ty = ty;
        }

        @Override
        protected boolean equals(PossibleUnification obj) {
            if (obj instanceof UniUnification) {
                UniUnification u = (UniUnification) obj;
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

        //TODOdate
        int matchStructure = Integer.MAX_VALUE;

        public MapUnification(Term x, Term y) {
            super(x, y);
            this.xy = new UnifiedMap(4);

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
            return transform.apply(x);
        }

        private final AbstractTermTransform.NegObliviousTermTransform transform = new AbstractTermTransform.NegObliviousTermTransform() {

            @Override
            public Term transformAtomic(Atomic x) {
                Term y = xy.get(x);
                if (y == null)
                    return x;
                else
                    return y;
            }

            @Override
            public Term apply(Term x) {
                if (!x.hasAny(matchStructure))
                    return x; //no change

                return super.apply(x);
            }

        };

        @Override
        public String toString() {
            return "unification(" +
                    sourceX + "," + sourceY + ',' +
                    xy +
                    ")";

        }

        public MapUnification putIfAbsent(FasterList<Term> xyPairs) {
            for (int i = 0, n = xyPairs.size(); i < n; ) {
                putIfAbsent(xyPairs.get(i++),xyPairs.get(i++));
            }
            return this;
        }
    }


    public static class PermutingUnification extends Unification {

        private final PossibleUnification start;
        public final ArrayHashSet<PossibleUnification> fork = new ArrayHashSet();

        public final Termutator[] termutes;

        public PermutingUnification(Term x, Term y, PossibleUnification start, Termutator[] termutes) {
            super(x, y);
            this.start = start;
            this.termutes = termutes;
            //TODO calculate max possible permutations from Termutes
        }

        public void discover(Unify x, int ttl) {
            Unify y = x.emptyCopy((yy)->{
                Unification z = yy.unification(sourceX, sourceY, false);
                if (z instanceof PossibleUnification)
                    fork.add((PossibleUnification) z);
                else
                    throw new TODO("recursive termute");
                return true;
            });
            y.setTTL(ttl);
            start.apply(y);
            y.tryMatches(termutes);

        }

        @Override
        public Iterable<Term> apply(Term x) {
            switch (fork.size()) {
                case 0:
                    return List.of();
                case 1:
                    return fork.first().apply(x);
                default:
                    Iterable<PossibleUnification> forkShuffled = shuffle(fork, ThreadLocalRandom.current());
                    return Iterables.transform(forkShuffled, a -> a.subst(x)); //HACK could be better
            }
        }

        static private MutableList<PossibleUnification> shuffle(ArrayHashSet<PossibleUnification> fork, Random rng) {
            return fork.list.clone().shuffleThis(rng);
        }
    }


//TODO 1-element simple substitution.  compact and fast
//public static class ReplacementUnification = new Unification();
}
