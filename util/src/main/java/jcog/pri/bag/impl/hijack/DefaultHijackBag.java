package jcog.pri.bag.impl.hijack;

import jcog.pri.PLink;
import jcog.pri.op.PriMerge;


public class DefaultHijackBag<K> extends PriHijackBag<K, PLink<K>> {

    public DefaultHijackBag(PriMerge merge, int capacity, int reprobes) {
        super(capacity, reprobes);
        merge(merge);
    }


    @Override
    public K key(PLink<K> value) {
        return value.get();
    }

}























