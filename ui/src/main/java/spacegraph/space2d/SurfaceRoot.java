package spacegraph.space2d;

import jcog.event.On;
import org.jetbrains.annotations.Nullable;
import spacegraph.util.SpaceLogger;
import spacegraph.video.JoglWindow;

import java.util.function.Consumer;

public interface SurfaceRoot extends SurfaceBase, SpaceLogger {

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

}
