package jcog.sort;

import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

@FunctionalInterface public interface FloatRank<X> extends FloatFunction<X> {
    /**
     *
     * @param value
     * @param min this value which may be NEGATIVE_INFINITY, is a value that the rank must exceed to matter.
     *            so if a scoring function can know that, before completing,
     *            it wont meet this threshold, it can fail fast (by returning NaN).
     * @return
     */
    float rank(X x, float min);

    default float rank(X x) {
        return rank(x, Float.NEGATIVE_INFINITY);
    }

    /** adapter which ignores the minimum */
    static <X> FloatRank<X> the(FloatFunction<X> f) {
        if (f instanceof FloatRank)
            return ((FloatRank)f);
        else
            return (x, min) -> f.floatValueOf(x);
    }


    @Override
    default float floatValueOf(X x) {
        return rank(x, Float.NEGATIVE_INFINITY);
    }

    default FloatRank<X> filter(@Nullable Predicate<X> filter) {
        if (filter == null) return this;
        return new FilteredFloatRank<>(filter, this);

    }

    final class FilteredFloatRank<X> implements FloatRank<X> {

        private @Nullable final Predicate<X> filter;
        private final FloatRank<X> rank;

        public FilteredFloatRank(@Nullable Predicate<X> filter, FloatRank<X> rank) {
            this.filter = filter;
            this.rank = rank;
        }

        @Override
        public float rank(X t, float m) {
            if (filter != null && !filter.test(t))
                return Float.NaN;
            return rank.floatValueOf(t);
        }
    }
}
