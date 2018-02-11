
package jcog.event;


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
 * notifies subscribers when a value is emitted
 */
public interface Topic<V> {

    void enable(Consumer<V> o);

    void disable(Consumer<V> o);

    void clear();

    //List<Consumer<V>> all();


    static Ons all(Object obj, BiConsumer<String /* fieldName*/, Object /* value */> f) {
        return all(obj, f, (key)->true);
    }

    /** warning this could grow large */
    Map<Class,Field[]> fieldCache = new ConcurrentHashMap();

    static void each(Class c, Consumer<Field /* fieldName*/> f) {
        /** TODO cache the fields because reflection may be slow */


        for (Field field : fieldCache.computeIfAbsent(c, (cc) ->
            Stream.of(cc.getFields()).filter(x -> x.getType().equals(Topic.class)).toArray(Field[]::new)
        )) {
            f.accept(field);
        }

    }


    /** registers to all public Topic fields in an object
     * BiConsumer<String  fieldName, Object  value >
     * */
    static Ons all(Object obj, BiConsumer<String, Object> f, Predicate<String> includeKey) {

        Ons s = new Ons();

        each(obj.getClass(), (field) -> {
            String fieldName = field.getName();
            if (includeKey!=null && !includeKey.test(fieldName))
                return;

            try {
                Topic t = ((Topic) field.get(obj));

                // could send start message: f.accept(f.getName(),  );

                s.add(
                        t.on((nextValue) -> f.accept(
                                fieldName /* could also be the Topic itself */,
                                nextValue
                        )));

            } catch (IllegalAccessException e) {
                f.accept( fieldName, e);
            }

        });


        return s;
    }



    /** TODO rename to 'out' to match Streams api */
    void emit(V arg);

    default On on(long minUpdatePeriodMS, Consumer<V> o) {
        if (minUpdatePeriodMS == 0)
            return on(o);
        return on(System::currentTimeMillis, ()->minUpdatePeriodMS, o);
    }

    default On on(LongSupplier time, LongSupplier minUpdatePeriod, Consumer<V> o) {
        AtomicLong lastUpdate = new AtomicLong(time.getAsLong() - minUpdatePeriod.getAsLong() );
        return on((v) -> {
            long now = time.getAsLong();
            if (now - lastUpdate.get() >= minUpdatePeriod.getAsLong()) {
                lastUpdate.set(now);
                o.accept(v);
            }
        });
    }

    default On on(Consumer<V> o) {
        return new On.Strong<>(this, o);
    }

    default On on(Runnable o) {
        return new On.Strong<>(this, (ignored)->o.run());
    }

    default On onWeak(Consumer<V> o) {
        return new On.Weak<>(this, o);
    }


//    @SafeVarargs
//    static <V> Active onAll(@NotNull Consumer<V> o, Topic... w) {
//        Active r = new Active(w.length);
//
//        for (Topic<V> c : w)
//            r.add( c.on(o) );
//
//        return r;
//    }

    int size();

    boolean isEmpty();

    void emitAsync(V inputted, Executor e);

    void emitAsyncAndWait(V inputted, Executor e) throws InterruptedException;

    void emitAsync(V inputted, Executor e, Runnable onFinish);

//    String name();


//
//    @Override
//    @Deprecated public void emit(Class channel, Object arg) {
//
//        if (!(arg instanceof Object[]))
//            super.emit(channel, new Object[] { arg });
//        else
//            super.emit(channel, arg);
//    }

}