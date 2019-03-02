package nars.unify;

import jcog.Util;
import jcog.data.list.FasterList;
import jcog.data.set.ArrayHashSet;
import jcog.version.*;
import nars.Op;
import nars.Param;
import nars.term.Term;
import nars.term.Termlike;
import nars.term.Variable;
import nars.term.atom.Atomic;
import nars.term.util.map.TermHashMap;
import nars.term.util.transform.AbstractTermTransform;
import nars.unify.constraint.UnifyConstraint;
import nars.unify.mutate.Termutator;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static nars.Op.NEG;
import static nars.Op.VAR_PATTERN;
import static nars.unify.Unification.NotUnified;
import static nars.unify.Unification.Self;


/* recurses a pair of compound target tree's subterms
across a hierarchy of sequential and permutative fanouts
where valid matches are discovered, backtracked,
and collected until power is depleted.



https:
https:
see this code for a clear explanation of what a prolog unifier does.
this code does some additional things but shares a general structure with the lojix code which i just found now
So it can be useful for a more easy to understand rewrite of this class TODO


*/
public abstract class Unify extends Versioning<Term> implements AbstractTermTransform.AbstractNegObliviousTermTransform {

    /**
     * accumulates the next segment of the termutation stack
     */
    public final ArrayHashSet<Termutator> termutes =
            new ArrayHashSet<>(4);
            //new UnifiedSet(4, 0.99f);

    public final VersionMap<Variable, Term> xy;

    /** bits of the unifiable variables; variables not unifiable are tested for equality only */
    public int varBits;


    public Random random;

    /**
     * whether the variable unification allows to happen in reverse (a variable in Y can unify a constant in X)
     */


    public int dtTolerance = 0;

    public boolean commonVariables = true;

    /** recursion limiter HACK
     * TODO use a real stack of arbitrarily length for detecting cycles */
    public int varDepth = 0;

    private final FasterList<ConstrainedVersionedTerm> constrained = new FasterList();


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
        setVarBits(varBits);

        this.xy = new ConstrainedVersionMap(this, termMap);
    }

    public void setVarBits(int varBits) {
        this.varBits = varBits;
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

        if (++next < chain.length) {

            chain[next].mutate(chain, next, this);

            return true;

        } else {
            tryMatch();

            return use(Param.TTL_MUTATE);
        }

    }


    /**
     * completely dereferences a target (usually a variable)
     */
    public final Term resolve(final Variable x) {
        Term /*Variable*/ z = x, y;

        do {
            y = xy.get(z);
            if (y==null)
                break;
            z = y;
        } while (z instanceof Variable);
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

        //assert(ttl > 0): "needs some TTL";

        if (x.unify(y, this)) {

            if (finish)
                tryMatches();

            return true;
        }
        return false;
    }

    /** will have been cleared() */
    public Unification unification(Term x, Term y) {
        if (!unify(x, y, false)) {
            clear();
            return NotUnified;
        } else {
            return unification(x, y, true);
        }
    }

    protected Unification unification(Term x, Term y, boolean clear) {
        FasterList<Term> xyPairs = new FasterList(size * 2 /* estimate */);

        Termutator[] termutes = commitTermutes();

        BiConsumer<Term, Term> eachXY = xyPairs::addAll;
        if (clear) {
            clear(eachXY);
        } else {
            forEach(eachXY);
        }

        Unification.PossibleUnification base;
        int n = xyPairs.size()/2;
        if (n == 0)
            base = Self;
        else if (n == 1)
            base = new Unification.UniUnification(x, y, xyPairs.get(0), xyPairs.get(1));
        else
            base = new Unification.MapUnification(x, y).putIfAbsent(xyPairs);

        if (termutes==null) {
            return base;
        } else {
            return new Unification.PermutingUnification(x, y, base, termutes);
        }
    }

    public Unification unification(Term x, Term y, int discoveryTTL) {
        Unification u = unification(x, y);
        if (u instanceof Unification.PermutingUnification) {
            ((Unification.PermutingUnification)u).discover(this, discoveryTTL);
        }
        return u;
    }

    protected void tryMatches() {
        Termutator[] t = commitTermutes();
        if (t!=null) {
            tryMatches(t);
        } else {
            tryMatch();
        }
    }

    public void tryMatches(Termutator[] t) {
        if (Param.SHUFFLE_TERMUTES && t.length > 1) {
            Util.shuffle(t, random);
        }

        tryMutate(t, -1);
    }

    protected Termutator[] commitTermutes() {
        int ts = termutes.size();
        if (ts > 0) {
            Termutator[] t = termutes.toArray(new Termutator[ts]);
            termutes.clear();
            return t;
        } else {
            return null;
        }
    }

    @Override
    public Versioning clear() {
        clear((BiConsumer)null);
        return this;
    }

    public Unify clear(@Nullable BiConsumer<Term,Term> each) {

        revert(0, each);

        if (Param.DEBUG) {
            assert (((TermHashMap) (xy.map)).other.isEmpty());
            assert (((TermHashMap) (xy.map)).id.isEmpty());
        }

        varDepth = 0;

        termutes.clear();

        constrained.clear(ConstrainedVersionedTerm::unconstrain);

        return this;
    }

    @Override
    public String toString() {
        return xy + "$" + this.ttl;
    }


    /**
     * whether assignable variable terms
     */
    public final boolean var(Op var) {
        return ((this.varBits & var.bit) != 0);
    }

    /** whether is or contains assignable variable terms */
    public final boolean var(Termlike x) {
        return x.hasAny(varBits);
    }

    /** allow common variable (tested for both types) */
    public final boolean varCommon(Op var) {
        return var!=VAR_PATTERN && var(var);
    }

    public final boolean varReverse(Op var) {
        assert(var!=VAR_PATTERN);
        //return var!=VAR_PATTERN && var!=VAR_QUERY && var(var);
        //return false;
        return true;
    }


    public final void forEach(BiConsumer<Term,Term> each) {
        forEach(versionedToBiConsumer(each));
    }

    public final void revert(int when, BiConsumer<Term,Term> each) {
        if (each==null)
            revert(0);
        else
            revert(when, versionedToBiConsumer(each));
    }


    static private Consumer<Versioned<Term>> versionedToBiConsumer(BiConsumer<Term, Term> each) {
        return (Versioned<Term> v)->{
            if (v instanceof KeyMultiVersioned) {
                each.accept(((KeyMultiVersioned<Term,Term>)v).key, v.get());
            } else if (v instanceof KeyUniVersioned) {
                each.accept(((KeyUniVersioned<Term,Term>)v).key, v.get());
            }
        };
    }

    /**
     * returns true if the assignment was allowed, false otherwise
     * args should be non-null. the annotations are removed for perf reasons
     */
    public final boolean putXY(final Variable x, Term y) {
        return xy.set(x, y);
    }


    public final void constrain(UnifyConstraint m) {
        Variable target = m.x;
        ConstrainedVersionedTerm targetVersioned = (ConstrainedVersionedTerm) xy.getOrCreateIfAbsent(target);
        targetVersioned.constrain(m);
        constrained.add(targetVersioned);
    }


    /** xdt and ydt must both not equal either XTERNAL or DTERNAL */
    public final boolean unifyDT(int xdt, int ydt) {
        //assert(xdt!=DTERNAL && xdt!=XTERNAL && ydt!=DTERNAL && ydt!=XTERNAL);
        return Math.abs(xdt - ydt) < dtTolerance;
    }

    public final Term resolvePosNeg(Term x) {
        Op o = x.op();
        boolean neg = o == NEG;
        Term xx;
        if (neg) {
            xx = x.unneg();
            o = xx.op();
        } else
            xx = x;

        if (o.var && var(o)) {
            Term y = resolve((Variable) xx);
            if (y != xx) {
                return neg ? y.neg() : y;
            }
        }

        return x; //no change
    }

    @Override
    public Term transformAtomic(Atomic atomic) {
        return atomic instanceof Variable ? resolve((Variable)atomic) : atomic;
    }

    public Unify emptyCopy(Predicate<Unify> each) {
        return new LazyUnify(this, each);
    }

    private static class ConstrainedVersionMap extends VersionMap<Variable, Term> {
        ConstrainedVersionMap(Versioning<Term> versioning, Map<Variable, Versioned<Term>> termMap) {
            super(versioning,
                    termMap,
                    1);
        }


        @Override
        protected Versioned<Term> newEntry(Variable x) {
            return new ConstrainedVersionedTerm(x, context);
        }


    }

    static final class ConstrainedVersionedTerm extends KeyUniVersioned<Term,Term> {

        /**
         * lazily constructed
         */
        public UnifyConstraint constraint;

        ConstrainedVersionedTerm(Term key, Versioning<Term> sharedContext) {
            super(key, sharedContext);
        }

        @Override
        protected int match(Term prevValue, Term nextValue) {
            if (prevValue.equals(nextValue))
                return 0;

            if (prevValue.unify(nextValue, (Unify) context)) {
                if (nextValue.hasAny(Op.Temporal)) {
                    //prefer more specific temporal matches, etc?
                    if (prevValue.hasXternal() && !nextValue.hasXternal()) {
                        return +1;
                    }
                }
                return 0;
            } else return -1;
        }

        @Override protected boolean valid(Term x) {
            UnifyConstraint c = this.constraint;
            return c == null || !c.invalid(x, (Unify)context);
        }

        void constrain(UnifyConstraint c) {
            //assert(value==null && constraint == null);
            constraint = c;
        }

        void unconstrain() {
            //assert(constraint != null && value==null);
            constraint = null;
        }
    }

    public static class LazyUnify extends Unify {
        private final Predicate<Unify> each;

        public LazyUnify(Unify u, Predicate<Unify> each) {
            super(u.varBits, u.random, u.items.length);
            commonVariables = u.commonVariables;
            dtTolerance = u.dtTolerance;
            //TODO any other flags?
            this.each = each;
        }

        @Override
        protected void tryMatch() {
            if (!each.test(this))
                stop();
        }
    }
}


