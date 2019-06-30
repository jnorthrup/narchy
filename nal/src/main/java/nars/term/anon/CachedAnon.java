package nars.term.anon;

import nars.term.Compound;
import nars.term.Term;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

/** TODO implement these as CachedTransform-wrapped sub-implementations of the Anon.GET and PUT transforms, each with their own cache */
public class CachedAnon extends Anon {

    final UnifiedMap<Compound,Term> putCache =
            new UnifiedMap();
    final UnifiedMap<Compound,Term> getCache =
            new UnifiedMap();

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
    protected final Term applyPosCompound(Compound x) {
        if (!cache(x,putOrGet))
            return super.applyPosCompound(x);
        else
            return applyPosCompoundCached(x);
    }

    private Term applyPosCompoundCached(Compound x) {
        return putOrGet ?
            putCache.computeIfAbsent(x, this::putCache)
            :
            getCache.computeIfAbsent(x, this::getCache);
    }

    /** whether a target is cacheable */
    protected boolean cache(Compound x, boolean putOrGet) {
        return true;
    }

    private Term putCache(Compound x) {
        Term y = super.applyPosCompound(x);
        if (y instanceof Compound && cache((Compound) y, false))
            getCache.put((Compound) y, x);
        return y;
    }

    private Term getCache(Compound xx) {
        return super.applyPosCompound(xx);
    }
}
