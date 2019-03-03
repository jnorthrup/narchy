package nars.unify;

import com.google.common.collect.Iterables;
import jcog.TODO;
import jcog.Util;
import jcog.data.list.FasterList;
import jcog.data.set.ArrayHashSet;
import nars.Param;
import nars.term.Term;
import nars.term.Variable;
import nars.term.util.transform.AbstractTermTransform;
import nars.unify.mutate.Termutator;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static nars.term.atom.Bool.Null;

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

        @Override
        public int forkCount() {
            return 0;
        }
    };

    /**
     * does this happen in any cases besides .equals, ex: conj seq
     */
    public static final DeterministicUnification Self = new DeterministicUnification() {

        @Override
        protected boolean equals(DeterministicUnification obj) {
            return this == obj;
        }

        @Override
        void apply(Unify y) {

        }

        @Override
        public Term xy(Term x) {
            return x;
        }
    };

    abstract public int forkCount();

    /**
     * an individual solution
     */
    abstract public static class DeterministicUnification extends Unification {

        public DeterministicUnification() {
            super();
        }

        @Override
        public final boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj instanceof DeterministicUnification)
                return equals((DeterministicUnification) obj);
            return false;
        }

        abstract protected boolean equals(DeterministicUnification obj);

        @Override
        public final int forkCount() {
            return 1;
        }

        @Override
        public final Iterable<Term> apply(Term x) {
            return List.of(transform(x));
        }

        public Term transform(Term x) {
            return AbstractTermTransform.transform(x, transform());
        }

        protected Unify.UnifyTransform.LambdaUnifyTransform transform() {
            return new Unify.UnifyTransform.LambdaUnifyTransform(this::xy);
        }

        @Nullable
        abstract public Term xy(Term t);

        /**
         * sets the mappings in a target unify
         */
        abstract void apply(Unify y);
    }

    public static class OneTermUnification extends DeterministicUnification {

        public final Term tx, ty;

        public OneTermUnification(Term tx, Term ty) {
            super();
            this.tx = tx;
            this.ty = ty;
        }

        @Override
        protected boolean equals(DeterministicUnification obj) {
            if (obj instanceof OneTermUnification) {
                OneTermUnification u = (OneTermUnification) obj;
                return tx.equals(u.tx) && ty.equals(u.ty);
            }
            return false;
        }

        @Override
        public Term xy(Term t) {
            if (tx.equals(t)) return ty;
            else return null;
        }

        @Override
        void apply(Unify u) {
            boolean applied = u.putXY((Variable/*HACK*/) tx, ty);
            assert (applied);
        }
    }

    public static class MapUnification extends DeterministicUnification {

        final Map<Term, Term> xy;

        //TODO
        int matchStructure = Integer.MAX_VALUE;

        public MapUnification() {
            super();
            this.xy = new UnifiedMap(4);
        }

        @Override
        protected boolean equals(DeterministicUnification obj) {
            if (obj instanceof MapUnification) {
                MapUnification u = (MapUnification) obj;
                if (u.matchStructure != matchStructure)
                    return false;
                return xy.equals(u.xy);
            }
            return false;
        }

        @Override
        void apply(Unify u) {
            xy.forEach((tx, ty) -> {
                boolean applied = u.putXY((Variable/*HACK*/) tx, ty);
                assert (applied);
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

        @Override
        public final Term xy(Term x) {
            return xy.get(x);
        }


        @Override
        public String toString() {
            return "unification(" + xy + ")";
        }

        public MapUnification putIfAbsent(FasterList<Term> xyPairs) {
            for (int i = 0, n = xyPairs.size(); i < n; ) {
                putIfAbsent(xyPairs.get(i++), xyPairs.get(i++));
            }
            return this;
        }
    }


    /**
     * not thread-safe
     */
    public static class PermutingUnification extends Unification {

        private final DeterministicUnification start;
        public final ArrayHashSet<DeterministicUnification> fork = new ArrayHashSet();

        public final Termutator[] termutes;
        private final Unify discovery;
        private final int restart;
        private int discoveriesRemain;


        public PermutingUnification(Unify x, DeterministicUnification start, Termutator[] termutes) {
            super();
            this.start = start;
            this.termutes = termutes;
            this.discovery = new Unify.LazyUnify(x) {

                @Override
                protected boolean live() {
                    return super.live() && discoveriesRemain > 0;
                }

                @Override
                protected void tryMatch() {

                    --discoveriesRemain;

                    Unification z = unification(false);
                    if (z instanceof DeterministicUnification) {
                        if (fork.add((DeterministicUnification) z)) {
                            //TODO calculate max possible permutations from Termutes, and set done
                        }
                    } else if (z instanceof PermutingUnification) {
                        if (Param.DEBUG)
                            throw new TODO("recursive or-other 2nd-layer termute");
                    }
                }

            };
            start.apply(discovery);
            restart = discovery.size();
        }


        /**
         * returns how many TTL used
         */
        public int discover(int discoveriesMax, int ttl) {
            discovery.revert(restart);

            this.discoveriesRemain = discoveriesMax;
            discovery.setTTL(ttl);

            discovery.tryMatches(termutes);

            return Util.clamp(ttl - discovery.ttl, 0, ttl);
        }

        @Override
        public int forkCount() {
            return fork.size();
        }

        @Override
        public Iterable<Term> apply(Term x) {
            switch (fork.size()) {
                case 0:
                    return List.of();
                case 1:
                    return fork.first().apply(x);
                default:
                    //HACK could be better
                    return Iterables.filter(
                            Iterables.transform(shuffle(fork, discovery.random), a -> a.transform(x)),
                            z -> z != null
                                    &&
                                    z != Null
                    );
            }
        }

        static private MutableList<DeterministicUnification> shuffle(ArrayHashSet<DeterministicUnification> fork, Random rng) {
            return fork.list.clone().shuffleThis(rng);
        }
    }

}
