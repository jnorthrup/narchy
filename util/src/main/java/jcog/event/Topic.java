
package jcog.event;


import jcog.exe.Exe;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.*;

/**
 * notifies subscribers when a value is emitted (publisher)
 */
public interface Topic<X> extends Iterable<Consumer<X>> {

    void start(Consumer<X> o);

    void stop(Consumer<X> o);

    void clear();

    default void emitAsync(X x) {
        emitAsync(x, Exe.executor());
    }


    static RunThese all(Object obj, BiConsumer<String /* fieldName*/, Object /* value */> f) {
        return all(obj, f, new Predicate<String>() {
            @Override
            public boolean test(String key) {
                return true;
            }
        });
    }

    /**
     * warning this could grow large TODO use a soft cache
     */
    Map<Class<?>, Field[]> fieldCache = new ConcurrentHashMap<>();

    static void each(Class<?> c, Consumer<Field /* fieldName*/> f) {
        /** TODO cache the fields because reflection may be slow */


        for (Field field : fieldCache.computeIfAbsent(c, new Function<Class<?>, Field[]>() {
                    @Override
                    public Field[] apply(Class<?> cc) {
                        List<Field> list = new ArrayList<>();
                        for (Field x : cc.getFields()) {
                            if (x.getType() == Topic.class) {
                                list.add(x);
                            }
                        }
                        return list.toArray(new Field[0]);
                    }
                }
        )) {
            f.accept(field);
        }

    }


    /**
     * registers to all public Topic fields in an object
     * BiConsumer<String  fieldName, Object  value >
     */
    static <X> RunThese all(X obj, BiConsumer<String, X> f, Predicate<String> includeKey) {

        RunThese s = new RunThese();

        each(obj.getClass(), new Consumer<Field>() {
            @Override
            public void accept(Field field) {
                String fieldName = field.getName();
                if (includeKey != null && !includeKey.test(fieldName))
                    return;

                try {
                    Topic<X> t = ((Topic<X>) field.get(obj));


                    s.add(
                            t.on(new Consumer<X>() {
                                @Override
                                public void accept(X nextValue) {
                                    f.accept(
                                            fieldName /* could also be the Topic itself */,
                                            nextValue
                                    );
                                }
                            }));

                } catch (IllegalAccessException e) {
                    //f.accept(fieldName, e);
                    throw new RuntimeException(e);
                }

            }
        });


        return s;
    }

    /** broadcast the signal to zero or more attached recipients */
    void emit(X x);

    /** emits the supplier procedure's result IF there is any listener to receive it */
    default   void emit(Supplier<X> t) {
        if (!isEmpty()) {
            X x = t.get();
            if (x!=null)
                emit(x);
        }
    }

    default Off on(long minUpdatePeriodMS, Consumer<X> o) {
        return minUpdatePeriodMS == 0L ? on(o) :
                on(System::currentTimeMillis, new LongSupplier() {
                    @Override
                    public long getAsLong() {
                        return minUpdatePeriodMS;
                    }
                }, o);
    }

    default Off on(LongSupplier time, LongSupplier minUpdatePeriod, Consumer<X> o) {
        AtomicLong lastUpdate = new AtomicLong(time.getAsLong() - minUpdatePeriod.getAsLong());
        return on(new Consumer<X>() {
            @Override
            public void accept(X x) {
                long now = time.getAsLong();
                if (now - lastUpdate.get() >= minUpdatePeriod.getAsLong()) {
                    lastUpdate.set(now);
                    o.accept(x);
                }
            }
        });
    }

    default Off on(Consumer<X> o, boolean strong) {
        return strong ? on(o) : onWeak(o);
    }

    default Off on(Consumer<X> o) {
        return new AbstractOff.Strong<>(this, o);
    }

    default Off on(Runnable o) {
        return on(new ConsumerAdapter(o));
    }

    default Off onWeak(Consumer<X> o) {
        return AbstractOff.weak(this, o);
        ///return new AbstractOff.Weak<>(this, o);
    }

    default Off onWeak(Runnable o) {
        return onWeak(new ConsumerAdapter(o));
    }


    int size();

    boolean isEmpty();

    void emitAsync(X inputted, Executor e);

    void emitAsyncAndWait(X inputted, Executor e) throws InterruptedException;

    void emitAsync(X inputted, Executor e, Runnable onFinish);


}