package spacegraph.space2d.phys.callbacks;

import spacegraph.space2d.phys.dynamics.Fixture;
import spacegraph.util.math.v2;

public interface ParticleRaycastCallback {
    /**
     * Called for each particle found in the query. See
     * {@link RayCastCallback#reportFixture(Fixture, v2, v2, float)} for
     * argument info.
     *
     * @param index
     * @param point
     * @param normal
     * @param fraction
     * @return
     */
    float reportParticle(int index, v2 point, v2 normal, float fraction);

}
