package jcog.pri.bag.impl.hijack;

import jcog.pri.PriReference;
import jcog.pri.op.PriMerge;
import jcog.data.NumberX;


public class DefaultHijackBag<K> extends PriorityHijackBag<K, PriReference<K>> {

    protected final PriMerge merge;

    public DefaultHijackBag(PriMerge merge, int capacity, int reprobes) {
        super(capacity, reprobes);
        this.merge = merge;
    }

    @Override
    protected PriReference<K> merge(PriReference<K> existing, PriReference<K> incoming, NumberX overflowing) {
        float overflow = merge.merge(existing, incoming); 
        if (overflow > 0) {
            
            if (overflowing!=null) overflowing.add(overflow);
        }
        return existing;
    }



    @Override
    public K key(PriReference<K> value) {
        return value.get();
    }


}























