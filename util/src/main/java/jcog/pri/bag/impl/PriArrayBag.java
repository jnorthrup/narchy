package jcog.pri.bag.impl;

import jcog.pri.Priority;
import jcog.pri.op.PriMerge;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class PriArrayBag<X extends Priority> extends ArrayBag<X,X> {


    public PriArrayBag(PriMerge mergeFunction, int capacity) {
        this(mergeFunction, new HashMap(capacity));
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
