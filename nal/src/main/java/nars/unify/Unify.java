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
import nars.term.util.TermHashMap;
import nars.term.util.transform.Subst;
import nars.unify.constraint.MatchConstraint;
import nars.unify.mutate.Termutator;
import org.eclipse.collections.api.block.predicate.Predicate2;
import org.jetbrains.annotations.Nullable;

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




    /**
     * accumulates the next segment of the termutation stack
     */
    public final Set<Termutator> termutes = new LinkedHashSet(4, 0.99f);

    public final VersionMap<Variable, Term> xy;
    public final Random random;

    /**
     * whether the variable unification allows to happen in reverse (a variable in Y can unify a constant in X)
     */
    public boolean symmetric = false;

    public int dtTolerance = 0;

    public final Op type;

    public final int typeBits;


    /**
     * @param type   if null, unifies any variable type.  if non-null, only unifies that type
     * @param random
     */
    protected Unify(@Nullable Op type, Random random, int stackMax, int initialTTL) {
        this(type, random, stackMax);
        setTTL(initialTTL);
    }

    protected Unify(@Nullable Op type, Random random, int stackMax) {
        this(type, random, stackMax, new TermHashMap());
    }

    protected Unify(@Nullable Op type, Random random, int stackMax, Map/*<Variable,Versioned<Term>>*/ termMap) {
        super(stackMax);

        this.random = random;
        this.type = type;
        this.typeBits = type == null ? Op.Variable : type.bit;

        this.xy = new ConstrainedVersionMap(this, termMap);
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
    protected abstract void tryMatch();


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
    public final Term xy(/*Variable*/Term x) {
        return !(x instanceof Variable) ? null : xy.get(x);
    }

    /**
     * completely dereferences a term (usually a variable)
     */
    public final Term resolve(final Term x) {
        Term /*Variable*/ z = x, y;
        while (z instanceof Variable && (y = xy.get(z)) != null) {
            //assert(y!=z && y!=x);
            z = y;
        }
        return z;
    }

    /**
     * default usage: invokes the match callback if successful
     */
    public final void unify(Term x, Term y) {
        unify(x, y, true);
    }

    /**
     * unifies the next component, which can either be at the start (true, false), middle (false, false), or end (false, true)
     * of a matching context
     * <p>
     * setting finish=false allows matching in pieces before finishing
     * <p>
     * NOT thread safe, use from single thread only at a time
     */
    public final boolean unify(Term x, Term y, boolean finish) {


        if (x.unify(y, this)) {
            if (finish) {
                tryMatches();
            }
            return true;
        }
        return false;
    }


    private void tryMatches() {
        int ts = termutes.size();
        if (ts > 0) {


            Termutator[] t = termutes.toArray(new Termutator[ts]);

            termutes.clear();

            if (ts > 1 && Param.SHUFFLE_TERMUTES) {
                Util.shuffle(t, random);
            }

            tryMutate(t, -1);

        } else {

            tryMatch();
        }


    }

    @Override
    public Unify clear() {
        super.clear();
        return this;
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

        return !y.containsRecursively(x) && replaceXY(x, y);

    }


    public final boolean replaceXY(final Variable x, final Term y) {
        return xy.tryPut(x, y);
    }


    /**
     * whether is constant with respect to the current matched variable type
     */
    public boolean constant(Termlike x) {
        return !x.hasAny(typeBits);
    }


    public boolean constrain(MatchConstraint m) {
        return constrain(m.x, m);
    }

    private boolean constrain(Variable target, MatchConstraint... mm) {
        ((ConstrainedVersionedTerm) xy.getOrCreateIfAbsent(target)).constrain(mm);
        return true;
    }

    /** xdt and ydt must both not equal either XTERNAL or DTERNAL */
    public boolean unifyDT(int xdt, int ydt) {
        //assert(xdt!=DTERNAL && xdt!=XTERNAL && ydt!=DTERNAL && ydt!=XTERNAL);
        return Math.abs(xdt - ydt) < dtTolerance;
    }

    private class ConstrainedVersionMap extends VersionMap<Variable, Term> {
        ConstrainedVersionMap(Versioning versioning, Map<Variable, Versioned<Term>> termMap) {
            super(versioning,

                    termMap,
                    1);
        }


        @Override
        protected Versioned newEntry(Variable x) {
            return new ConstrainedVersionedTerm();
        }


    }

    final class ConstrainedVersionedTerm extends Versioned<Term> implements Predicate2<MatchConstraint, Term> {

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
            return c == null || !c.anySatisfyWith(this, x);
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

        @Override
        public boolean accept(MatchConstraint c, Term x) {
            return c.invalid(x, Unify.this);
        }
    }

}


