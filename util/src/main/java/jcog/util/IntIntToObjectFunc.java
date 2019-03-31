package jcog.util;

/** two argument non-variable integer functor (convenience method) */
@FunctionalInterface public interface IntIntToObjectFunc<X> {
    X apply(int x, int y);
}
