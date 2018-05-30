package jcog.bag.util;

import jcog.bag.Bag;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Random;
import java.util.function.Consumer;

/**
 * proxies to a delegate bag

 * TODO find any inherited methods which would return the proxied
 * bag instead of this instance
 */
abstract public class ProxyBag<X,Y> implements Bag<X,Y> {

    @NotNull Bag<X,Y> bag;

    protected ProxyBag(Bag<X, Y> delegate) {
        set(delegate);
    }

    public final void set(Bag<X,Y> delegate) {
        bag = delegate;
    }

    @Override
    public @Nullable Y get(@NotNull Object key) {
        return bag.get(key);
    }

    @Override
    public int capacity() {
        return bag.capacity();
    }

    @Override
    public void forEach(Consumer<? super Y> action) {
        bag.forEach(action);
    }

    @Override
    public void forEachKey(Consumer<? super X> each) {
        bag.forEachKey(each);
    }



    public void forEach(int max, @NotNull Consumer<? super Y> action) {
        throw new UnsupportedOperationException(); 
    }

    @Override
    public void clear() {
        bag.clear();
    }

    @Nullable
    @Override
    public Y remove(@NotNull X x) {
        return bag.remove(x);
    }

    @Override
    public Y put(@NotNull Y b, @Nullable MutableFloat overflowing) {
        return bag.put(b, overflowing);
    }


    @NotNull
    @Override
    public Iterable<Y> sample(Random rng, @NotNull Bag.BagCursor<? super Y> each) {
        bag.sample(rng, each);
        return this;
    }

    @Override
    public int size() {
        return bag.size();
    }

    @NotNull
    @Override
    public Iterator<Y> iterator() {
        return bag.iterator();
    }


    @Override
    public void setCapacity(int c) {
        bag.setCapacity(c);
    }

    @Override
    public float priMax() {
        return bag.priMax();
    }

    @Override
    public float priMin() {
        return bag.priMin();
    }


    @NotNull
    @Override
    public Bag<X,Y> commit(Consumer<Y> update) {
        bag.commit(update);
        return this;
    }



    @Override
    public void onAdd(Y v) {
        bag.onAdd(v);
    }

    @Override
    public void onRemove(@NotNull Y v) {
        bag.onRemove(v);
    }

    @Override
    public void onReject(@NotNull Y v) {
        bag.onReject(v);
    }

    @Override
    public Iterable<Y> commit() {
        bag.commit();
        return this;
    }
}
