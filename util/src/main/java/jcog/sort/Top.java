package jcog.sort;

import org.eclipse.collections.api.block.function.primitive.FloatFunction;

import java.util.function.Consumer;

import static java.lang.Float.NEGATIVE_INFINITY;

public class Top<T> implements Consumer<T> {
    /** TODO use FloatRank */
    public final FloatFunction<T> rank;
    /* TODO private */ public T the;
    public float score;

    public Top(FloatFunction<T> rank) {
        this.rank = rank;
        this.score = NEGATIVE_INFINITY;
    }

    public void clear() {
        score = NEGATIVE_INFINITY;
        the = null;
    }

    public final T get() { return the; }

    @Override
    public String toString() {
        return the + "=" + score;
    }

    @Override
    public void accept(T x) {
        float override = rank.floatValueOf(x);
        if (override==override && override > score) {
            the = x;
            score = override;
        }
    }

//    public Top<T> of(Iterator<T> iterator) {
//        iterator.forEachRemaining(this);
//        return this;
//    }

    public boolean isEmpty() {
        return the==null;
    }
}
