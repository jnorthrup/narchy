package nars.unify;

import jcog.Util;
import jcog.version.VersionMap;
import jcog.version.Versioned;
import jcog.version.Versioning;
import nars.Op;
import nars.Param;
import nars.subterm.Subterms;
import nars.term.Term;
import nars.term.Termlike;
import nars.term.Variable;
import nars.unify.constraint.MatchConstraint;
import nars.unify.mutate.Termutator;
import nars.util.term.TermHashMap;
import nars.util.term.transform.Subst;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;


/* recurses a pair of compound term tree's subterms
across a hierarchy of sequential and permutative fanouts
where valid matches are discovered, backtracked,
and collected until power is depleted.



https:
https:
see this code for a clear explanation of what a prolog unifier does.
this code does some additional things but shares a general structure with the lojix code which i just found now
So it can be useful for a more easy to understand rewrite of this class TODO


*/
public abstract class Unify extends Versioning implements Subst {

    protected final static Logger logger = LoggerFactory.getLogger(Unify.class);
    @Nullable
    public final Op type;
    public final Set<Termutator> termutes = new LinkedHashSet(8);
    public final VersionMap<Variable, Term> xy;
    public Random random;
    /**
     * temporal tolerance; if -1, then it is not tested
     */
    public int dur = -1;

    /**
     * whether the variable unification allows to happen in reverse (a variable in Y can unify a constant in X)
     */
    public boolean varSymmetric = true;


    /**
     * @param type   if null, unifies any variable type.  if non-null, only unifies that type
     * @param random
     */
    protected Unify(@Nullable Op type, Random random, int stackMax, int initialTTL) {
        this(type, random, stackMax, initialTTL,
                new TermHashMap()

        );
    }

    protected Unify(@Nullable Op type, Random random, int stackMax, int initialTTL, Map/*<Variable,Versioned<Term>>*/ termMap) {
        super(stackMax, initialTTL);


        this.random = random;
        this.type = type;

        xy = new ConstrainedVersionMap(this, termMap);


    }

    /**
     * spend an amount of TTL; returns whether it is still live
     */
    public final boolean use(int cost) {
        return ((ttl -= cost) > 0);
    }


    /**
     * called each time all variables are satisfied in a unique way
     *
     * @return whether to continue on any subsequent matches
     */
    public abstract void tryMatch();


    public final boolean tryMutate(Termutator[] chain, int next) {

        if (++next < chain.length) {

            chain[next].mutate(this, chain, next);

        } else {
            tryMatch();
        }

        return use(Param.TTL_MUTATE);
    }


    /**
     * only really useful with atom/variable parameters.  compounds arent unified here like apply() will
     */
    @Nullable
    @Override
    public final Term xy(Term x0) {
        if (!(x0 instanceof Variable))
            return null;
        return xy.get(x0);


    }


    /**
     * unifies the next component, which can either be at the start (true, false), middle (false, false), or end (false, true)
     * of a matching context
     * <p>
     * setting finish=false allows matching in pieces before finishing
     * <p>
     * NOT thread safe, use from single thread only at a time
     */
    public Unify unify(Term x, Term y, boolean finish) {


        if (x.unify(y, this)) {
            if (finish) {
                tryMatches();
            }

        }
        return this;
    }


    void tryMatches() {
        int ts = termutes.size();
        if (ts > 0) {


            Termutator[] t = termutes.toArray(new Termutator[ts]);

            termutes.clear();


            if (ts > 1)
                Util.shuffle(t, random);

            tryMutate(t, -1);

        } else {

            tryMatch();
        }


    }


    @Override
    public String toString() {
        return xy + "$" + ((Versioning<Term>) this).ttl;
    }


    /**
     * whether the op is assignable
     */
    public final boolean matchType(Op oy) {
        Op t = this.type;
        return t == null ?
                oy.var :
                oy == t;
    }

    @Override
    public boolean isEmpty() {
        throw new UnsupportedOperationException("slow");
    }


    /**
     * returns true if the assignment was allowed, false otherwise
     * args should be non-null. the annotations are removed for perf reasons
     */
    public final boolean putXY(final Variable x, Term y) {

        if (y.containsRecursively(x)) {


            return false;


        }


        return replaceXY(x, y);

    }


    public final boolean replaceXY(final Variable x, final Term y) {
        return xy.tryPut(x, y);
    }


    /**
     * whether is constant with respect to the current matched variable type
     */
    public boolean constant(Termlike x) {
        return !x.hasAny(type == null ? Op.varBits : type.bit);
    }

    /**
     * counts the number of variables are unifiable in the given term
     */
    public int vars(Subterms x) {
        if (type == null) {
            return x.vars();
        } else {
            return x.subs(type);
        }
    }

    public int typeBits() {
        if (type == null) {
            return Op.varBits;
        } else {
            return type.bit;
        }
    }

    public boolean constrain(MatchConstraint m) {
        return constrain(m.x, m);
    }

    private boolean constrain(Variable target, MatchConstraint... mm) {
        ((ConstrainedVersionedTerm) xy.getOrCreateIfAbsent(target)).constrain(mm);
        return true;
    }

    private class ConstrainedVersionMap extends VersionMap<Variable, Term> {
        public ConstrainedVersionMap(Versioning versioning, Map<Variable, Versioned<Term>> termMap) {
            super(versioning,

                    termMap,
                    1);
        }


        @Override
        protected Versioned newEntry(Variable x) {
            return new ConstrainedVersionedTerm();
        }


    }

    final class ConstrainedVersionedTerm extends Versioned<Term> {

        /**
         * lazily constructed
         */
        Versioned<MatchConstraint> constraints;

        ConstrainedVersionedTerm() {
            super(Unify.this, new Term[1]);
        }

        @Nullable
        @Override
        public Versioned<Term> set(Term next) {
            return valid(next) ? super.set(next) : null;
        }

        private boolean valid(Term x) {
            Versioned<MatchConstraint> c = this.constraints;
            if (c != null) {
                int s = c.size();
                for (int i = 0; i < s; i++)
                    if (c.get(i).invalid(x, Unify.this))
                        return false;
            }
            return true;
        }

        void constrain(MatchConstraint... mm) {

            Versioned<MatchConstraint> c = this.constraints;
            if (c == null)
                c = constraints = new Versioned<>(Unify.this, 4);

            for (MatchConstraint m : mm) {
                Versioned<MatchConstraint> wasSet = c.set(m);
                assert (wasSet != null);
            }

        }

    }

}


