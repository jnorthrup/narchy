package jcog.sort;

import com.google.common.collect.Iterators;
import jcog.Util;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;

import java.util.Iterator;

import static java.lang.Float.NEGATIVE_INFINITY;

public class Top<T> implements TopFilter<T> {
    public final FloatRank<T> rank;
    /* TODO private */ public T the;
    public float score;

    public Top(FloatFunction<T> rank) {
        this(FloatRank.the(rank));
    }

    public Top(FloatRank<T> rank) {
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

    public final T get() { return the; }

    @Override
    public String toString() {
        return the + "=" + score;
    }

    @Override
    public void accept(T x) {
        float override = rank.rank(x, score);
        if (override==override && override > score) {
            the = x;
            score = override;
        }
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
    public final Iterator<T> iterator() {
        T t = the;
        return t == null ? Util.emptyIterator : Iterators.singletonIterator(t);
    }
}
