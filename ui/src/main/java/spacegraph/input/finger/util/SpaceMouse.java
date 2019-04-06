package spacegraph.input.finger.util;

import com.jogamp.newt.event.MouseAdapter;
import spacegraph.space3d.SpaceDisplayGraph3D;

/**
 * 3D camera control
 */
abstract class SpaceMouse extends MouseAdapter {

    final SpaceDisplayGraph3D space;

    SpaceMouse(SpaceDisplayGraph3D g) {
        this.space = g;
    }
}
