package nars.util.term.builder;

import jcog.Util;
import jcog.memoize.HijackMemoize;
import nars.Op;
import nars.The;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Term;
import nars.util.term.HijackTermCache;
import nars.util.term.InternedCompound;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

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

    /** attempts to recursively intern the elements of a subterm being interned */
    final boolean deepIntern = true;

    static final int cacheSizePerOp = 32 * 1024;

    {
        termCache = new HijackTermCache[Op.ops.length];
        for (int i = 0; i < Op.ops.length; i++) {
            if (Op.ops[i].atomic || Op.ops[i]==NEG) continue;

            termCache[i] = newOpCache(cacheSizePerOp);
        }
    }


    private HijackTermCache newOpCache(int capacity) {
        return new HijackTermCache(capacity, 4);
    }

    @Override
    public final Term newCompound(Op op, int dt, Term[] u) {
        return internable(op, dt, u) ?
                termCache[op.id].apply(new InternedCompound(op, dt, u)) :
                super.newCompound(op, dt, u);
    }

    @Override
    public Subterms newSubterms(Op inOp, Term... s) {
        if (inOp != PROD && internable(s)) {

            if (deepIntern) {
                for (int i = 0, subtermsLength = s.length; i < subtermsLength; i++) {
                    Term x = s[i];
                    Term ux = x.unneg();
                    Term y = resolveInternable(ux); //already determined to be internable
                    if (y != null && y!=ux)
                        s[i] = y.negIf(x != ux);
                }
            }

            return compound(PROD, s).subterms();
        } else
            return super.newSubterms(inOp, s);

    }


    protected boolean internable(Term x) {
        return (x instanceof The) &&
                internable(x.op(), x.dt()) &&
                x.AND(this::internable);
    }

    protected boolean internable(Op op, int dt, Term[] u) {
        return internable(op, dt) && internable(u);
    }

    private boolean internable(Op op, int dt) {
        return !op.atomic && (op!=NEG) && (!op.temporal || internable(dt));
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

    protected boolean internable(Term[] subterms) {
        if (subterms.length == 0)
            return false;

        for (Term x : subterms) {

            if (!internable(x))
                return false;

        }


        return true;
    }

    @Override protected Term resolve(Term x){
        return !internable(x) ? x : resolveInternable(x);
    }

    protected Term resolveInternable(Term x){
        HijackTermCache tc = termCache[x.op().id];
        if (tc == null)
            return x;
        Term y = tc.apply(new InternedCompound((Compound)x));
        if (y!=null)
            return y;
        return x;
    }

    public String summary() {
        return
                summary(termCache);
    }

    public static String summary(HijackTermCache[] termCache) {
        return Arrays.toString(Util.map(HijackMemoize::summary, new String[termCache.length], termCache));
    }
}
