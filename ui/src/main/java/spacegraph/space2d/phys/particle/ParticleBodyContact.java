package spacegraph.space2d.phys.particle;

import spacegraph.space2d.phys.common.Vec2;
import spacegraph.space2d.phys.dynamics.Body2D;
import spacegraph.util.math.v2;

public class ParticleBodyContact {
    /**
     * Index of the particle making contact.
     */
    public int index;
    /**
     * The body making contact.
     */
    public Body2D body;
    /**
     * Weight of the contact. A value between 0.0f and 1.0f.
     */
    float weight;
    /**
     * The normalized direction from the particle to the body.
     */
    public final v2 normal = new Vec2();
    /**
     * The effective mass used in calculating force.
     */
    float mass;
}
