package org.jbox2d.particle;

import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body2D;
import spacegraph.math.Tuple2f;

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
    public final Tuple2f normal = new Vec2();
    /**
     * The effective mass used in calculating force.
     */
    float mass;
}
