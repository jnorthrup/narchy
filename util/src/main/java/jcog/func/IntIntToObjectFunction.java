package jcog.func;

/** two argument non-variable integer functor (convenience method) */
@FunctionalInterface public interface IntIntToObjectFunction<X> {
    X apply(int x, int y);
}
