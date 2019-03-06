package jcog.pri.bag.impl.hijack;

import jcog.data.NumberX;
import jcog.pri.UnitPrioritizable;
import jcog.pri.bag.impl.HijackBag;
import jcog.pri.op.PriMerge;

/**
 * Created by me on 2/17/17.
 */
abstract public class PriHijackBag<K,V extends UnitPrioritizable> extends HijackBag<K, V> {


    protected PriHijackBag(int cap, int reprobes) {
        super(cap, reprobes);
    }


    protected PriMerge merge() {
        return PriMerge.plus;
    }

    @Override
    protected V merge(V existing, V incoming, NumberX overflowing) {
        float overflow = merge().merge(existing, incoming);
        //if (overflow > 0) {
            if (overflowing!=null) overflowing.add(overflow);
        //}
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
