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
import nars.term.atom.Atomic;
import nars.term.util.InternedCompound;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Function;

import static nars.Op.*;
import static nars.time.Tense.DTERNAL;

/**
 * can intern subterms and compounds.
 * the requirements for intern cache admission are configurable.
 **/
public class InterningTermBuilder extends HeapTermBuilder {


    protected static final int DEFAULT_SIZE = Memoizers.DEFAULT_MEMOIZE_CAPACITY;
    protected static final int maxInternedVolumeDefault = 16;
    protected static boolean deepDefault = false;

    /** memory-saving */
    private static final boolean sortCanonically = true;

    private final boolean deep;
    private final int volInternedMax;

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
        terms = new ByteHijackMemoize[Op.ops.length];


        subterms = newOpCache("subterms", x -> theSubterms(x.rawSubs.get()), cacheSizePerOp * 2);
        anonSubterms = newOpCache("anonSubterms", x -> new AnonVector(x.rawSubs.get()), cacheSizePerOp);
        ByteHijackMemoize statements = newOpCache("statement", this::_statement, cacheSizePerOp * 3);

        for (int i = 0; i < Op.ops.length; i++) {
            Op o = Op.ops[i];
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
                c = newOpCache(o.str, x -> theCompound(Op.ops[x.op], x.dt, x.rawSubs.get(), x.key), s);
            }
            terms[i] = c;
        }


    }


    protected ByteHijackMemoize newOpCache(String name, Function<InternedCompound, ?> f, int capacity) {
        ByteHijackMemoize h = new ByteHijackMemoize(f, capacity, 4, false);
        Memoizers.the.add(id + '_' + InterningTermBuilder.class.getSimpleName() + '_' + name, h);
        return h;
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
                return y.negIf(negate);
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


        //TODO separate cache for anon's
        if (isAnon(t))
            return subsInterned(anonSubterms, t);

        if (internableSubs(t)) {
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
            Term x = t[i];
            if (x instanceof Atomic)
                continue;
            if (!internableSub(x))
                continue;

            Term y = get(x);

            if (y != null)
                t[i] = y;
        }
    }

    private boolean internableRoot(Op op, int dt, Term[] u) {
        boolean i = internableRoot(op, dt) && internableSubs(u);
//        if (!i) {
//            System.out.println(op + " " + dt + " " + Arrays.toString(u));
//        }
        return i;
    }

    private static boolean internableRoot(Op op, int dt) {
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


        if (subject.the() && predicate.the()) {

            boolean negate = false;

            //quick preparations to reduce # of unique entries
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

            return this.terms[op.id].apply(InternedCompound.get(op, dt, subject, predicate)).negIf(negate);

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

//        u = conjPrefilter(dt, u);

        if (u.length > 1 && internableRoot(CONJ, dt, u))
            return terms[CONJ.id].apply(InternedCompound.get(CONJ, dt, u));
        else
            return super.conj(dt, u);
    }


}
