package jcog.pri.bag.impl.hijack;

import jcog.data.NumberX;
import jcog.pri.Prioritized;
import jcog.pri.UnitPrioritizable;
import jcog.pri.bag.impl.HijackBag;
import jcog.pri.op.PriMerge;
import jcog.pri.op.PriReturn;

/**
 * Created by me on 2/17/17.
 */
abstract public class PriHijackBag<K,V extends UnitPrioritizable> extends HijackBag<K, V> {


    protected PriHijackBag(int cap, int reprobes) {
        super(cap, reprobes);
        merge(PriMerge.plus);
    }

    @Override
    protected V merge(V existing, V incoming, NumberX overflowing) {
        float overflow = merge().merge(existing, ((Prioritized) incoming).pri(), PriReturn.Overflow);
        if (overflow > Float.MIN_NORMAL) {
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
