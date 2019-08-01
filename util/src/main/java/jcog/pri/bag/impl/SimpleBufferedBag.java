package jcog.pri.bag.impl;

import jcog.pri.PriMap;
import jcog.pri.Prioritizable;
import jcog.pri.bag.Bag;

public class SimpleBufferedBag<X, Y extends Prioritizable> extends BufferedBag<X, Y, Y> {

    public SimpleBufferedBag(Bag<X, Y> activates) {
        this(activates, new PriMap(activates.merge()));
    }

    public SimpleBufferedBag(Bag<X, Y> activates, PriMap<Y> conceptPriMap) {
        super(activates, conceptPriMap);
    }
    @Override
    protected final Y valueInternal(Y c) {
        return c;
    }
//    @Override
//    protected final Y valueInternal(Y c, float pri) {
//        return c;
//    }

}
