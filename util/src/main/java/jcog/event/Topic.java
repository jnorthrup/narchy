
package jcog.event;


import jcog.exe.Exe;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * notifies subscribers when a value is emitted (publisher)
 */
public interface Topic<X> extends Iterable<Consumer<X>> {

    void enable(Consumer<X> o);

    void disable(Consumer<X> o);

    void clear();

    default void emitAsync(X x) {
        emitAsync(x, Exe.executor());
    }


    static Offs all(Object obj, BiConsumer<String /* fieldName*/, Object /* value */> f) {
        return all(obj, f, (key) -> true);
    }

    /**
     * warning this could grow large TODO use a soft cache
     */
    Map<Class, Field[]> fieldCache = new ConcurrentHashMap();

    static void each(Class c, Consumer<Field /* fieldName*/> f) {
        /** TODO cache the fields because reflection may be slow */


        for (Field field : fieldCache.computeIfAbsent(c, (cc) ->
                Stream.of(cc.getFields()).filter(x -> x.getType().equals(Topic.class)).toArray(Field[]::new)
        )) {
            f.accept(field);
        }

    }


    /**
     * registers to all public Topic fields in an object
     * BiConsumer<String  fieldName, Object  value >
     */
    static Offs all(Object obj, BiConsumer<String, Object> f, Predicate<String> includeKey) {

        Offs s = new Offs();

        each(obj.getClass(), (field) -> {
            String fieldName = field.getName();
            if (includeKey != null && !includeKey.test(fieldName))
                return;

            try {
                Topic t = ((Topic) field.get(obj));


                s.add(
                        t.on((nextValue) -> f.accept(
                                fieldName /* could also be the Topic itself */,
                                nextValue
                        )));

            } catch (IllegalAccessException e) {
                f.accept(fieldName, e);
            }

        });


        return s;
    }


    /**
     * TODO rename to 'out' to match Streams api
     */
    void emit(X arg);

    default Off on(long minUpdatePeriodMS, Consumer<X> o) {
        if (minUpdatePeriodMS == 0)
            return on(o);
        return on(System::currentTimeMillis, () -> minUpdatePeriodMS, o);
    }

    default Off on(LongSupplier time, LongSupplier minUpdatePeriod, Consumer<X> o) {
        AtomicLong lastUpdate = new AtomicLong(time.getAsLong() - minUpdatePeriod.getAsLong());
        return on((x) -> {
            long now = time.getAsLong();
            if (now - lastUpdate.get() >= minUpdatePeriod.getAsLong()) {
                lastUpdate.set(now);
                o.accept(x);
            }
        });
    }

    default Off on(Consumer<X> o) {
        return new AbstractOff.Strong<>(this, o);
    }

    default Off on(Runnable o) {
        return on(new ConsumerAdapter<>(o));
    }

    default Off onWeak(Consumer<X> o) {
        return new AbstractOff.Weak<>(this, o);
    }

    default Off onWeak(Runnable o) {
        return onWeak(new ConsumerAdapter<>(o));
    }


    int size();

    boolean isEmpty();

    void emitAsync(X inputted, Executor e);

    void emitAsyncAndWait(X inputted, Executor e) throws InterruptedException;

    void emitAsync(X inputted, Executor e, Runnable onFinish);


}