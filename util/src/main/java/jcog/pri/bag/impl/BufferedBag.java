package jcog.pri.bag.impl;

import jcog.data.NumberX;
import jcog.pri.PriMap;
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

	/**
	 * pre-bag accumulating buffer
	 */
	public final PriMap<B> pre;

	public BufferedBag(Bag<X, Y> bag, PriMap<B> pre) {
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

	protected abstract Y valueInternal(B b);
//	protected abstract Y valueInternal(B b, float pri);

	@Override
	public int size() {
		return Math.max(bag.size(), pre.size());
	}

	@Override
	public final Y put(Y x) {
		float pri = ((Prioritized) x).pri();

//        boolean tryBypass = true;
//        if (tryBypass) {
//            Y y = bag.get(x);
//            if (y != null && !y.isDeleted()) {
//                float delta = pre.merge(y, (B)x, pri, merge());
//                if (delta > Float.MIN_NORMAL)
//                    pressurize(delta);
//                return y;
//            }
//        }

		return (Y) pre.put((B) x, pri,
			merge(),
			this::pressurize
		);
	}


	@Override
	public final Y put(Y b, @Nullable NumberX overflowingIgnored) {
		return put(b);
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


}
