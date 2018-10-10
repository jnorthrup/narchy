package jcog.pri.bag.impl;

import jcog.pri.Prioritizable;
import jcog.pri.op.PriMerge;

import java.util.Map;

/**
 * ArrayBag with a randomized sampling range
 */
@Deprecated public class CurveBag<X extends Prioritizable> extends PriArrayBag<X> {


    public CurveBag(PriMerge mergeFunction, Map<X, X> map, int cap) {
        this(mergeFunction, map);
        setCapacity(cap);
    }


    public CurveBag(PriMerge mergeFunction, Map<X, X> map) {
        super(mergeFunction, map);
    }


}