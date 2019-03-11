package jcog.pri.bag.impl;

import jcog.pri.PriReference;
import jcog.pri.op.PriMerge;

import java.util.Map;

public class PriReferenceArrayBag<X,Y extends PriReference<X>> extends ArrayBag<X, Y> {

    public PriReferenceArrayBag(PriMerge mergeFunction, int capacity) {
        super(mergeFunction, capacity);
    }

    public PriReferenceArrayBag(int cap, PriMerge mergeFunction, Map<X, Y> map) {
        super(mergeFunction, cap, map);
    }

    @Override
    public final X key(Y l) {
        return l.get();
    }
}
