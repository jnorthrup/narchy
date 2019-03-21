package jcog.pri.bag.impl;

import jcog.data.NumberX;
import jcog.pri.PriBuffer;
import jcog.pri.Prioritizable;
import jcog.pri.Prioritized;
import jcog.pri.bag.Bag;
import jcog.pri.bag.util.ProxyBag;
import jcog.pri.op.PriMerge;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * concurrent buffering bag wrapper
 */
abstract public class BufferedBag<X, B, Y extends Prioritizable> extends ProxyBag<X, Y> {

//    final AtomicBoolean busy = new AtomicBoolean(false);

    /**
     * pre-bag accumulating buffer
     */
    public final PriBuffer<B> pre;

    public BufferedBag(Bag<X, Y> bag, PriBuffer<B> pre) {
        super(bag);
        this.pre = pre;
        merge(bag.merge()); //by default.  changing this later will set pre and bag's merges
    }


    @Override
    public void clear() {
        pre.clear();
        super.clear();
    }

    @Override
    public final Bag<X, Y> commit(@Nullable Consumer<Y> update) {

//        if (busy.compareAndSet(false, true)) {
//            try {

                bag.commit(update);

                if (!pre.isEmpty()) {

                    //before
//                    boolean growDuringMerge = bag instanceof ArrayBag;
//                    int cap = bag.capacity();
//                    if (growDuringMerge) {
//                        bag.setCapacity(Math.max(cap, Math.min(cap * OVER_CAPACITY_FACTOR, bag.size() + pre.size()))); //expand before
//                    }

                    //merge
                    pre.drain(bag::putAsync, this::valueInternal);

                    //after
//                    if (growDuringMerge) {
//                        bag.setCapacity(cap); //contract after
//                    }

                    //bag.commit(after); //force sort after
                }

//            } finally {
//                busy.set(false);
//            }
//        }

        return this;
    }

    protected abstract Y valueInternal(B b, float pri);

    @Override
    public int size() {
        return Math.max(bag.size(), pre.size());
    }

    @Override
    public final Y put(Y x) {
        return (Y) put((B) x, ((Prioritized) x).pri());
    }

    @Override
    public final Y put(Y b, @Nullable NumberX overflowingIgnored) {
        return put(b);
    }

    public final B put(B x, float p) {
        return pre.put(x, p, merge());
    }

    @Override
    public final boolean isEmpty() {
        return bag.isEmpty() && pre.isEmpty();
    }


    public void merge(PriMerge nextMerge) {
        super.merge(nextMerge);
        pre.merge(nextMerge);
        bag.merge(nextMerge);
    }

    public static class SimpleBufferedBag<X, Y extends Prioritizable> extends BufferedBag<X, Y, Y> {

        public SimpleBufferedBag(Bag<X, Y> activates, PriBuffer<Y> conceptPriBuffer) {
            super(activates, conceptPriBuffer);
        }

        @Override
        protected final Y valueInternal(Y c, float pri) {
            return c;
        }

    }

}
