package jcog.pri.bag.impl.hijack;

import jcog.data.NumberX;
import jcog.pri.PLinkHashCached;
import jcog.pri.bag.impl.HijackBag;

import java.util.function.Consumer;

/**
 * Created by me on 2/17/17.
 */
public class PLinkHijackBag<X> extends HijackBag<X, PLinkHashCached<X>> {

    public PLinkHijackBag(int initialCapacity, int reprobes) {
        super(initialCapacity, reprobes);
    }

    @Override
    public final float pri( PLinkHashCached<X> key) {
        return key.pri();
    }

    @Override
    public void priAdd(PLinkHashCached<X> entry, float amount) {
        entry.priAdd(amount);
    }

    @Override
    public X key(PLinkHashCached<X> value) {
        return value.id;
    }

    /** optimized for PLink */
    @Override
    public void forEachKey( Consumer<? super X> each) {
        forEach(x -> each.accept(x.id));
    }


































    @Override
    protected PLinkHashCached<X> merge(PLinkHashCached<X> existing, PLinkHashCached<X> incoming, NumberX overflowing) {
        float overflow = existing.priAddOverflow(incoming.priElseZero() );
        if (overflow > 0) {
            
            if (overflowing!=null) overflowing.add(overflow);
        }
        return existing;
    }







}
