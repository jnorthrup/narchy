package spacegraph;

import jcog.event.On;
import org.jetbrains.annotations.Nullable;
import org.slf4j.event.Level;
import spacegraph.render.JoglWindow;

import java.util.function.Consumer;
import java.util.function.Supplier;

public interface SurfaceRoot extends SurfaceBase {

    default SurfaceRoot root() {
        return this;
    }

    default void zoom(Surface s) {
        //ignored
    }
    default void unzoom() {
        //ignored
    }

    ///** broadcast notifications, logs, etc */
    //On onLog(Consumer o);


    /**
     * puts value into singleton table
     * can provide special handling for lifecycle states of stored entries
     * by providing a callback which will be invoked when the value is replaced.
     *
     * if 'added' == null, it will attempt to remove any set value.
     */
    void the(String key, @Nullable Object added, @Nullable Runnable onRemove);

    /** gets value from the singleton table */
    Object the(String key);

    default void the(Class key, @Nullable Object added, @Nullable Runnable onRemove) {
        the(key.toString(), added, onRemove);
    }
    default Object the(Class key) {
        return the(key.toString());
    }


    /** attaches an event handler for updates (less frequent than render cycle) */
    On onUpdate(Consumer<JoglWindow> c);

    /** new log message will likely replace an existing log message by the same key. */
    default void log(@Nullable Object key, float duration /* seconds */, Level level, Supplier<String> message) {
        if (logging(level))
            System.out.println(message.get());
    }

    default boolean logging(Level level) {
        return true;
    }

    default void debug(@Nullable Object key, float duration /* seconds */, Supplier<String> message) {
        log(key, duration, Level.DEBUG, message);
    }
    default void debug(@Nullable Object key, float duration /* seconds */, String message) {
        log(key, duration, Level.DEBUG, ()->message);
    }
    default void info(@Nullable Object key, float duration /* seconds */, Supplier<String> message) {
        log(key, duration, Level.INFO, message);
    }
    default void info(@Nullable Object key, float duration /* seconds */, String message) {
        log(key, duration, Level.INFO, ()->message);
    }
    default void error(@Nullable Object key, float duration /* seconds */, Supplier<String> message) {
        log(key, duration, Level.ERROR, message);
    }
    default void error(@Nullable Object key, float duration /* seconds */, String message) {
        log(key, duration, Level.ERROR, ()->message);
    }

}
