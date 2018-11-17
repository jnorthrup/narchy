package nars.unify;

import jcog.TODO;
import jcog.Util;
import jcog.WTF;
import jcog.data.set.ArrayHashSet;
import jcog.version.VersionMap;
import jcog.version.Versioned;
import jcog.version.Versioning;
import nars.Op;
import nars.Param;
import nars.term.Term;
import nars.term.Termlike;
import nars.term.Variable;
import nars.term.util.TermHashMap;
import nars.term.util.transform.Subst;
import nars.unify.constraint.UnifyConstraint;
import nars.unify.mutate.Termutator;
import org.jetbrains.annotations.Nullable;

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
    public final Set<Termutator> termutes = new ArrayHashSet<>(4);

    public final VersionMap<Variable, Term> xy;
    public Random random;

    /**
     * whether the variable unification allows to happen in reverse (a variable in Y can unify a constant in X)
     */


    public int dtTolerance = 0;

    public int varBits;


    /**
     * @param type   if null, unifies any variable type.  if non-null, only unifies that type
     * @param random
     */
    protected Unify(@Nullable Op type, Random random, int stackMax, int initialTTL) {
        this(type, random, stackMax);
        setTTL(initialTTL);
    }

    protected Unify(@Nullable Op type, Random random, int stackMax) {
        this(type == null ? Op.Variable : type.bit, random, stackMax);
    }

    protected Unify(int varBits, Random random, int stackMax) {
        this(varBits, random, stackMax, new TermHashMap<>());
    }

    protected Unify(int varBits, Random random, int stackMax, Map/*<Variable,Versioned<Term>>*/ termMap) {
        super(stackMax);

        this.random = random;
        this.varBits = varBits;

        this.xy = new ConstrainedVersionMap(this, termMap);
    }

    /**
     * spend an amount of TTL; returns whether it is still live
     */
    public final boolean use(int cost) {
        return ((ttl -= cost) > 0);
    }

    public <U extends Unify> U random(Random random) {
        this.random = random;
        return (U) this;
    }


    /**
     * called each time all variables are satisfied in a unique way
     *
     * @return whether to continue on any subsequent matches
     */
    protected abstract void tryMatch();


    public final boolean tryMutate(Termutator[] chain, int next) {

        if (!use(Param.TTL_MUTATE))
            return false;

        if (++next < chain.length) {

            chain[next].mutate(this, chain, next);

        } else {
            tryMatch();
        }

        return ttl>0;
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
    public final Term resolve(final Variable x) {
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
    public final boolean unify(Term x, Term y) {
        return unify(x, y, true);
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

//        if (!(ttl > 0))
//            throw new WTF("likely needs some TTL");

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
        termutes.clear();
        return this;
    }

    @Override
    public String toString() {
        return xy + "$" + ((Versioning<Term>) this).ttl;
    }


    /**
     * whether the op is assignable
     */
    public final boolean matchType(Op v) {
        return ((this.varBits & v.bit) != 0);
    }

    @Override
    public boolean isEmpty() {
        throw new TODO();
    }


    /**
     * returns true if the assignment was allowed, false otherwise
     * args should be non-null. the annotations are removed for perf reasons
     */
    public final boolean putXY(final Variable x, Term y) {

        return xy.set(x, y);

    }


    /**
     * whether is constant with respect to the current matched variable type
     */
    public final boolean constant(Termlike x) {
        return !x.hasAny(varBits);// || (x instanceof ImDep);
    }
    public final boolean constant(int structure) {
        return !Op.hasAny(structure, varBits);// || (x instanceof ImDep);
    }


    public boolean constrain(UnifyConstraint m) {
        Term target = m.x;
        if (target instanceof Variable) {
            return ((ConstrainedVersionedTerm) xy.getOrCreateIfAbsent((Variable)target)).constrain(m);
        } else {
            throw new WTF();
        }
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

    final class ConstrainedVersionedTerm extends Versioned<Term> {

        /**
         * lazily constructed
         */
        UnifyConstraint constraint;

        ConstrainedVersionedTerm() {
            super(Unify.this, new Term[1]);
        }

        @Nullable
        @Override
        public Versioned<Term> set(Term next) {
            @Nullable Term existing = get();
            if (existing!=null)
                return existing.equals(next) ? this : null;
            if (valid(next) && addWithoutResize(next)) {
                if (context.add(this))
                    return this;
                else
                    pop();
            }

            return null;
        }

        private boolean valid(Term x) {
            UnifyConstraint c = this.constraint;
            //return MatchConstraint.valid(x, c);
            return c == null || !c.invalid(x, Unify.this);
        }

        boolean constrain(UnifyConstraint c) {
            constraint = c;
            return Unify.this.add(off);
        }

        final Versioned off = new DummyVersioned() {
            @Override
            protected void off() {
                constraint = null;
            }
        };
    }

}


