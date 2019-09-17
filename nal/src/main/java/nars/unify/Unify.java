package nars.unify;

import jcog.TODO;
import jcog.Util;
import jcog.WTF;
import jcog.data.list.FasterList;
import jcog.data.set.ArrayHashSet;
import jcog.version.*;
import nars.NAL;
import nars.Op;
import nars.subterm.Subterms;
import nars.subterm.TermList;
import nars.term.*;
import nars.term.atom.Bool;
import nars.unify.constraint.UnifyConstraint;
import nars.unify.mutate.Termutator;
import nars.unify.unification.DeterministicUnification;
import nars.unify.unification.MapUnification;
import nars.unify.unification.OneTermUnification;
import nars.unify.unification.Termutifcation;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;
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

//    /** recursion limiter HACK
//     * TODO use a real stack of arbitrarily length for detecting cycles */
//    public int varDepth = 0;

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
        this(varBits, random, stackMax,
                //new TermHashMap<>()
                //new UnifiedMap(32, 0.99f)
                new HashMap(16, 0.5f)
        );
    }

    protected Unify(int varBits, Random random, int stackMax, Map/*<Variable,Versioned<Term>>*/ termMap) {
        super(stackMax);

        this.random = random;
        this.varBits = varBits;

        this.xy = new ConstrainedVersionMap( termMap);
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
    private final MyUnifyTransform transform = new MyUnifyTransform();

    public MyUnifyTransform transform() {
        return transform;
    }

    public final Term apply(Term x) {
        return transform().apply(x);
    }


    public static Function<Term, Term> transform(Function<Variable,Term> resolve) {
        return new AbstractUnifyTransform.LambdaUnifyTransform(resolve);
    }

    /**
     * called each time all variables are satisfied in a unique way
     *
     * @return whether to continue on any subsequent matches
     */
    protected abstract boolean match();


    public final boolean tryMutate(Termutator[] chain, int next) {

        if (++next < chain.length) {

            chain[next].mutate(chain, next, this);

            return use(NAL.derive.TTL_COST_MUTATE_COMPONENT);

        } else {

            boolean kontinue = match();

            return use(NAL.derive.TTL_COST_MUTATE) && kontinue;
        }

    }


    /**
     * completely dereferences a target (usually a variable)
     */
    public Term resolveVar(final Variable x) {
        int s = this.size;

        if (s == 0) return x; //nothing assigned


        Term /*Variable*/ z = x, y;

        int safety = NAL.unify.UNIFY_VAR_RECURSION_DEPTH_LIMIT;
            //Math.min(s, NAL.unify.UNIFY_VAR_RECURSION_DEPTH_LIMIT) + 1;

        do {

            y = xy.get(z);

            if (y==null)
                return z; //done

            if (y==x || !(y instanceof Variable))
                return y; //cycle or early exit

            z = y;

        } while (--safety > 0);

        throw new WTF("var cycle detected");
    }

//    /** UNTESTED */
//    @Nullable public Term resolve(final Variable x, boolean store, Function<Variable,Term> resolver) {
//        //if (size == 0)
//            //TODO fast insert
//
//        Variable /*Variable*/ z = x;
//
//
//        do {
//            Term y = xy.get(z);
//            if (y==null) {
//                Term r = resolver.apply(z);
//                if (store && r!=null) {
//                    if (!putXY(x, r))
//                        return null; //fail
//                }
//                return r;
//            } else if (y instanceof Variable) {
//                z = (Variable) y;
//            } else {
//                return y;
//            }
//        } while (true);
//    }
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
                return matches();
//            else {
//                if (!couldMatch())
//                    return false;
//                else
            return true;
//            }
        }
        return false;
    }


    public Unification unification(boolean clear) {
        FasterList<Term> xyPairs = new FasterList(size * 2 /* estimate */);

        Termutator[] termutes = commitTermutes(true);
        if (termutes == Termutator.TerminateTermutator)
            throw new TODO("this means fail");

        BiConsumer<Term, Term> eachXY = xyPairs::addAll;
        if (clear) {
            clear(versionedToBiConsumer(eachXY));
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

    /** called after unifying 'start' mode to test if unification can proceed to 'finish' mode */
    private boolean couldMatch() {
        //just needs to detect if 'TerminateTermutator' is returned.  otherwise null or some other result is considered valid for proceeding
        Termutator[] t = commitTermutes(false);
        return t!=Termutator.TerminateTermutator;
    }

    protected boolean matches() {
        Termutator[] t = commitTermutes(true);
        if (t == null)
            match();
        else if (t == Termutator.TerminateTermutator)
            return false;
        else
            matches(t);
        return true;
    }

    public void matches(Termutator[] t) {

        tryMutate(t, -1);
    }

    private Termutator[] commitTermutes(boolean finish) {
        int ts = termutes.size();
        if (ts > 0) {
            FasterList<Termutator> tl = termutes.list;
            for (int i = 0; i < ts; i++) {
                Termutator x = tl.get(i);
                @Nullable Termutator y = x.preprocess(this);
                if (y == null) {
                    termutes.clear();
                    return Termutator.TerminateTermutator;
                } else if (y == Termutator.NullTermutator) {
                    tl.remove(i);
                    i--;
                    ts--;
                } else if (x!=y)
                    tl.setFast(i, y);
            }
            if (ts==0)
                return null;


            if (finish) {
                Termutator[] tt = tl.toArrayRecycled(Termutator[]::new);
                termutes.clear();

                if (NAL.SHUFFLE_TERMUTES && tt.length > 1) {
                    Util.shuffle(tt, random);
                }

                return tt;
            } else
                return null;
        } else {
            return null;
        }
    }

    @Override
    public Unify clear() {
        clear(null);
        return this;
    }

    public Unify clear(@Nullable Consumer<Versioned<Term>> each) {

        if (size > 0) {
            constrained.clear(ConstrainedVersionedTerm::unconstrain);

            if (each != null)
                revert(0, each);
            else
                revert(0);
        }

        termutes.clear();

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
        return var(var.bit);
    }

    public final boolean var(int varBit) {
        return (this.varBits & varBit) != 0;
    }

    /** whether is or contains assignable variable terms */
    public final boolean varIn(Termlike x) {
        return x.hasAny(varBits);
    }

    public final boolean var(Term x) {
        return x instanceof Variable && var((Variable)x);
    }
    public final boolean var(Variable x) {
        return var(x.opBit());
    }

    /** how many matchable variables are present */
    public int vars(Term x) {
        if (x instanceof Compound) {
            if (x.hasAny(varBits)) {
                int v = 0;
                if (0 != (varBits & Op.VAR_PATTERN.bit)) v += x.varPattern();
                if (0 != (varBits & Op.VAR_QUERY.bit)) v += x.varQuery();
                if (0 != (varBits & Op.VAR_DEP.bit)) v += x.varDep();
                if (0 != (varBits & Op.VAR_INDEP.bit)) v += x.varIndep();
                return v;
            }
        } else if (x instanceof Variable) {
            if (0!=(varBits & x.opBit())) return 1;
        }
        return 0;
    }

    public final boolean canPut(Term target, Term value) {
        return target instanceof Variable && canPut(target.op(), value);
    }

    /** can x be assigned to y (y <= x) */
    public final boolean canPut(Op target, Term value) {
        if (!var(target))
            return false;
        int exc;
        switch (target) {
            case VAR_DEP:
                exc = Op.VAR_PATTERN.bit | Op.VAR_QUERY.bit | Op.VAR_INDEP.bit | Op.VAR_DEP.bit;
                break;
            case VAR_INDEP:
                exc = Op.VAR_PATTERN.bit | Op.VAR_QUERY.bit | Op.VAR_INDEP.bit;
                break;
            case VAR_QUERY:
                exc = Op.VAR_PATTERN.bit | Op.VAR_QUERY.bit;
                break;
            case VAR_PATTERN:
                exc = Op.VAR_PATTERN.bit;
                break;
            default:
                return false;
        }
        return !value.hasAny(exc);
    }


    public final void forEach(BiConsumer<Term,Term> each) {
        forEach(versionedToBiConsumer(each));
    }

    private void revertTerms(int when, BiConsumer<Term, Term> each) {
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


        Term Y = y;
        //Term Y = resolveTerm(y, false);
        //Term Y = resolveTerm(y, true);
        if (Y == Null)
            return false;
        else {
            return xy.set(x, Y);
        }
    }


    public final void constrain(UnifyConstraint<?> m) {
        ConstrainedVersionedTerm target = (ConstrainedVersionedTerm) xy.getOrCreateIfAbsent(m.x);
        target.constrain(m);
        constrained.add(target);
    }

    public final boolean unifyDT(Term x, Term y) {
        int xdt = x.dt();
        if (xdt == XTERNAL) return true;

        int ydt = y.dt();
        if (ydt == XTERNAL || ydt==xdt) return true;

        if (xdt == DTERNAL) xdt = 0; //HACK
        if (ydt == DTERNAL) ydt = 0; //HACK

        return xdt == ydt || unifyDT(xdt, ydt);
    }

    /** xdt and ydt must both not equal either XTERNAL or DTERNAL */
    private boolean unifyDT(int xdt, int ydt) {
        //assert(xdt!=DTERNAL && xdt!=XTERNAL && ydt!=DTERNAL && ydt!=XTERNAL);
        return Math.abs(xdt - ydt) <= dtTolerance;
    }

    public final Term resolveTerm(Term x) {
        return resolveTerm(x, false);
    }
    public final Term resolveTermRecurse(Term x) {
        return resolveTerm(x, true);
    }

    /** full resolution of a term */
    public final Term resolveTerm(Term _x, boolean recurse) {
        if (this.size == 0 && !recurse)
            return _x;

        boolean neg = _x instanceof Neg;
        Term x = neg ? _x.unneg() : _x;

        Term y;
        if (this.size > 0) {
            boolean vx = var(x);
            y = vx ? resolveVar((Variable) x) : x;
        } else
            y = x;

        if (recurse && y instanceof Compound && y.hasAny(varBits)) {
            Term z = transform().applyPosCompound((Compound) y); //recurse
            //Term z = transform().applyCompound((Compound) y); //recurse
            //Term z = y.replace(xy); //recurse

            y = z;
        }

        return x!=y ? y.negIf(neg) : _x;
    }

    public final Subterms resolveSubs(Subterms x) {
        return x.transformSubs(this::resolveTerm, null);
    }
    public final Subterms resolveSubsRecurse(Subterms x) {
        return x.transformSubs(this::resolveTermRecurse, null);
    }

    @Nullable public final TermList resolveListIfChanged(Subterms x) {
        return resolveListIfChanged(x, false);
    }

    @Nullable public final TermList resolveListIfChanged(Subterms x, boolean recurse) {
        Subterms y = recurse ? resolveSubsRecurse(x) : resolveSubs(x);
        if (y == null)
            return new TermList(Bool.Null); //HACK
        else if (y != x)
            return y instanceof TermList ? (TermList) y : y.toList();
        else
            return null;
    }


    private final class ConstrainedVersionMap extends VersionMap<Variable, Term> {
        ConstrainedVersionMap(Map<Variable, Versioned<Term>> termMap) {
            super(Unify.this, termMap);
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
        public UnifyConstraint<Unify> constraint;

        ConstrainedVersionedTerm(Term key) {
            super(key);
        }


        @Override protected boolean valid(Term x, Versioning<Term> context) {
            UnifyConstraint<Unify> c = this.constraint;
            return c == null || !c.invalid(x, (Unify)context);
        }

        void constrain(UnifyConstraint c) {
//            if (constraint!=null)
//                throw new WTF("constraint replacement: " + constraint + " -> " + c);
//            assert(constraint == null);
            constraint = c;
        }

        void unconstrain() {
            //if (NAL.DEBUG) {  assert(constraint != null && value==null); }
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
        protected boolean match() {
            return true;
        }
    }

    protected class MyUnifyTransform extends AbstractUnifyTransform {
        @Override protected Term resolveVar(Variable v) {
            return Unify.this.resolveVar(v);
        }

        @Override
        public Term applyPosCompound(Compound x) {
            boolean recurse = evalInline();

            if (!recurse && (size==0 || !x.hasAny(varBits))) {
                return x;
            }

            return super.applyPosCompound(x);
        }
    }

}


