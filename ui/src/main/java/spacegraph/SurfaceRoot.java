package spacegraph;

import com.jogamp.opengl.GL2;
import jcog.event.On;
import org.jetbrains.annotations.Nullable;
import spacegraph.render.JoglSpace;

import java.util.function.Consumer;

public interface SurfaceRoot {

    default SurfaceRoot root() {
        return this;
    }

    Ortho move(float x, float y);

    default Ortho scale(float s) {
        return scale(s, s);
    }

    Ortho scale(float sx, float sy);

    void zoom(float x, float y, float sx, float sy);
    void unzoom();

    /** receives notifications, logs, etc */
    On onLog(Consumer o);

    GL2 gl();

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
    On onUpdate(Consumer<JoglSpace> c);
}
