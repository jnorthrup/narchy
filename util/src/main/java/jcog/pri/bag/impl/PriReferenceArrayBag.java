package jcog.pri.bag.impl;

import jcog.data.list.FasterList;
import jcog.pri.PriReference;
import jcog.pri.op.PriMerge;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class PriReferenceArrayBag<X,Y extends PriReference<X>> extends ArrayBag<X, Y> {

    public PriReferenceArrayBag(PriMerge mergeFunction, int capacity) {
        this(mergeFunction, capacity,//new HashMap<>(capacity, 0.5f)
                new UnifiedMap<>(capacity, 0.9f));
    }

    public PriReferenceArrayBag(PriMerge mergeFunction, int cap, Map<X, Y> map) {
        super(mergeFunction, cap, map);
    }

    @Override
    public final X key(Y l) {
        return l.get();
    }

    @Deprecated public List<PriReference<X>> listCopy() {
        List l = new FasterList(size());
        forEach((Consumer) l::add);
        return l;
    }
}
