package nars.util.term.builder;

import jcog.Util;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Term;
import nars.util.term.HijackTermCache;
import nars.util.term.InternedCompound;

import java.util.Arrays;
import java.util.function.Function;

import static nars.Op.*;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;

/**
 * can intern subterms and compounds.
 * the requirements for intern cache admission are configurable.
 **/
public class InterningTermBuilder extends HeapTermBuilder {

    final HijackTermCache[] terms;

    private final int cacheSizePerOp;

    private final HijackTermCache statements;
    private final HijackTermCache conj;
    private final HijackTermCache normalize;
    private final HijackTermCache concept;
    private final HijackTermCache root;


    public InterningTermBuilder() {
        this(32 * 1024);
    }

    public InterningTermBuilder(int sizePerOp) {
        this.cacheSizePerOp = sizePerOp;
        terms = new HijackTermCache[Op.ops.length];
        for (int i = 0; i < Op.ops.length; i++) {
            Op o = Op.ops[i];
            if (o.atomic || o ==NEG) continue;

            int s = cacheSizePerOp;
            if (o == PROD)
                s *= 2; //HACK since PROD also serves as subterm cache
            //TODO use multiple PROD slices to decrease contention

            terms[i] = newOpCache(this::compoundInterned, s);
        }

        concept = newOpCache(this::_concept, cacheSizePerOp);
        root = newOpCache(this::_root, cacheSizePerOp);

        normalize = newOpCache(this::_normalize, cacheSizePerOp);
        conj = newOpCache(this::_conj, cacheSizePerOp);
        statements = newOpCache(this::_statement, cacheSizePerOp*3);

        Runtime.getRuntime().addShutdownHook(new Thread(()->{
            System.out.println(InterningTermBuilder.this + "\n" + summary());
        }));
    }


    private HijackTermCache newOpCache(Function<InternedCompound, Term> f, int capacity) {
        return new HijackTermCache(f, capacity, 4);
    }

    private Term apply(InternedCompound x) {
        return terms[x.op].apply(x);
    }


    private Term compoundInterned(InternedCompound x) {
        return compoundInstance(Op.ops[x.op], x.dt, x.rawSubs.get(), x.key);
    }

    @Override
    public final Term compound(Op op, int dt, Term[] u) {
        boolean internable = internable(op, dt, u);
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

        if (inOp != PROD && internable(s)) {
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

        for (Term x : subterms) {
            if (x.the() == null)
                return false;
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
        return summary(terms, normalize);
        //return summary(terms, conj, statements, normalize, concept, root);
    }

    private static String summary(HijackTermCache[] termCache, HijackTermCache transforms) {
        return Arrays.toString(Util.map(0, termCache.length, x -> termCache[x]!=null ?
                (Op.ops[x] + ": " + termCache[x].summary() + "\n")
                : "", String[]::new)) + "\ntransforms=" + transforms.summary();
    }

    @Override
    public Term normalize(Compound x, byte varOffset) {

//        if (!x.hasVars())
//            throw new WTF();

        if (varOffset == 0) {
            Term xx = x.the();
            if (xx != null)
                return normalize.apply(InternedCompound.get(PROD, xx)); //new LighterCompound(PROD, x, NORMALIZE)));
        }

        return super.normalize(x, varOffset);

    }



    private Term _normalize(InternedCompound i) {
        return super.normalize((Compound) i.rawSubs.get()[0], (byte)0);
    }
    @Override
    public Term concept(Compound x) {
        Term xx = x.the();
        if (xx == null)
            return super.concept(x);
        return concept.apply(InternedCompound.get(PROD, xx));
    }
    private Term _concept(InternedCompound i) {
        return super.concept((Compound) i.rawSubs.get()[0]);
    }
    @Override
    public Term root(Compound x) {
        Term xx = x.the();
        if (xx == null)
            return super.root(x);
        return root.apply(InternedCompound.get(PROD, xx));
    }
    private Term _root(InternedCompound i) {
        return super.root((Compound) i.rawSubs.get()[0]);
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

        Term s = subject.the();
        if (s!=null) {
            Term p = predicate.the();
            if (p!=null)
                return statements.apply(InternedCompound.get(op, dt, subject, predicate));
        }
        return super.statement(op, dt, subject, predicate);
    }

    private Term _statement(InternedCompound c) {
        Term[] s = c.rawSubs.get();
        return super.statement(Op.ops[c.op], c.dt, s[0], s[1]);
    }

    @Override
    public Term conj(int dt, Term[] u) {
        //TODO presort if commutive?
        if (internable(CONJ, dt, u)) {
            switch(dt) {
                case 0:
                case DTERNAL:
                case XTERNAL:
                    //pre-sort
                    Arrays.sort(u);
                    break;
            }
            return conj.apply(InternedCompound.get(CONJ, dt, u));
        } else
            return super.conj(dt, u);
    }

    private Term _conj (InternedCompound c) {
        Term[] s = c.rawSubs.get();
        return super.conj(c.dt, s);
    }


}
