package spacegraph.input.finger.util;

import com.jogamp.newt.event.MouseAdapter;
import spacegraph.space3d.SpaceGraphPhys3D;

/**
 * 3D camera control
 */
abstract class SpaceMouse extends MouseAdapter {

    final SpaceGraphPhys3D space;

    SpaceMouse(SpaceGraphPhys3D g) {
        this.space = g;
    }
}
