package jcog.pri.bag.impl;

import jcog.pri.Priority;
import jcog.pri.op.PriMerge;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class PriArrayBag<X extends Priority> extends ArrayBag<X,X> {


    public PriArrayBag(PriMerge mergeFunction, Map<X, X> map) {
        super(mergeFunction, map);
    }


    @Nullable
    @Override public X key(X k) {
        return k;
    }
}
