package jcog.bag.impl.hijack;

import jcog.pri.PriReference;
import jcog.pri.op.PriMerge;
import org.apache.commons.lang3.mutable.MutableFloat;


public class DefaultHijackBag<K> extends PriorityHijackBag<K, PriReference<K>> {

    protected final PriMerge merge;

    public DefaultHijackBag(PriMerge merge, int capacity, int reprobes) {
        super(capacity, reprobes);
        this.merge = merge;
    }

    @Override
    protected PriReference<K> merge( PriReference<K> existing,  PriReference<K> incoming, MutableFloat overflowing) {
        float overflow = merge.merge(existing, incoming); //modify existing
        if (overflow > 0) {
            //pressurize(-overflow);
            if (overflowing!=null) overflowing.add(overflow);
        }
        return existing;
    }



    @Override
    public K key(PriReference<K> value) {
        return value.get();
    }


}

//    public static void flatForget(BudgetHijackBag<?,? extends Budgeted> b) {
//        double p = b.pressure.get() /* MULTIPLIER TO ANTICIPATE NEXT period */;
//        int s = b.size();
//
//
//            //float ideal = s * b.temperature();
//            if (p > Param.BUDGET_EPSILON * s) {
//                if (b.pressure.compareAndSet(p, 0)) {
//
//                    b.commit(null); //precommit to get accurate mass
//                    float mass = b.mass;
//
//                    float over = //(float) ((p + mass) - ideal);
//                            ((float) p / ((float)p + mass) / s);
//                    if (over >= Param.BUDGET_EPSILON) {
//                        b.commit(x -> x.budget().priSub(over * (1f - x.qua())));
//                    }
//                }
//
//            }
//
//    }
