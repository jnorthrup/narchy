package jcog.pri.bag.impl;

import jcog.data.list.FasterList;
import jcog.pri.PriMap;
import jcog.pri.PriReference;
import jcog.pri.op.PriMerge;

import java.util.List;
import java.util.Map;

public class PriReferenceArrayBag<X,Y extends PriReference<X>> extends ArrayBag<X, Y> {

    public PriReferenceArrayBag(PriMerge mergeFunction, int capacity) {
        this(mergeFunction, capacity,
                //new HashMap<>(capacity, 0.5f)
                //new UnifiedMap<>(capacity, 0.9f)
                PriMap.newMap(false)
        );
    }

    public PriReferenceArrayBag(PriMerge mergeFunction, int cap, Map<X, Y> map) {
        super(mergeFunction, cap, map);
    }

    @Override
    public final X key(Y l) {
        return l.get();
    }

    @Deprecated public List<Y> listCopy() {
        var l = new FasterList<Y>(size());
        for (var y : this) {
            l.addFast(y);
        }
        return l;
    }

}
