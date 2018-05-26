package nars.util.term.builder;

import jcog.Util;
import jcog.memoize.HijackMemoize;
import nars.Op;
import nars.The;
import nars.subterm.Subterms;
import nars.term.Term;
import nars.util.term.HijackTermCache;
import nars.util.term.InternedCompound;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

import static nars.Op.PROD;
import static nars.time.Tense.DTERNAL;

/** can intern subterms, compounds, and temporal compounds.
 * the requirements for intern cache admission are configurable.
 **/
public class InterningTermBuilder extends HeapTermBuilder {

    //TODO Atom Cache



    private HijackTermCache newOpCache(int capacity) {
        return new HijackTermCache(capacity , 3);
    }

    final HijackTermCache[] termCache;
    final HijackTermCache[] termTemporalCache;
    //public final SubtermsCache subtermCache = new SubtermsCache(128 * 1024, 3);
    {
        termCache = new HijackTermCache[Op.ops.length];
        termTemporalCache = new HijackTermCache[Op.ops.length];
        for (int i = 0; i < Op.ops.length; i++) {
            if (Op.ops[i].atomic) continue;
            termCache[i] = newOpCache(8 * 1024);
            termTemporalCache[i] = newOpCache(8 * 1024);
        }
    }




    @Override public final Term newCompound(Op op, int dt, Term[] u) {
        return internable(op, dt, u) ?
                (dt == DTERNAL ? termCache : termTemporalCache)[op.id].apply(new InternedCompound(op, dt, u)) :
                super.newCompound(op, dt, u);
    }

    @Override public Subterms newSubterms(Op inOp, Term... s) {
        if (inOp!=PROD && internable(s)) {
            return compound(PROD, s).subterms();

            //return subtermCache.apply(new InternedSubterms(s));
        } else
            return super.newSubterms(inOp, s);

    }

    protected boolean internable(Op op, int dt, Term[] u) {
        return //op!=NEG &&
                internable(u);
    }

    protected boolean internable(Term[] subterms) {
        if (subterms.length == 0)
            return false;

        for (Term x : subterms) {
            if (!(x instanceof The)) {
                //HACK caching these interferes with unification.  instead fix unification then allow caching of these
                return false;
            }
            if (x.hasAny(Op.Temporal))
                return false;
//            switch (x.dt()) {
//                case DTERNAL:
//                case 0:
//                case XTERNAL:
//                    break; //OK
//                default:
//                    return false; //specific dt: exclude temporal terms polluting the cache
//            }
        }
        return true;
    }


    public String summary() {
        return  //"subterm cache: " + subtermCache.summary() + "\n" +
                "compound cache: " + summary(termCache, termCache) + "\n" +
                "termporal cache: " + summary(termCache, termTemporalCache) + "\n"
                ;
    }

    @NotNull
    public static String summary(HijackTermCache[] termCache, HijackTermCache[] termTemporalCache2) {
        return Arrays.toString(Util.map(HijackMemoize::summary, new String[termCache.length], termTemporalCache2));
    }
}
