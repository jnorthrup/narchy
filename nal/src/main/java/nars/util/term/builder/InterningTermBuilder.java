package nars.util.term.builder;

import jcog.Util;
import jcog.WTF;
import nars.Op;
import nars.The;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.atom.Int;
import nars.term.compound.LighterCompound;
import nars.util.term.HijackTermCache;
import nars.util.term.InternedCompound;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.function.Function;

import static nars.Op.NEG;
import static nars.Op.PROD;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;

/**
 * can intern subterms and compounds.
 * the requirements for intern cache admission are configurable.
 **/
public class InterningTermBuilder extends HeapTermBuilder {


    final HijackTermCache[] termCache;
    final HijackTermCache transformCache;

    private final int cacheSizePerOp;


    public InterningTermBuilder() {
        this(32 * 1024);
    }

    public InterningTermBuilder(int sizePerOp) {
        this.cacheSizePerOp = sizePerOp;
        termCache = new HijackTermCache[Op.ops.length];
        for (int i = 0; i < Op.ops.length; i++) {
            if (Op.ops[i].atomic || Op.ops[i]==NEG) continue;

            termCache[i] = newOpCache(this::compoundInterned, cacheSizePerOp);
        }

        transformCache = newOpCache(this::normalize, cacheSizePerOp);
    }


    private HijackTermCache newOpCache(Function<InternedCompound, Term> f, int capacity) {
        return new HijackTermCache(f, capacity, 4);
    }

    private Term apply(InternedCompound x) {
        return termCache[x.op].apply(x);
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

        if (inOp != PROD && internable(s)) {

            return compound(PROD, s).subterms();
        } else {
//            if (s.length == 2 && s[0].compareTo(s[1]) > 0) {
//                //TODO filter purely anon
//                return ((BiSubterm.ReversibleBiSubterm)newSubterms(inOp, s[1], s[0])).reverse();
//            }

            return super.subterms(inOp, s);
        }

    }


//    private boolean internable(Term x) {
//        return (x instanceof The) && (
//            (x instanceof Atomic) || (internable(x.op(), x.dt(),true) && internable(x.subterms()))
//        );
//    }

//    private boolean internable(Subterms x) {
//        return x instanceof The.FullyInternable || x.AND(this::internableSubterm);
//    }

    private boolean internableSubterm(Term x) {
        return (x instanceof The) &&
                (x instanceof Atomic)
                ||
                (
                        internable(x.op(), x.dt(), false) &&
                                x.AND(this::internableSubterm)
                );
    }

    /** root */
    private boolean internable(Op op, int dt, Term[] u) {
        return internable(op, dt, true) && internable(u);
    }

    private boolean internable(Op op, int dt, boolean root) {
        return !op.atomic && (!root || op!=NEG) && (!op.temporal || internable(dt));
    }

    @Nullable
    private boolean internable(int dt) {
        switch (dt) {
            case 0:
            case DTERNAL:
            case XTERNAL:
                return true;
            default:
                return false; //some other DT value
        }

    }

    private boolean internable(Term[] subterms) {
        if (subterms.length == 0)
            return false;

        for (Term x : subterms) {

            if (!internableSubterm(x))
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
        return summary(termCache);
    }

    public static String summary(HijackTermCache[] termCache) {
        return Arrays.toString(Util.map(0, termCache.length, x -> termCache[x]!=null ?
                (Op.ops[x] + ": " + termCache[x].summary() + "\n")
                : "", String[]::new));
    }

    @Override
    public Term normalize(Compound x, byte varOffset) {

        if (!x.hasVars())
            throw new WTF();

        if (varOffset == 0) {
            return transformCache.apply(InternedCompound.get(new LighterCompound(PROD, x, NORMALIZE)));
        } else {
            return super.normalize(x, varOffset);
        }
    }

    final static Atomic NORMALIZE = Int.the(1);
    protected Term normalize(InternedCompound i) {
        Term[] termAndTransform = i.rawSubs.get();
        Term term = termAndTransform[0];
        int transform = ((Int)termAndTransform[1]).id;
        switch (transform) {
            case 1: /* normalize */
                return super.normalize((Compound)term, (byte)0);
            default:
                throw new  UnsupportedOperationException();
        }
    }
}
