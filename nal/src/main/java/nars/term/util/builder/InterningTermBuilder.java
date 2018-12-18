package nars.term.util.builder;

import jcog.data.byt.DynBytes;
import jcog.data.byt.RecycledDynBytes;
import jcog.memoize.Memoizers;
import jcog.memoize.byt.ByteHijackMemoize;
import nars.Op;
import nars.subterm.AnonVector;
import nars.subterm.SortedSubterms;
import nars.subterm.Subterms;
import nars.term.Term;
import nars.term.Terms;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.util.InternedCompound;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.UUID;
import java.util.function.Function;

import static nars.Op.*;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;

/**
 * can intern subterms and compounds.
 * the requirements for intern cache admission are configurable.
 **/
public class InterningTermBuilder extends HeapTermBuilder {


    protected static final int DEFAULT_SIZE = Memoizers.DEFAULT_MEMOIZE_CAPACITY;
    protected static final int maxInternedVolumeDefault = 32;
    protected static boolean deepDefault = true;

    /** memory-saving */
    private static final boolean sortCanonically = true;
    private boolean cacheSubtermKeyBytes = false;

    private final boolean deep;
    protected final int volInternedMax;

    final ByteHijackMemoize<InternedCompound, Subterms> subterms, anonSubterms;
    final ByteHijackMemoize<InternedCompound, Term>[] terms;

    private final String id;


    public InterningTermBuilder() {
        this(UUID.randomUUID().toString(), deepDefault, maxInternedVolumeDefault, DEFAULT_SIZE);
    }

    public InterningTermBuilder(String id, boolean deep, int volInternedMax, int cacheSizePerOp) {
        this.id = id;
        this.deep = deep;
        this.volInternedMax = volInternedMax;
        Op[] ops = values();
        terms = new ByteHijackMemoize[ops.length];


        subterms = newOpCache("subterms", x -> theSubterms(x.rawSubs.get()), cacheSizePerOp * 2);
        anonSubterms = newOpCache("anonSubterms", x -> new AnonVector(x.rawSubs.get()), cacheSizePerOp);
        ByteHijackMemoize statements = newOpCache("statement", this::_statement, cacheSizePerOp * 3);

        for (int i = 0; i < ops.length; i++) {
            Op o = ops[i];
            if (o.atomic || o == NEG) continue;

            int s = cacheSizePerOp;
            if (o == PROD)
                s *= 2; //HACK since PROD also serves as subterm cache

            //TODO use multiple PROD slices to decrease contention

            ByteHijackMemoize c;
            if (o == CONJ) {
                c = newOpCache("conj", j -> super.conj(j.dt, j.rawSubs.get()), cacheSizePerOp);
            } else if (o.statement) {
                c = statements;
            } else {
                c = newOpCache(o.str, x -> theCompound(ops[x.op], x.dt, x.rawSubs.get(), x.key), s);
            }
            terms[i] = c;
        }


    }


    protected ByteHijackMemoize newOpCache(String name, Function<InternedCompound, ?> f, int capacity) {
        ByteHijackMemoize h = new ByteHijackMemoize(f, capacity, 4, false);
        Memoizers.the.add(id + '_' + InterningTermBuilder.class.getSimpleName() + '_' + name, h);
        return h;
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

    private Term compoundInterned(InternedCompound x) {
        return terms[x.op].apply(x);
    }

    private Subterms subsInterned(ByteHijackMemoize<InternedCompound,Subterms> m, Term[] t) {
        return m.apply(InternedCompound.get(PROD, DTERNAL, t));
    }

    @Nullable
    private Term get(Term x) {
        Op xo = x.op();
        boolean negate = xo == NEG;
        if (negate) {
            x = x.unneg();
            xo = x.op();
        }
        if (internableRoot(xo, x.dt())) {
            Term y = terms[xo.id].apply(InternedCompound.get(x));
            if (y != null)
                return negate ? y.neg() : y;
        }
        return null;
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
        return compoundInterned(InternedCompound.get(op, dt, u));
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
                return SortedSubterms.the(t, u ->
                        subsInterned(subterms, u), false);
            } else {
                return subsInterned(subterms, t);
            }
        }


        return theSubterms(t);
    }


    public Subterms theSubterms(Term... t) {
        if (deep)
            resolve(t);

        return super.theSubterms(false, t);
    }


    public static DynBytes tmpKey() {
        return RecycledDynBytes.get();
    }

    private void resolve(Term[] t) {
        for (int i = 0, tLength = t.length; i < tLength; i++) {
            Term y = resolve(t[i]);
            if (y != null)
                t[i] = y;
        }
    }

    @Nullable private Term resolve(Term x) {
        Term y;
        if (!(x instanceof Atomic || x.volume() > volInternedMax || !internableSub(x)))
            y = get(x);
        else
            y = null;
        return y;
    }

    protected boolean internableRoot(Op op, int dt, Term[] u) {
        boolean i = internableRoot(op, dt) && internableSubs(u);
//        if (!i) {
//            System.out.println(op + " " + dt + " " + Arrays.toString(u));
//        }
        return i;
    }

    protected static boolean internableRoot(Op op, int dt) {
        return !op.atomic && op != NEG
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
                internableSub(subject) && internableSub(predicate) ) {

            boolean negate = false;

            //quick preparations to reduce # of unique entries

            if (!(((op==INH || op ==SIM) && (dt!=DTERNAL || subject.equals(predicate))))) {
                switch (op) {
                    case SIM:
                        //pre-sort by swapping to avoid saving redundant mappings
                        if (subject.compareTo(predicate) > 0) {
                            Term x = predicate;
                            predicate = subject;
                            subject = x;
                        }
                        break;
                    case IMPL:
                        negate = (predicate.op() == NEG);
                        if (negate)
                            predicate = predicate.unneg();
                        break;
                }

                if (deep) {
                    Term sr = resolve(subject);
                    if (sr != null) subject = sr;
                    Term pr = resolve(predicate);
                    if (pr != null) predicate = pr;
                }

                return this.terms[op.id].apply(InternedCompound.get(op, dt, subject, predicate)).negIf(negate);
            }

            //return statements.apply(InternedCompound.get(op, dt, subject, predicate));
        }

        return super.statement(op, dt, subject, predicate);
    }

    private Term _statement(InternedCompound c) {
        Term[] s = c.rawSubs.get();
        return super.statement(Op.ops[c.op], c.dt, s[0], s[1]);
    }

    @Override
    public Term conj(int dt, Term[] u) {

        //preFilter
//        u = conjPrefilter(dt, u);
//        boolean trues = false;
//        for (int i = 0, uLength = u.length; i < uLength; i++) {
//            Term x = u[i];
//            if (x == True) {
//                u[i] = null;
//                trues = true;
//            }
//            if (x == False)
//                return False;
//            if (x == Null)
//                return Null;
//        }
//        if (trues) {
//            u = ArrayUtils.removeNulls(u, Term[]::new);
//        }

        if (u.length > 1 && internableRoot(CONJ, dt, u)) {

            if (dt == 0 || dt == DTERNAL)
                u = Terms.sorted(u); //pre-sort
            else if (dt == XTERNAL) {
                Arrays.sort(u = u.clone()); //TODO deduplicate down to at least 2x, no further
            }

            if (deep)
                resolve(u);

            if (u.length > 1) {
                return terms[CONJ.id].apply(InternedCompound.get(CONJ, dt, u));
            }
        }

        return super.conj(dt, u);
    }


}
