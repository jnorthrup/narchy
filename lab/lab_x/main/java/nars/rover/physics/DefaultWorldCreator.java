package nars.rover.physics;

import spacegraph.space2d.phys.common.Vec2;
import spacegraph.space2d.phys.dynamics.World;

public class DefaultWorldCreator implements WorldCreator {

    @Override
    public World createWorld(Vec2 gravity) {
        return new World(gravity);            
    }
}
