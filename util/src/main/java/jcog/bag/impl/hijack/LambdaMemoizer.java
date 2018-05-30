package jcog.bag.impl.hijack;

import jcog.Util;
import jcog.memoize.Memoize;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * https:
 * h ttps:
 */
public class LambdaMemoizer {


    final static private AtomicInteger serial = new AtomicInteger();


    public static <V> Function<Object[], V> memoize(
            Method method, MemoizeBuilder<V> memoizer) {
        return memoize((Class<? super V>) method.getReturnType(), method, memoizer);
    }

    /** method should be static */
    public static <V> Function<Object[], V> memoize(Class<? super V> returnType,
            Method method, MemoizeBuilder<V> memoizer) {

        MethodHandles.Lookup lookup = MethodHandles.lookup();
        try {
            MethodHandle methodHandle = lookup.unreflect(method)
                    .asSpreader(Object[].class, method.getParameterCount());

            return memoize(memoizer, methodHandle);

        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }


    public static <V> Function<Object[], V> memoize(MemoizeBuilder<V> memoizer, MethodHandle methodHandle) {
        int methodID = serial.getAndIncrement();

        Function<ArgKey, V> compute = x -> {
            try {
                return (V) methodHandle.invoke(x.args);
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        };

        final Memoize<ArgKey, V> memoizedCalls = memoizer.apply(compute);

        return args -> memoizedCalls.apply(new ArgKey(methodID, args));
    }


    public static <V> Function<Object[], V> memoize(Class klass, String methodName, Class[] paramTypes, MemoizeBuilder<V> m) {
        try {
            Method method = klass.getMethod(methodName, paramTypes);
            method.trySetAccessible();
            return memoize(method, m);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * TODO make work like TermKey, etc
     */
    public static class ArgKey {

        public final int methodID;

        public final Object[] args;
        private final int hash;

        private ArgKey(int methodID, Object[] args) {
            this.methodID = methodID;
            this.args = args;
            this.hash = Util.hashCombine(methodID, args != null ? Arrays.hashCode(args) : 1);
        }

        @Override
        public boolean equals(Object o) {
            ArgKey argList = (ArgKey) o;
            return hash == argList.hash && Arrays.equals(args, argList.args);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    @FunctionalInterface
    public interface MemoizeBuilder<V> extends Function<Function<ArgKey, V>, Memoize<ArgKey, V>> {

    }

}
