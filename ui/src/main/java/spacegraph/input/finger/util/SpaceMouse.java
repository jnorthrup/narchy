package spacegraph.input.finger.util;

import com.jogamp.newt.event.MouseAdapter;
import spacegraph.space3d.SpaceGraph3D;

/**
 * 3D camera control
 */
abstract class SpaceMouse extends MouseAdapter {

    final SpaceGraph3D space;

    SpaceMouse(SpaceGraph3D g) {
        this.space = g;
    }
}
