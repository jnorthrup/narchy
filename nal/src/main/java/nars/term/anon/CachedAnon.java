package nars.term.anon;

import nars.term.Term;
import nars.term.Termed;
import nars.term.transform.CachedTermTransform;
import nars.term.transform.DirectTermTransform;
import nars.term.transform.TermTransform;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/** TODO imiplement these as CachedTransform-wrapped sub-implementations of the Anon.GET and PUT transforms, each with their own cache */
public class CachedAnon extends Anon {

    private Map<Term,Term> externCache;
    private DirectTermTransform.CachedDirectTermTransform internCache;

    public CachedAnon(int capacity, int internCacheSize) {
        super(capacity);
        this.internCache.resize(internCacheSize);
    }

    @Override
    public boolean rollback(int uniques) {
        if (super.rollback(uniques)) {
            externCache.clear(); //must clear externCache if uniques are removed;
            //TODO technically we can keep externCache entries that dont use the removed uniques

            //internCache can remain
            return true;
        }
        return false;
    }

    @Override
    public void clear() {
        //the internCache is universal so it doesnt need cleared
        externCache.clear();
        super.clear();
    }


    protected TermTransform newPut() {
        return internCache = new DirectTermTransform.CachedDirectTermTransform(0) {
            @Override
            public final @Nullable Termed transformAtomic(Term atomic) {
                return put(atomic);
            }
        };
    }

    protected TermTransform newGet() {
        //HACK
        if (externCache == null)
            //new HashMap(capacity); //<-- cant use; CME's
            externCache = new UnifiedMap<>();

        return new CachedTermTransform(new TermTransform() {
            @Override
            public final @Nullable Termed transformAtomic(Term atomic) {
                return get(atomic);
            }
        }, externCache);
    }

}
