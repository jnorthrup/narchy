package jcog.sort;

import com.google.common.collect.Iterators;
import jcog.Util;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;

import static java.lang.Float.NEGATIVE_INFINITY;

public class Top<X> implements TopFilter<X> {
    public final FloatRank<X> rank;
    /* TODO private */ public X the;
    public float score;

    public Top(FloatFunction<X> rank) {
        this(FloatRank.the(rank));
    }

    public Top(FloatRank<X> rank) {
        this.rank = rank;
        this.score = NEGATIVE_INFINITY;
    }

    public void clear() {
        score = NEGATIVE_INFINITY;
        the = null;
    }

    @Override
    public int size() {
        return the==null ? 0 : 1;
    }

    public final X get() { return the; }

    @Override
    public String toString() {
        return the + "=" + score;
    }

    @Override
    public final void accept(X x) {
       add(x);
    }

    @Override
    public boolean add(X x) {
        var override = rank.rank(x, score);
        if (override==override && override > score) {
            the = x;
            score = override;
            return true;
        }
        return false;
    }

    @Override
    public @Nullable X pop() {
        var x = this.the;
        clear();
        return x;
    }
    //    public Top<T> of(Iterator<T> iterator) {
//        iterator.forEachRemaining(this);
//        return this;
//    }

    @Override
    public boolean isEmpty() {
        return the==null;
    }

    @Override
    public final Iterator<X> iterator() {
        var x = the;
        return x == null ? Util.emptyIterator : Iterators.singletonIterator(x);
    }
}
