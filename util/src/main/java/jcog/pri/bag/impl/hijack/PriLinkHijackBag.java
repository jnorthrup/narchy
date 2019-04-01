package jcog.pri.bag.impl.hijack;

import jcog.data.NumberX;
import jcog.pri.PriReference;
import jcog.pri.Prioritized;
import jcog.pri.ScalarValue;
import jcog.pri.bag.impl.HijackBag;
import jcog.pri.op.PriMerge;


public class PriLinkHijackBag<X,Y extends PriReference<? extends X>> extends HijackBag<X, Y> {

    private final PriMerge merge;

    public PriLinkHijackBag(int initialCapacity, int reprobes) {
        this(PriMerge.plus, initialCapacity, reprobes);
    }

    private PriLinkHijackBag(PriMerge merge, int initialCapacity, int reprobes) {
        super(initialCapacity, reprobes);
        this.merge = merge;
    }

    @Override
    public final float pri(Y key) {
        return key.pri();
    }

    @Override
    public void priAdd(Y entry, float amount) {
        entry.priAdd(amount);
    }

    @Override
    public X key(Y value) {
        return value.get();
    }

    @Override
    protected Y merge(Y existing, Y incoming, NumberX overflowing) {
        float overflow = merge.merge(existing, ((Prioritized) incoming).pri(), PriMerge.MergeResult.Overflow);
        if (overflow > ScalarValue.EPSILON) {
            if (overflowing != null) overflowing.add(overflow);
        }
        return existing;
    }


}
