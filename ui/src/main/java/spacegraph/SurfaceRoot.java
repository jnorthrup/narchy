package spacegraph;

import com.jogamp.opengl.GL2;
import jcog.event.On;
import org.eclipse.collections.api.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public interface SurfaceRoot {

    default SurfaceRoot root() {
        return this;
    }

    Ortho move(float x, float y);

    Ortho scale(float s);

    Ortho scale(float sx, float sy);

    void zoom(float x, float y, float sx, float sy);

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

}
