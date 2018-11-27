package nars.term.util.builder;

import jcog.data.byt.DynBytes;
import jcog.data.byt.RecycledDynBytes;
import jcog.memoize.Memoizers;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.util.HijackTermCache;
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
    protected static final int maxInternedVolumeDefault = 4;

    private final boolean deep;
    private final int volInternedMax;

    final HijackTermCache[] terms;

    private final String id;

    public InterningTermBuilder() {
        this(UUID.randomUUID().toString(), false, maxInternedVolumeDefault, DEFAULT_SIZE);
    }

    public InterningTermBuilder(String id, boolean deep, int volInternedMax, int cacheSizePerOp) {
        this.id = id;
        this.deep = deep;
        this.volInternedMax = volInternedMax;
        terms = new HijackTermCache[Op.ops.length];

        HijackTermCache statements = newOpCache("statement", this::_statement, cacheSizePerOp * 3);

        for (int i = 0; i < Op.ops.length; i++) {
            Op o = Op.ops[i];
            if (o.atomic || o == NEG) continue;

            int s = cacheSizePerOp;
            if (o == PROD)
                s *= 2; //HACK since PROD also serves as subterm cache

            //TODO use multiple PROD slices to decrease contention

            HijackTermCache c;
            if (o == CONJ) {
                c =
                        newOpCache("conj", j -> super.conj(false, j.dt, j.rawSubs.get()), cacheSizePerOp);
            } else if (o.statement) {
                c = statements;
            } else {
                c = newOpCache(o.str, this::compoundInterned, s);
            }
            terms[i] = c;
        }


    }


    protected HijackTermCache newOpCache(String name, Function<InternedCompound, Term> f, int capacity) {
        HijackTermCache h = new HijackTermCache(f, capacity, 4);
        Memoizers.the.add(id + '_' + InterningTermBuilder.class.getSimpleName() + '_' + name, h);
        return h;
    }

    private Term apply(InternedCompound x) {
        return terms[x.op].apply(x);
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


    private Term compoundInterned(InternedCompound x) {
        return theCompound(Op.ops[x.op], x.dt, x.rawSubs.get(), x.key); //x.arrayFinal());
    }

    @Override
    public final Term compound(Op op, int dt, Term[] u) {
        boolean internable = internableRoot(op, dt, u);
//        if (!internable) {
//            //internable(op, dt, u);
//            System.out.println("why: " + op + " " + dt + " " + Arrays.toString(u));
//        }

        return internable ?
                compoundInterned(op, dt, u) :
                super.compound(op, dt, u);
    }

    private Term compoundInterned(Op op, int dt, Term[] u) {
        return apply(InternedCompound.get(op, dt, u));
    }


    @Override
    public Subterms subterms(Op inOp, Term... s) {
        if (s.length == 0)
            return Op.EmptySubterms;

        if (inOp != PROD && internableSubs(s)) {
            return compoundInterned(PROD, DTERNAL, s).subterms();
        } else {
//            if (s.length == 2 && s[0].compareTo(s[1]) > 0) {
//                //TODO filter purely anon
//                return ((BiSubterm.ReversibleBiSubterm)newSubterms(inOp, s[1], s[0])).reverse();
//            }


            //resolve already-interned subterms
            /// s.clone() ?
//            for (int i = 0, sLength = s.length; i < sLength; i++) {
//                Term x = s[i];
//
//                HijackTermCache tt = terms[x.op().id];
//                if (tt != null) {
//                    Term z = tt.getIfPresent(InternedCompound.get(x));
//                    if (z != x && z != null)
//                        s[i] = z;
//                }
//            }

            return super.subterms(inOp, s);
        }

    }

    protected Term theCompound(Op o, int dt, Term[] t, @Nullable DynBytes key) {
        if (deep)
            resolve(t);
        return super.theCompound(o, dt, t, key);
    }

    public Subterms theSubterms(Term... t) {
        if (deep)
            resolve(t);
        return super.theSubterms(t);
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

        u = conjPrefilter(dt, u);

        if (u.length > 1 && internableRoot(CONJ, dt, u))
            return terms[CONJ.id].apply(InternedCompound.get(CONJ, dt, u));
        else
            return super.conj(false, dt, u);
    }


}
