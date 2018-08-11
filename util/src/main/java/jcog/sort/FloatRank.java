package jcog.sort;

import org.eclipse.collections.api.block.function.primitive.FloatFunction;

@FunctionalInterface public interface FloatRank<X> {
    /**
     *
     * @param value
     * @param min this value which may be NEGATIVE_INFINITY, is a value that the rank must exceed to matter.
     *            so if a scoring function can know that, before completing,
     *            it wont meet this threshold, it can fail fast (by returning NEGATIVE_INFINITY).
     * @return
     */
    float rank(X value, float min);

    /** adapter which ignores the minimum */
    static <X> FloatRank<X> from(FloatFunction<X> f) {
        return (x, min) -> f.floatValueOf(x);
    }
}
