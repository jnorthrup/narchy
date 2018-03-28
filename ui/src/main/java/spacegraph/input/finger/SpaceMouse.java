package spacegraph.input.finger;

import com.jogamp.newt.event.MouseAdapter;
import spacegraph.space3d.SpaceGraphPhys3D;

/**
 * Created by me on 11/20/16.
 */
public abstract class SpaceMouse extends MouseAdapter {

    public final SpaceGraphPhys3D space;

    protected SpaceMouse(SpaceGraphPhys3D g) {
        this.space = g;
    }
}
