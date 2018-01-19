/*
 * Copyright 2016 higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jcog.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/*
 * from: Chronicle Core
 * Created by Peter Lawrey on 13/12/16.
 *
 * untested
 */
public enum Mocker {
    ;

    @NotNull
    public static <T> T logging(@NotNull Class<T> tClass, String description, @NotNull PrintStream out) {
        return intercepting(tClass, description, out::println);
    }

    @NotNull
    public static <T> T logging(@NotNull Class<T> tClass, String description, @NotNull PrintWriter out) {
        return intercepting(tClass, description, out::println);
    }

    @NotNull
    public static <T> T logging(@NotNull Class<T> tClass, String description, @NotNull StringWriter out) {
        return logging(tClass, description, new PrintWriter(out));
    }

    @NotNull
    public static <T> T queuing(@NotNull Class<T> tClass, String description, @NotNull BlockingQueue<String> queue) {
        return intercepting(tClass, description, queue::add);
    }

    @NotNull
    public static <T> T intercepting(@NotNull Class<T> tClass, String description, @NotNull Consumer<String> consumer) {
        return intercepting(tClass, description, consumer, null);
    }

    @NotNull
    public static <T> T intercepting(@NotNull Class<T> tClass, @NotNull final String description, @NotNull Consumer<String> consumer, T t) {
        return intercepting(tClass,
                (name, args) -> consumer.accept(description + name + (args == null ? "()" : Arrays.toString(args))),
                t);
    }

    @NotNull
    public static <T> T intercepting(@NotNull Class<T> tClass, @NotNull BiConsumer<String, Object[]> consumer, T t) {
        //noinspection unchecked
        return (T) Proxy.newProxyInstance(tClass.getClassLoader(), new Class[]{tClass}, new AbstractInvocationHandler(ConcurrentHashMap::new) {
            @Override
            protected Object doInvoke(Object proxy, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
                consumer.accept(method.getName(), args);
                if (t != null)
                    method.invoke(t, args);
                return null;
            }
        });
    }

    @NotNull
    public static <T> T ignored(@NotNull Class<T> tClass) {
        //noinspection unchecked
        return (T) Proxy.newProxyInstance(tClass.getClassLoader(), new Class[]{tClass}, new AbstractInvocationHandler(ConcurrentHashMap::new) {
            @Override
            protected Object doInvoke(Object proxy, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
                return null;
            }
        });
    }

    static public abstract class AbstractInvocationHandler implements InvocationHandler {
        // Lookup which allows access to default methods in another package.
        private static final ClassLocal<MethodHandles.Lookup> PRIVATE_LOOKUP = ClassLocal.withInitial(AbstractInvocationHandler::acquireLookup);
        private static final Object[] NO_ARGS = {};
        // called when close() is called.
        private Closeable closeable;
//        // cache the proxy to MethodHandler lookup.
//        private Map<Object, Function<Method, MethodHandle>> proxyToLambda;
//        private Map<Method, MethodHandle> defaultMethod;

        /**
         * @param mapSupplier ConcurrentHashMap::new for thread safe, HashMap::new for single thread, Collections::emptyMap to turn off.
         */
        protected AbstractInvocationHandler(Supplier<Map> mapSupplier) {
//            //noinspection unchecked
//            proxyToLambda = mapSupplier.get();
//            //noinspection unchecked
//            defaultMethod = mapSupplier.get();
        }

        private static MethodHandles.Lookup acquireLookup(Class<?> c) {
            try {
                // try to create one using a constructor
                Constructor<MethodHandles.Lookup> lookupConstructor =
                        MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, Integer.TYPE);
                if (!lookupConstructor.isAccessible()) {
                    lookupConstructor.setAccessible(true);
                }
                return lookupConstructor.newInstance(c, MethodHandles.Lookup.PRIVATE);
            } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException ignored) {
            }
            try {
                // Try to grab an internal one,
                final Field field = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
                field.setAccessible(true);
                return (MethodHandles.Lookup) field.get(null);
            } catch (Exception e) {
                // use the default to produce an error message.
                return MethodHandles.lookup();
            }
        }

        @Override
        public final Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Class<?> declaringClass = method.getDeclaringClass();
            if (declaringClass == Object.class) {
                return method.invoke(this, args);

            } else if (declaringClass == Closeable.class && method.getName().equals("close")) {

                closeQuietly(closeable);
                return null;

//        } else if (method.isDefault()) {
//            // this will call the default impl. of the method, not the proxy's impl.
//            Function<Method, MethodHandle> function = proxyToLambda.computeIfAbsent(proxy, p -> m -> methodHandleForProxy(p, m));
//            MethodHandle methodHandle = defaultMethod.computeIfAbsent(method, function);
//            return methodHandle.invokeWithArguments(args);
            }

            if (args == null)
                args = NO_ARGS;

            Object o = doInvoke(proxy, method, args);

            return o == null ? defaultValues.getOrDefault(method.getReturnType(), null) : o;
        }

        static void closeQuietly(@Nullable Object o) {
            if (o instanceof Object[]) {
                for (Object o2 : (Object[]) o) {
                    closeQuietly(o2);
                }
            } else if (o instanceof java.io.Closeable) {
                try {
                    ((java.io.Closeable) o).close();
                } catch (IOException e) {
                    LoggerFactory.getLogger(Closeable.class).debug("", e);
                }
            }
        }

        static final Map<Class,Object> defaultValues = Map.of(
                boolean.class, false,
                byte.class, (byte) 0,
                short.class, (short) 0,
                char.class, (char) 0,
                int.class, 0,
                long.class, 0L,
                float.class, 0.0f,
                double.class, 0.0
                );

        /**
         * Default handler for method call.
         */
        protected abstract Object doInvoke(Object proxy, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException;

        @SuppressWarnings("WeakerAccess")
        MethodHandle methodHandleForProxy(Object proxy, Method m) {
            try {
                Class<?> declaringClass = m.getDeclaringClass();
                final MethodHandles.Lookup lookup = PRIVATE_LOOKUP.get(declaringClass);
                return lookup
                        .in(declaringClass)
                        .unreflectSpecial(m, declaringClass)
                        .bindTo(proxy);
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }

        public void onClose(Closeable closeable) {
            this.closeable = closeable;
        }
    }

}
