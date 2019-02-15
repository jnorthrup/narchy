package nars.term.util.builder;

import jcog.data.byt.DynBytes;
import jcog.memoize.Memoizers;
import nars.Op;
import nars.subterm.AnonVector;
import nars.subterm.SortedSubterms;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.util.cache.Intermed;
import nars.term.util.cache.Intermed.InternedCompoundByComponents;
import nars.term.util.cache.Intermed.InternedSubterms;
import nars.term.util.conj.Conj;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Function;

import static nars.Op.*;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.dtSpecial;

/**
 * intern subterms and compounds.
 * the requirements for intern cache admission are configurable.
 **/
public class InterningTermBuilder extends HeapTermBuilder {


    protected static final int sizeDefault = Memoizers.DEFAULT_MEMOIZE_CAPACITY;
    protected static final int volMaxDefault = 13;
    protected static final boolean deepDefault = true;

    /**
     * memory-saving
     */
    public static final boolean sortCanonically = true;
    private final static boolean internNegs = false;
    private final static boolean cacheSubtermKeyBytes = false;


    private final boolean deep;
    protected final int volInternedMax;

    final Function<InternedSubterms, Subterms> subterms, anonSubterms;
    final Function<InternedCompoundByComponents, Term>[] terms;

    private final String id;


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


        subterms = newOpCache("subterms",
                (InternedSubterms x) -> theSubterms(resolve(x.subs)), cacheSizePerOp * 2);
        anonSubterms = newOpCache("anonSubterms",
                (InternedSubterms x) -> new AnonVector(x.subs), cacheSizePerOp);

        Function statements = newOpCache("statement", this::_statement, cacheSizePerOp * 3);

        for (int i = 0; i < ops.length; i++) {
            Op o = ops[i];
            if (o.atomic || (!internNegs && o == NEG)) continue;

            int s = cacheSizePerOp;

            //TODO use multiple PROD slices to decrease contention

            Function<Intermed.InternedCompoundByComponents, Term> c;
            if (o == CONJ) {
                c = newOpCache("conj",
                        (InternedCompoundByComponents j) -> super.conj(true, j.dt, j.subs()), cacheSizePerOp);
            } else if (o.statement) {
                c = statements;
            } else {
                c = newOpCache(o.str,
                        (InternedCompoundByComponents x) -> theCompound(ops[x.op], x.dt, x.subs(), x.key), s);
            }
            terms[i] = c;
        }


    }


    protected <I extends Intermed, Y> Function<I, Y> newOpCache(String name, Function<I, Y> f, int capacity) {
        return Memoizers.the.memoizeByte(
                id + '_' + InterningTermBuilder.class.getSimpleName() + '_' + name,
                capacity, f);
    }

    @Override
    protected Subterms subterms(Op o, Term[] t, int dt, @Nullable DynBytes key) {
        Subterms subs = super.subterms(o, t, dt, key);

        if (key != null && cacheSubtermKeyBytes) {
            if (dt == DTERNAL) //HACK TODO if temporal then the final bytes are for dt should be excluded from what the subterms gets.
                if (subs instanceof Subterms.SubtermsBytesCached)
                    ((Subterms.SubtermsBytesCached) subs).acceptBytes(key);
        }

        return subs;
    }

    private Term compoundInterned(Intermed.InternedCompoundByComponents x) {
        return terms[x.op].apply(x);
    }

    private static Subterms subsInterned(Function<InternedSubterms, Subterms> m, Term[] t) {
        return m.apply(new InternedSubterms(t));
    }


    @Override
    public final Term compound(Op o, int dt, Term[] u) {
        boolean internable = internableRoot(o, dt, u);
//        if (!internable) {
//            //internable(op, dt, u);
//            System.out.println("why: " + op + " " + dt + " " + Arrays.toString(u));
//        }

        return internable ?
                compoundInterned(o, dt, o.sortedIfNecessary(dt, u)) :
                super.compound(o, dt, u);
    }

    private Term compoundInterned(Op op, int dt, Term[] u) {
        return compoundInterned(new Intermed.InternedCompoundByComponentsArray(op, dt, u));
    }


    @Override
    protected Subterms subterms(Op inOp, Term... t) {

        if (t.length == 0)
            return EmptySubterms;

        if (internableSubs(t)) {
            //TODO separate cache for anon's
            if (isAnon(t))
                return subsInterned(anonSubterms, t);

            if (sortCanonically) {
                return SortedSubterms.the(t, this::newSubterms, false);
            } else {
                return subsInterned(subterms, t);
            }
        } else {
            return theSubterms(t);
        }
    }

    private Subterms newSubterms(Term... u) {
        return subsInterned(subterms, u);
    }

    private Subterms theSubterms(Term... t) { return super.theSubterms(false, t); }


    private Term[] resolve(Term[] t) {
        if (!deep)
            return t;
        Term px = null;
        for (int i = 0, tLength = t.length; i < tLength; i++) {
            Term x = t[i];
            Term y = (i == 0 || x!=px) ?
                    resolve(x) :
                    t[i-1] /* re-use previous if identical */;
            if (y != x && y.equals(x))
                t[i] = y;
            px = x;
        }
        return t;
    }

    private Term resolve(Term x) {
        if (x instanceof Atomic)
            return x;
        int v = x.volume();
        if (v <= 1 || v > volInternedMax) {
            return x;
        } else {
            Op xo = x.op();
            boolean negate;
            if (!internNegs) {
                negate = xo == NEG;
                if (negate) {
                    Term xx = x.unneg();
                    if (xx instanceof Atomic)
                        return x; //HACK do this earlier
                    x = xx;
                    xo = x.op();
                }
            } else {
                negate = false;
            }
            if (internableRoot(xo, x.dt()) && x.the()) {
                Term y = terms[xo.id].apply(new Intermed.InternedCompoundByComponentsSubs(x));
                if (y != null)
                    return negate ? y.neg() : y;
            }
            return x;
        }
    }

    protected boolean internableRoot(Op op, int dt, Term[] u) {
        boolean i = internableRoot(op, dt) && internableSubs(u);
//        if (!i) {
//            System.out.println(op + " " + dt + " " + Arrays.toString(u));
//        }
        return i;
    }

    protected static boolean internableRoot(Op op, int dt) {
        return !op.atomic && (internNegs || op != NEG)
                //&& Tense.dtSpecial(dt)
                ;
    }

    private boolean internableSubs(Term[] subterms) {

        int volRemain = volInternedMax;
        for (Term x : subterms) {
            if ((volRemain -= x.volume()) < 0)
                return false;
            if (!internableSub(x))
                return false;
        }


        return true;
    }

    private static boolean internableSub(Term x) {
        return x.the();
    }

    @Override
    public Term statement(Op op, int dt, Term subject, Term predicate) {


        if (!(subject instanceof Bool) && !(predicate instanceof Bool) &&
                (subject.volume() + predicate.volume() < volInternedMax) &&
                internableSub(subject) && internableSub(predicate)) {

            boolean negate = false;

            //quick preparations to reduce # of unique entries

            if (!((op == INH || op == SIM) && (dt != DTERNAL || subject.equals(predicate)))) {

                if (op == IMPL) {
                    negate = (predicate.op() == NEG);
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
        return super.statement(Op.ops[c.op], c.dt, s[0], s[1]);
    }

    /** compound1 does not traverse the subterms interning pathway so an explicit resolution step for the only argument is applied here */
    @Override protected Compound compound1(Op o, Term x) {
        return super.compound1(o, resolve(x));
    }

    @Override
    public Term conj(int dt, Term[] u) {

        if (dtSpecial(dt))
            u = Conj.preSort(dt, u);

        if (u.length > 1 && internableRoot(CONJ, dt, u)) {
            return terms[CONJ.id].apply(new Intermed.InternedCompoundByComponentsArray(CONJ, dt, u));
        }

        return super.conj(true, dt, u);
    }


}
