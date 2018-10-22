package jcog.pri.bag.impl;

import jcog.pri.PriReference;
import jcog.pri.op.PriMerge;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class PLinkArrayBag<X> extends ArrayBag<X, PriReference<X>> {

    public PLinkArrayBag(PriMerge mergeFunction, int cap) {
        super(mergeFunction, cap);
    }

    public PLinkArrayBag(int cap, PriMerge mergeFunction, @NotNull Map<X, PriReference<X>> map) {
        super(cap, mergeFunction, map);
    }

    @Override
    public final X key( PriReference<X> l) {
        return l.get();
    }


}
