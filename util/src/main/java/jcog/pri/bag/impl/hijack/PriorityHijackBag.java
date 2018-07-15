package jcog.pri.bag.impl.hijack;

import jcog.data.NumberX;
import jcog.pri.Priority;
import jcog.pri.bag.impl.HijackBag;

/**
 * Created by me on 2/17/17.
 */
abstract public class PriorityHijackBag<K,V extends Priority> extends HijackBag<K, V> {


    protected PriorityHijackBag(int cap, int reprobes) {
        super(cap, reprobes);
    }


    @Override
    protected V merge(V existing, V incoming, NumberX overflowing) {
        float overflow = existing.priAddOverflow(incoming.priElseZero());
        if (overflow > 0) {
            if (overflowing!=null) overflowing.add(overflow);
        }
        return existing; 
    }

    @Override
    public float pri(V key) {
        return key.pri();
    }

    @Override
    public void priAdd(V entry, float amount) {
        entry.priAdd(amount);
    }

}
