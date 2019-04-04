package jcog.pri.bag.impl;

import jcog.pri.PriBuffer;
import jcog.pri.Prioritizable;
import jcog.pri.op.PriMerge;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class PriArrayBag<X extends Prioritizable> extends ArrayBag<X,X> {

    public PriArrayBag(PriMerge merge) {
        this(merge, 0);
    }

    public PriArrayBag(PriMerge merge, int capacity) {
        this(merge, PriBuffer.newMap(false));
        setCapacity(capacity);
    }

    public PriArrayBag(PriMerge merge, Map<X, X> map) {
        super(merge, map);
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
