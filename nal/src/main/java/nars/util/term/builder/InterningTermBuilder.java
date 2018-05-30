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

    



    private HijackTermCache newOpCache(int capacity) {
        return new HijackTermCache(capacity , 3);
    }

    final HijackTermCache[] termCache;
    final HijackTermCache[] termTemporalCache;
    
    {
        termCache = new HijackTermCache[Op.ops.length];
        termTemporalCache = new HijackTermCache[Op.ops.length];
        for (int i = 0; i < Op.ops.length; i++) {
            if (Op.ops[i].atomic) continue;
            termCache[i] = newOpCache(16 * 1024);
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

            
        } else
            return super.newSubterms(inOp, s);

    }

    protected boolean internable(Op op, int dt, Term[] u) {
        return 
                internable(u);
    }

    protected boolean internable(Term[] subterms) {
        if (subterms.length == 0)
            return false;

        for (Term x : subterms) {







            if (x.hasAny(Op.Temporal))
                return false;

            if (!x.ANDrecurse(xx -> xx instanceof The)) {
                return false;
            }










        }
        return true;
    }


    public String summary() {
        return  
                "compound cache: " + summary(termCache, termCache) + "\n" +
                "termporal cache: " + summary(termCache, termTemporalCache) + "\n"
                ;
    }

    @NotNull
    public static String summary(HijackTermCache[] termCache, HijackTermCache[] termTemporalCache2) {
        return Arrays.toString(Util.map(HijackMemoize::summary, new String[termCache.length], termTemporalCache2));
    }
}
