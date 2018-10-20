package spacegraph.input.key;

import com.jogamp.newt.event.KeyEvent;
import spacegraph.util.math.v2;

public interface KeyPressed {


    /**
     * for receiving keyboard events in 2d context
     * returns true if the event has been absorbed, false if it should continue propagating
     */
    default boolean key(KeyEvent e, boolean pressedOrReleased) {
        return false;
    }

    /**
     * for receiving keyboard events in 3d context
     * returns true if the event has been absorbed, false if it should continue propagating
     */
    @Deprecated default boolean key(v2 hitPoint, char charCode, boolean pressedOrReleased) {
        return false;
    }

}
