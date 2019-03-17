package jcog.pri.bag.impl;

import jcog.pri.Prioritizable;
import jcog.pri.op.PriMerge;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class PriArrayBag<X extends Prioritizable> extends ArrayBag<X,X> {


    public PriArrayBag(PriMerge mergeFunction, int capacity) {
        this(mergeFunction, new UnifiedMap<>(capacity));
        setCapacity(capacity);
    }

    public PriArrayBag(PriMerge mergeFunction, Map<X, X> map) {
        super(mergeFunction, map);
    }

//    @Override
//    protected void removed(X x) {
//        //dont affect the result
//    }

    @Nullable
    @Override public X key(X k) {
        return k;
    }
}
