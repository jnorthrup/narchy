package nars.util.term.builder;

import jcog.Util;
import jcog.WTF;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.atom.Int;
import nars.util.term.HijackTermCache;
import nars.util.term.InternedCompound;

import java.util.Arrays;
import java.util.function.Function;

import static nars.Op.*;
import static nars.time.Tense.DTERNAL;

/**
 * can intern subterms and compounds.
 * the requirements for intern cache admission are configurable.
 **/
public class InterningTermBuilder extends HeapTermBuilder {

    final HijackTermCache[] terms;
    final HijackTermCache normalizes;

    private final int cacheSizePerOp;

    private final HijackTermCache statements;
    private final HijackTermCache conj;


    public InterningTermBuilder() {
        this(64 * 1024);
    }

    public InterningTermBuilder(int sizePerOp) {
        this.cacheSizePerOp = sizePerOp;
        terms = new HijackTermCache[Op.ops.length];
        for (int i = 0; i < Op.ops.length; i++) {
            if (Op.ops[i].atomic || Op.ops[i]==NEG) continue;

            terms[i] = newOpCache(this::compoundInterned, cacheSizePerOp);
        }

        normalizes = newOpCache(this::normalize, cacheSizePerOp);
        conj = newOpCache(this::_conj, cacheSizePerOp);
        statements = newOpCache(this::_statement, cacheSizePerOp*3);
    }


    private HijackTermCache newOpCache(Function<InternedCompound, Term> f, int capacity) {
        return new HijackTermCache(f, capacity, 4);
    }

    private Term apply(InternedCompound x) {
        return terms[x.op].apply(x);
    }


    protected Term compoundInterned(InternedCompound x) {
        return compoundInstance(Op.ops[x.op], x.dt, x.rawSubs.get());
    }

    @Override
    public final Term compound(Op op, int dt, Term[] u) {
        boolean internable = internable(op, dt, u);
//        if (!internable) {
//            //internable(op, dt, u);
//            System.out.println("why: " + op + " " + dt + " " + Arrays.toString(u));
//        }

        return internable ?
                apply(InternedCompound.get(op, dt, u)) :
                super.compound(op, dt, u);
    }



    @Override
    public Subterms subterms(Op inOp, Term... s) {
        if (s.length == 0)
            return Op.EmptySubterms;

        if (inOp != PROD && internable(s)) {
            return compound(PROD, s).subterms();
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



//    private Term internableSubterm(Term x) {
//        return (x instanceof The) &&
//                ((x instanceof Atomic)
//                ||
//                internable(x.op(), x.dt(), false)) ? x.the() : null;
//    }

    /** root */
    private boolean internable(Op op, int dt, Term[] u) {
        return internable(op, dt, true) && internable(u);
    }


    private boolean internable(Op op, int dt, boolean root) {
        return !op.atomic && (!root || op!=NEG);
                //&& (!op.temporal || !Tense.dtSpecial(dt)); //allow temporals
    }



    private boolean internable(Term[] subterms) {

        for (int i = 0, subtermsLength = subterms.length; i < subtermsLength; i++) {
            Term x = subterms[i];
            Term y = x.the();
            if (y == null) {
                return false;
            }
        }

        return true;
    }

//    @Override protected Term resolve(Term x){
//        return !internable(x) ? x : resolveInternable(x);
//    }
//
//    private Term resolveInternable(Term x){
//        HijackTermCache tc = termCache[x.op().id];
//        if (tc == null)
//            return x;
//        Term y = tc.apply(InternedCompound.get((Compound)x));
//        if (y!=null)
//            return y;
//        return x;
//    }

    public String summary() {
        return summary(terms, normalizes);
    }

    static String summary(HijackTermCache[] termCache, HijackTermCache transforms) {
        return Arrays.toString(Util.map(0, termCache.length, x -> termCache[x]!=null ?
                (Op.ops[x] + ": " + termCache[x].summary() + "\n")
                : "", String[]::new)) + "\ntransforms=" + transforms.summary();
    }

    @Override
    public Term normalize(Compound x, byte varOffset) {

        if (!x.hasVars())
            throw new WTF();

        if (varOffset == 0) {
            return normalizes.apply(InternedCompound.get(PROD, DTERNAL,x )); //new LighterCompound(PROD, x, NORMALIZE)));
        } else {
            return super.normalize(x, varOffset);
        }
    }

    protected Term normalize(InternedCompound i) {
        Term[] termAndTransform = i.rawSubs.get();
        Term term = termAndTransform[0];
//        int transform = ((Int)termAndTransform[1]).id;
//        switch (transform) {
//            case 1: /* normalize */
                return super.normalize((Compound)term, (byte)0);
//            default:
//                throw new  UnsupportedOperationException();
//        }
    }

    @Override
    public Term statement(Op op, int dt, Term subject, Term predicate) {
        if (op==SIM) {
            //pre-sort by swapping to avoid saving redundant mappings
            if (subject.compareTo(predicate)>0) {
                Term x = predicate;
                predicate = subject;
                subject = x;
            }
        }

        //terms[op.id]
        return statements.apply(InternedCompound.get(op, dt, subject, predicate));
    }

    private Term _statement(InternedCompound c) {
        Term[] s = c.rawSubs.get();
        return super.statement(Op.ops[c.op], c.dt, s[0], s[1]);
    }

    private Term _conj (InternedCompound c) {
        Term[] s = c.rawSubs.get();
        return super.conj(c.dt, s);
    }

    @Override
    public Term conj(int dt, Term[] u) {
        //TODO presort if commutive?
        return conj.apply(InternedCompound.get(CONJ, dt, u));
    }
}
