package nars.term.anon;

import nars.term.Term;
import nars.term.transform.CachedTermTransform;
import nars.term.transform.TermTransform;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.util.Map;

/** TODO imiplement these as CachedTransform-wrapped sub-implementations of the Anon.GET and PUT transforms, each with their own cache */
public class CachedAnon extends Anon {

    protected Map<Term,Term> cache;

    public CachedAnon(int capacity) {
        super(capacity);
    }



    @Override
    public void clear() {
        cache.clear();
        super.clear();
    }

    protected TermTransform newPut() {
        //HACK
        if (cache == null)
            //new HashMap(capacity); //<-- cant use; CME's
            cache = new UnifiedMap<>();

        return new CachedTermTransform(super.newPut(), cache);
    }

    protected TermTransform newGet() {
        return new CachedTermTransform(super.newGet(), cache);
    }

}
