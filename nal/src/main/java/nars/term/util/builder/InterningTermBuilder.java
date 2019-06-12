package nars.term.util.builder;

import jcog.data.bit.MetalBitSet;
import jcog.data.byt.DynBytes;
import jcog.memoize.HijackMemoize;
import jcog.memoize.Memoizers;
import jcog.memoize.byt.ByteKeyExternal;
import nars.Op;
import nars.subterm.IntrinSubterms;
import nars.subterm.SortedSubterms;
import nars.subterm.Subterms;
import nars.term.Neg;
import nars.term.Term;
import nars.term.anon.Intrin;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.util.cache.Intermed;
import nars.term.util.cache.Intermed.InternedCompoundByComponents;
import nars.term.util.cache.Intermed.InternedSubterms;
import nars.term.util.conj.ConjBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Function;

import static nars.Op.*;
import static nars.term.atom.Bool.Null;
import static nars.time.Tense.DTERNAL;

/**
 * intern subterms and compounds.
 * the requirements for intern cache admission are configurable.
 **/
public class InterningTermBuilder extends HeapTermBuilder {


    protected static final int sizeDefault = Memoizers.DEFAULT_MEMOIZE_CAPACITY;
    public static final int volMaxDefault = 10;

    /**
     * memory-saving
     */
    static final boolean sortCanonically = true;
    private final static boolean cacheSubtermKeyBytes = false;

    private static final int ATOM_LENGTH_MAX = 8;

    private static final boolean resolveNeg = true;
    //    private final static boolean internNegs = false;
    static final boolean deepDefault = true;


    private final boolean deep;
    protected final int volInternedMax;

    final Function<InternedSubterms, Subterms> subterms, anonSubterms;
    final Function<InternedCompoundByComponents, Term>[] terms;

    private final String id;

    /** used for quickly determining if op type is internable */
    private final MetalBitSet termsInterned;

    private final HijackMemoize<String, Atom> atoms;


    public InterningTermBuilder() {
        this(sizeDefault, volMaxDefault);
    }

    public InterningTermBuilder(int size, int volMax) {
        this(UUID.randomUUID().toString(), size, volMax, deepDefault);
    }

    public InterningTermBuilder(String id, int cacheSizePerOp, int volInternedMax, boolean deep) {
        this.id = id;
        this.deep = deep;
        this.volInternedMax = volInternedMax;
        Op[] ops = values();
        terms = new Function[ops.length];

        atoms = new HijackMemoize<>(super::atom, cacheSizePerOp, 3);

        subterms = newOpCache("subterms",
                x -> TermConstructor.theSubterms(false, resolve(x.subs)), cacheSizePerOp * 2);
        anonSubterms = newOpCache("intrinSubterms",
                x -> new IntrinSubterms(x.subs), cacheSizePerOp);

        Function statements = newOpCache("statement", this::_statement, cacheSizePerOp * 3);

        for (int i = 0; i < ops.length; i++) {
            Op o = ops[i];
            if (o.atomic || (/*!internNegs && */o == NEG) || (o == FRAG)) continue;

            int s = cacheSizePerOp;

            //TODO use multiple PROD slices to decrease contention

            Function<Intermed.InternedCompoundByComponents, Term> c;
            if (o == CONJ) {
                c = newOpCache("conj",
                        x -> super.conj(true, x.dt, x.subs()), cacheSizePerOp);
            } else if (o.statement) {
                c = statements;
            } else {
                c = newOpCache(o.str,
                        x -> newCompound(ops[x.op], x.dt, x.subs(), x.key), s);
            }
            terms[i] = c;
        }

        termsInterned = MetalBitSet.bits(terms.length);
        for (int i = 0, termsLength = terms.length; i < termsLength; i++) {
            if (terms[i] != null) termsInterned.set(i);
        }

    }


    protected <I extends ByteKeyExternal, Y> Function<I, Y> newOpCache(String name, Function<I, Y> f, int capacity) {
        return Memoizers.the.memoizeByte(
                id + '_' + InterningTermBuilder.class.getSimpleName() + '_' + name,
                capacity, f);
    }



    private static Subterms subsInterned(Function<InternedSubterms, Subterms> m, Term[] t) {
        return m.apply(new InternedSubterms(t));
    }


    @Override public final Term compound(Op o, int dt, Subterms u) {
        boolean internable = internable(o, dt, u);
        return internable ?
                compoundInterned(o, dt, o.sortedIfNecessary(dt, u).arrayShared()) :
                super.compound(o, dt, u);
    }

    @Override
    public final Term compound(Op o, int dt, Term[] _u) {
        Term[] u = o.sortedIfNecessary(dt, _u);
        boolean internable = internable(o, dt, u);
        return internable ?
                compoundInterned(o, dt, u) :
                newCompound(o, dt, u);
    }

    private Term compoundInterned(Op op, int dt, Term[] u) {
        InternedCompoundByComponents x = new Intermed.InternedCompoundByComponentsArray(op, dt, u);
        return terms[x.op].apply(x);
    }

    @Override
    public final Subterms subterms(@Nullable Op o, Term... u) {
        return subterms(o, u, DTERNAL, null);
    }

    @Override
    protected Subterms subterms(Op o, Term[] t, int dt, @Nullable DynBytes key) {
        Subterms subs;
        if (t.length == 0)
            subs = EmptySubterms;
        else if (internable(t))
            subs = subtermsInterned(t);
        else
            subs = super.subterms(o, t);

        if (key != null && cacheSubtermKeyBytes) {
            if (dt == DTERNAL) //HACK TODO if temporal then the final bytes are for dt should be excluded from what the subterms gets.
                if (subs instanceof Subterms.SubtermsBytesCached)
                    ((Subterms.SubtermsBytesCached) subs).acceptBytes(key);
        }

        return subs;
    }
    private Subterms subtermsInterned(Term[] t) {
        //TODO separate cache for anon's
        if (Intrin.intrinsic(t))
            return subsInterned(anonSubterms, t);
        else if (sortCanonically)
            return SortedSubterms.the(t, this::subsInterned);
        else
            return subsInterned(t);
    }

    private Subterms subsInterned(Term... u) {
        return subsInterned(subterms, u);
    }




    private Term[] resolve(Term[] t) {
        if (!deep)
            return t;
        Term px = null;
        for (int i = 0, tLength = t.length; i < tLength; i++) {
            Term x = t[i];
            Term y = (i == 0 || x!=px) ?
                    resolve(x) :
                    t[i-1] /* re-use previous if identical */;
            if (y != x) // && y.equals(x))
                t[i] = y;
            px = x;
        }
        return t;
    }

    private Term resolve(Term _x) {
        if (_x instanceof Atomic)
            return _x;
        Op xo = _x.op();
        boolean negate = xo == NEG;
        Term xi;
        if (negate) {
            xi = _x.unneg();
            if (xi instanceof Atomic)
                return _x;
            xo = xi.op();
        } else {
            xi = _x;
        }

        if (internable(xo/*, x.dt()*/) && xi.volume() <= volInternedMax && xi.the()) {
            Term yi = terms[xo.id].apply(new Intermed.InternedCompoundByComponentsSubs(xi));
            return yi.negIf(negate);
        }

        return _x;
    }

    private boolean internable(Op op, int dt, Term[] u) {
        return internable(op) && internable(u);
    }
    private boolean internable(Op op, int dt, Subterms u) {
        return internable(op) && internable(u);
    }

    private boolean internable(Op op) {
        return termsInterned.get(op.id);
    }

    private boolean internable(Subterms subterms) {
        if (subterms.volume() > volInternedMax)
            return false;
        return subterms.AND(InterningTermBuilder::internableSub);
    }

    private boolean internable(Term[] subterms) {
        for (Term x : subterms) {
            if (x == Null)
                return false;
        }

        int volRemain = volInternedMax - subterms.length;
        for (Term x : subterms) {
            if ((volRemain -= x.volume()) < 0)
                return false;
            if (!internableSub(x))
                return false;
            volRemain++;
        }
        return true;
    }

    private static boolean internableSub(Term x) {
        return x.the();
    }

    @Override
    public Term statement(Op op, int dt, Term subject, Term predicate) {

        if (op==IMPL && dt == DTERNAL)
            dt = 0; //HACK temporary normalize

        if (!(subject instanceof Bool) && !(predicate instanceof Bool) &&
                (subject.volume() + predicate.volume() < volInternedMax) &&
                internableSub(subject) && internableSub(predicate) &&
                ((op==IMPL) || !subject.equals(predicate))
        ) {

            boolean negate = false;

            //quick preparations to reduce # of unique entries

            if (!((op == INH || op == SIM) && ((dt!=0) || subject.equals(predicate)))) {

                if (op == IMPL) {
                    negate = (predicate instanceof Neg);
                    if (negate)
                        predicate = predicate.unneg();
                }


                if (op == SIM) {
                    //commutive order: pre-sort by swapping to avoid saving redundant mappings
                    if (subject.compareTo(predicate) > 0) {
                        Term x = predicate;
                        predicate = subject;
                        subject = x;
                    }
                }

                return this.terms[op.id].apply(new Intermed.InternedCompoundByComponentsArray(op, dt, subject, predicate)).negIf(negate);
            }

            //return statements.apply(InternedCompound.get(op, dt, subject, predicate));
        }

//        if (deep) {
//            Term sr = resolve(subject);
//            if (sr != null) subject = sr;
//            Term pr = resolve(predicate);
//            if (pr != null) predicate = pr;
//        }

        return super.statement(op, dt, subject, predicate);
    }

    private Term _statement(Intermed.InternedCompoundByComponents c) {
        Term[] s = c.subs();
        return super.statement(Op.the(c.op), c.dt, s[0], s[1]);
    }

    /** compound1 does not traverse the subterms interning pathway so an explicit resolution step for the only argument is applied here */
    @Override protected Term newCompound1(Op o, Term x) {
        return super.newCompound1(o, resolve(x));
    }

    @Override public Term neg(Term x) {
        return resolveNeg ? super.neg(resolve(x)) : super.neg(x);
    }

    @Override
    public Atom atom(String id) {
        return (id.length() < ATOM_LENGTH_MAX) ? atoms.apply(id) : super.atom(id);
    }

    @Override
    public Term conj(int dt, Term[] u) {

        u = ConjBuilder.preSort(dt,u);

        return u.length > 1 && internable(CONJ, dt, u) ?
                terms[CONJ.id].apply(
                        new Intermed.InternedCompoundByComponentsArray(CONJ, dt, u)) :
                super.conj(true, dt, u);
    }


}
