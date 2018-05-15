package jcog.sort;

import org.eclipse.collections.api.block.function.primitive.FloatFunction;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

public class Top<T> implements Consumer<T> {
    public final FloatFunction<T> rank;
    public T the;
    public float score;

    public Top(FloatFunction<T> rank) {
        this(rank, Float.NEGATIVE_INFINITY);
    }

    public Top(FloatFunction<T> rank, float minScore) {
        this.rank = rank;
        this.score = minScore;
    }

    @Override
    public void accept(T x) {
        accept(x, rank.floatValueOf(x));
    }
    public void accept(T x, float override) {
        if (override==override && override >= score) { //note: >=
            the = x;
            score = override;
        }
    }

    public List<T> toList() {
        if (the == null) return Collections.emptyList();
        else return Collections.singletonList(the);
    }

    public Top<T> of(Iterator<T> iterator) {
        iterator.forEachRemaining(this);
        return this;
    }

    public boolean isEmpty() {
        return the==null;
    }
}
