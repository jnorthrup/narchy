package jcog.pri.bag.impl;

import jcog.pri.PriReference;
import jcog.pri.op.PriMerge;

@Deprecated public class PLinkArrayBag<X> extends PriReferenceArrayBag<X,PriReference<X>> {

    public PLinkArrayBag(PriMerge mergeFunction, int cap) {
        super(mergeFunction, cap);
    }

}
