package nars.rover.physics;

import spacegraph.space2d.phys.common.Vec2;
import spacegraph.space2d.phys.dynamics.World;

public interface WorldCreator {
  World createWorld(Vec2 gravity);
}
