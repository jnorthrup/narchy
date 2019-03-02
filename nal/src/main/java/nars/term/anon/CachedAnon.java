package nars.term.anon;

import nars.term.Compound;
import nars.term.Term;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.util.Map;

/** TODO implement these as CachedTransform-wrapped sub-implementations of the Anon.GET and PUT transforms, each with their own cache */
public class CachedAnon extends Anon {

    final Map<Compound,Term> putCache = new UnifiedMap();
    final Map<Compound,Term> getCache = new UnifiedMap();

    public CachedAnon() {
        super();
    }

    public CachedAnon(int cap) {
        super(cap);
    }

    @Override
    public void clear() {
        super.clear();
        invalidate();
    }

    @Override
    public boolean rollback(int toUniques) {
        if (super.rollback(toUniques)) {
            invalidate();
            return true;
        }
        return false;
    }

    protected void invalidate() {
        putCache.clear();
        getCache.clear();
    }

    @Override
    protected Term applyPosCompound(Compound x) {
        if (!cache(x,putOrGet))
            return super.applyPosCompound(x);

        return putOrGet ? putCache.computeIfAbsent(x, xx -> {
            Term y = super.applyPosCompound(xx);
            if (y instanceof Compound && cache((Compound) y, false))
                getCache.put((Compound) y, xx);
            return y;
        })
            :
            getCache.computeIfAbsent(x, xx -> super.applyPosCompound(xx));
    }

    /** whether a target is cacheable */
    protected boolean cache(Compound x, boolean putOrGet) {
        return true;
    }
}
