package nars.unify;

import jcog.TODO;
import jcog.Util;
import jcog.data.list.FasterList;
import jcog.version.UniVersioned;
import jcog.version.VersionMap;
import jcog.version.Versioned;
import jcog.version.Versioning;
import nars.Op;
import nars.Param;
import nars.term.Term;
import nars.term.Termlike;
import nars.term.Variable;
import nars.term.util.map.TermHashMap;
import nars.term.util.transform.Subst;
import nars.unify.constraint.UnifyConstraint;
import nars.unify.mutate.Termutator;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Random;
import java.util.Set;

import static nars.Op.NEG;


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
    public final Set<Termutator> termutes =
            //new ArrayHashSet<>(4);
            new UnifiedSet(4, 0.99f);

    public final VersionMap<Variable, Term> xy;

    /** bits of the unifiable variables; variables not unifiable are tested for equality only */
    public int varBits;

    /** bits of the unifiable variables + the temporal compounds, indicating unification can not rely on equality for fast failure */
    public int nonConstantBits;

    public Random random;

    /**
     * whether the variable unification allows to happen in reverse (a variable in Y can unify a constant in X)
     */


    public int dtTolerance = 0;

    public boolean commonVariables = true;

    private FasterList<ConstrainedVersionedTerm> constrained = new FasterList();


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
        this.nonConstantBits = varBits | Op.Temporal;
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


    protected void tryMatches() {
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

        if (!constrained.isEmpty()) {
            constrained.forEach(ConstrainedVersionedTerm::unconstrain);
            constrained.clear();
        }

        return this;
    }

    @Override
    public String toString() {
        return xy + "$" + ((Versioning<Term>) this).ttl;
    }


    /**
     * whether the op is assignable
     */
    public final boolean matchType(Op var) {
        return ((this.varBits & var.bit) != 0);
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
        return !x.hasAny(nonConstantBits);
    }
    public final boolean vars(Termlike x) {
        return !x.hasAny(varBits);
    }


    public void constrain(UnifyConstraint m) {
        Variable target = m.x;
        ConstrainedVersionedTerm targetVersioned = (ConstrainedVersionedTerm) xy.getOrCreateIfAbsent(target);
        targetVersioned.constrain(m);
        constrained.add(targetVersioned);
    }


    /** xdt and ydt must both not equal either XTERNAL or DTERNAL */
    public boolean unifyDT(int xdt, int ydt) {
        //assert(xdt!=DTERNAL && xdt!=XTERNAL && ydt!=DTERNAL && ydt!=XTERNAL);
        return Math.abs(xdt - ydt) < dtTolerance;
    }

    public Term tryResolve(Term x) {
        Op o = x.op();
        boolean neg = o == NEG;
        Term xx;
        if (neg) {
            xx = x.unneg();
            o = xx.op();
        } else
            xx = x;

        if (o.var && matchType(o)) {
            Term y = resolve((Variable) xx);
            if (y != xx) {
                return neg ? y.neg() : y;
            }
        }

        return x; //no change
    }


    private static class ConstrainedVersionMap extends VersionMap<Variable, Term> {
        ConstrainedVersionMap(Versioning<Term> versioning, Map<Variable, Versioned<Term>> termMap) {
            super(versioning,
                    termMap,
                    1);
        }


        @Override
        protected Versioned<Term> newEntry(Variable x) {
            return new ConstrainedVersionedTerm(context);
        }


    }

    static final class ConstrainedVersionedTerm extends UniVersioned<Term> {

        /**
         * lazily constructed
         */
        public UnifyConstraint constraint;

        ConstrainedVersionedTerm(Versioning<Term> sharedContext) {
            super(sharedContext);
        }

        @Override
        protected int match(Term prevValue, Term nextValue) {

            if (prevValue.unify(nextValue, (Unify) context)) {
                if (prevValue!=nextValue && nextValue.hasAny(Op.Temporal)) {
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

}


