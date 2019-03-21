package jcog.pri.bag.impl;

import jcog.data.list.FasterList;
import jcog.pri.PriReference;
import jcog.pri.op.PriMerge;

import java.util.List;
import java.util.function.Consumer;

public class PLinkArrayBag<X> extends PriReferenceArrayBag<X,PriReference<X>> {

    public PLinkArrayBag(PriMerge mergeFunction, int cap) {
        super(mergeFunction, cap);
    }

    @Deprecated public List<PriReference<X>> listCopy() {
        List l = new FasterList(size());
        forEach((Consumer) l::add);
        return l;
    }
}
