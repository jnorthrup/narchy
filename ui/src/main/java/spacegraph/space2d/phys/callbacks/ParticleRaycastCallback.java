package spacegraph.space2d.phys.callbacks;

import spacegraph.space2d.phys.dynamics.Fixture;
import spacegraph.util.math.Tuple2f;

public interface ParticleRaycastCallback {
    /**
     * Called for each particle found in the query. See
     * {@link RayCastCallback#reportFixture(Fixture, Tuple2f, Tuple2f, float)} for
     * argument info.
     *
     * @param index
     * @param point
     * @param normal
     * @param fraction
     * @return
     */
    float reportParticle(int index, Tuple2f point, Tuple2f normal, float fraction);

}
