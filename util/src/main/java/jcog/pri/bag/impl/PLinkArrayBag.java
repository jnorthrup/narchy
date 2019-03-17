package jcog.pri.bag.impl;

import jcog.pri.PriReference;
import jcog.pri.op.PriMerge;

import java.util.Map;

public class PLinkArrayBag<X> extends PriReferenceArrayBag<X,PriReference<X>> {

    public PLinkArrayBag(PriMerge mergeFunction, int cap) {
        super(mergeFunction, cap);
    }

    public PLinkArrayBag(PriMerge mergeFunction, int cap, Map<X, PriReference<X>> map) {
        super(mergeFunction, cap, map);
    }

}
