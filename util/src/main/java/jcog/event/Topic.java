
package jcog.event;


import jcog.exe.Exe;

import java.lang.reflect.Field;
import java.util.Arrays;
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
        return all(obj, f, (key) -> true);
    }

    /**
     * warning this could grow large TODO use a soft cache
     */
    Map<Class<?>, Field[]> fieldCache = new ConcurrentHashMap<>();

    static void each(Class<?> c, Consumer<Field /* fieldName*/> f) {
        /** TODO cache the fields because reflection may be slow */


        for (var field : fieldCache.computeIfAbsent(c, (cc) ->
            Arrays.stream(cc.getFields()).filter(x -> x.getType() == Topic.class).toArray(Field[]::new)
        )) {
            f.accept(field);
        }

    }


    /**
     * registers to all public Topic fields in an object
     * BiConsumer<String  fieldName, Object  value >
     */
    static <X> RunThese all(X obj, BiConsumer<String, X> f, Predicate<String> includeKey) {

        var s = new RunThese();

        each(obj.getClass(), (field) -> {
            var fieldName = field.getName();
            if (includeKey != null && !includeKey.test(fieldName))
                return;

            try {
                var t = ((Topic<X>) field.get(obj));


                s.add(
                        t.on((nextValue) -> f.accept(
                                fieldName /* could also be the Topic itself */,
                                nextValue
                        )));

            } catch (IllegalAccessException e) {
                //f.accept(fieldName, e);
                throw new RuntimeException(e);
            }

        });


        return s;
    }

    /** broadcast the signal to zero or more attached recipients */
    void emit(X x);

    /** emits the supplier procedure's result IF there is any listener to receive it */
    default   void emit(Supplier<X> t) {
        if (!isEmpty()) {
            var x = t.get();
            if (x!=null)
                emit(x);
        }
    }

    default Off on(long minUpdatePeriodMS, Consumer<X> o) {
        return minUpdatePeriodMS == 0 ? on(o) :
                on(System::currentTimeMillis, () -> minUpdatePeriodMS, o);
    }

    default Off on(LongSupplier time, LongSupplier minUpdatePeriod, Consumer<X> o) {
        var lastUpdate = new AtomicLong(time.getAsLong() - minUpdatePeriod.getAsLong());
        return on((x) -> {
            var now = time.getAsLong();
            if (now - lastUpdate.get() >= minUpdatePeriod.getAsLong()) {
                lastUpdate.set(now);
                o.accept(x);
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