package nars.unify;

import jcog.Util;
import jcog.WTF;
import jcog.data.list.FasterList;
import jcog.data.set.ArrayHashSet;
import jcog.version.*;
import nars.NAL;
import nars.Op;
import nars.term.Term;
import nars.term.Termlike;
import nars.term.Variable;
import nars.term.atom.Atomic;
import nars.term.util.map.TermHashMap;
import nars.term.util.transform.AbstractTermTransform;
import nars.term.util.transform.TermTransform;
import nars.unify.constraint.UnifyConstraint;
import nars.unify.mutate.Termutator;
import nars.unify.unification.DeterministicUnification;
import nars.unify.unification.MapUnification;
import nars.unify.unification.OneTermUnification;
import nars.unify.unification.Termutifcation;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static nars.Op.NEG;
import static nars.Op.VAR_PATTERN;
import static nars.unify.Unification.Null;
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
public abstract class Unify extends Versioning<Term> {

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
        if (cost > 0)
            ttl = Math.max(0, ttl - cost);
        return live();
    }

    public <U extends Unify> U random(Random random) {
        this.random = random;
        return (U) this;
    }

    /** default unify substitution */
    private final UnifyTransform transform = new UnifyTransform() {
        @Override protected Term resolve(Variable v) {
            return Unify.this.resolve(v);
        }
    };

    public TermTransform transform() {
        return transform;
    }

    public final Term apply(Term x) {
        return transform().apply(x);
    }


    public static TermTransform transform(Function<Variable,Term> resolve) {
        return new UnifyTransform.LambdaUnifyTransform(resolve);
    }

    /**
     * called each time all variables are satisfied in a unique way
     *
     * @return whether to continue on any subsequent matches
     */
    protected abstract boolean tryMatch();


    public final boolean tryMutate(Termutator[] chain, int next) {

        if (++next < chain.length) {

            chain[next].mutate(chain, next, this);

            //return use(Param.TTL_MUTATE_COMPONENT);
            return true;

        } else {

            boolean kontinue = tryMatch();

            return use(NAL.derive.TTL_COST_MUTATE) && kontinue;
        }

    }


    /**
     * completely dereferences a target (usually a variable)
     */
    public Term resolve(final Variable x) {
        if (size == 0)
            return x;

        Term /*Variable*/ z = x, y;

        int safety = NAL.unify.UNIFY_VAR_RECURSION_DEPTH_LIMIT;

        do {
            y = xy.get(z);
            if (y==null)
                break;
            if (y==x)
                return x; //cycle?
            z = y;

            if (--safety == 0) {
                if (NAL.DEBUG)
                    throw new WTF("var cycle detected");
                return x;
            }

        } while (z instanceof Variable);
        return z;
    }

    /** UNTESTED */
    @Nullable public Term resolve(final Variable x, boolean store, Function<Variable,Term> resolver) {
        //if (size == 0)
            //TODO fast insert

        Variable /*Variable*/ z = x;


        do {
            Term y = xy.get(z);
            if (y==null) {
                Term r = resolver.apply(z);
                if (store && r!=null) {
                    if (!putXY(x, r))
                        return null; //fail
                }
                return r;
            } else if (y instanceof Variable) {
                z = (Variable) y;
            } else {
                return y;
            }
        } while (true);
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


    public Unification unification(boolean clear) {
        FasterList<Term> xyPairs = new FasterList(size * 2 /* estimate */);

        Termutator[] termutes = commitTermutes();

        BiConsumer<Term, Term> eachXY = xyPairs::addAll;
        if (clear) {
            clear(eachXY);
        } else {
            forEach(eachXY);
        }

        DeterministicUnification base;
        int n = xyPairs.size()/2;
        if (n == 0)
            base = Self;
        else if (n == 1)
            base = new OneTermUnification(xyPairs.get(0), xyPairs.get(1));
        else
            base = new MapUnification().putIfAbsent(xyPairs);

        return (termutes != null) ?
            new Termutifcation(this, base, termutes) : base;
    }

    @Deprecated public final Unification unification(Term x, Term y, int discoveryTTL) {
        Unification u = unification(x, y);
        if (u instanceof Termutifcation) {
            ((Termutifcation)u).discover(this, Integer.MAX_VALUE, discoveryTTL);
        }
        return u;
    }

    /** will have been cleared() */
    protected Unification unification(Term x, Term y) {
        if (!unify(x, y, false)) {
            clear();
            return Null;
        } else {
            return unification(true);
        }
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
        if (NAL.SHUFFLE_TERMUTES && t.length > 1) {
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
        clear(null);
        return this;
    }

    public Unify clear(@Nullable BiConsumer<Term,Term> each) {

        revert(0, each);

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
        //assert(var!=VAR_PATTERN);
        return var!=VAR_PATTERN
                //&& var!=VAR_QUERY ;
                && var(var);
        //return false;
        //return true;
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


    private static final class ConstrainedVersionMap extends VersionMap<Variable, Term> {
        ConstrainedVersionMap(Versioning<Term> versioning, Map<Variable, Versioned<Term>> termMap) {
            super(versioning, termMap);
        }

        @Override
        protected Versioned<Term> newEntry(Variable x) {
            return new ConstrainedVersionedTerm(x);
        }
    }

    static final class ConstrainedVersionedTerm extends KeyUniVersioned<Term,Term> {

        /**
         * lazily constructed
         */
        public UnifyConstraint constraint;

        ConstrainedVersionedTerm(Term key) {
            super(key);
        }

        @Override
        protected int match(Term prevValue, Term nextValue) {
            if (prevValue.equals(nextValue))
                return 0;

//            if (prevValue.unify(nextValue, (Unify) context)) {
//                if (nextValue.hasAny(Op.Temporal)) {
//                    //prefer more specific temporal matches, etc?
//                    if (prevValue.hasXternal() && !nextValue.hasXternal()) {
//                        return +1;
//                    }
//                }
//                return 0;
//            } else
            else
                return -1;
        }

        @Override protected boolean valid(Term x, Versioning<Term> context) {
            UnifyConstraint c = this.constraint;
            return c == null || !c.invalid(x, (Unify)context);
        }

        void constrain(UnifyConstraint c) {
            if (NAL.DEBUG) { assert(value==null && constraint == null); }
            constraint = c;
        }

        void unconstrain() {
            if (NAL.DEBUG) {  assert(constraint != null && value==null); }
            constraint = null;
        }
    }

    /** extension adapter, can be used to extend a Unify */
    public static class ContinueUnify extends Unify {

        /**
         * if xy is null then inherits the Map<Term,Term> from u
         * otherwise, no mutable state is shared between parent and child
         */
        public ContinueUnify(Unify parent, @Nullable Map<Term,Term> xy) {
            super(parent.varBits, parent.random, parent.items.length, xy!=null ? xy : parent.xy);
            commonVariables = parent.commonVariables;
            dtTolerance = parent.dtTolerance;
            //TODO any other flags?
        }

        @Override
        protected boolean tryMatch() {
            return true;
        }
    }

    public abstract static class UnifyTransform extends AbstractTermTransform.NegObliviousTermTransform {

        abstract protected Term resolve(Variable v);

        @Override
        public Term applyAtomic(Atomic x) {
            if (x instanceof Variable) {
                Term y = resolve((Variable) x);
                if (y != null)
                    return y;
            }
            return x;
        }

        public static class LambdaUnifyTransform extends UnifyTransform {
            private final Function<Variable, Term> resolve;

            public LambdaUnifyTransform(Function<Variable, Term> resolve) {
                this.resolve = resolve;
            }

            @Override protected Term resolve(Variable v) {
                return resolve.apply(v);
            }
        }
    }
}


