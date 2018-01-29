package org.jbox2d.callbacks;

import spacegraph.math.Tuple2f;

public interface ParticleRaycastCallback {
    /**
     * Called for each particle found in the query. See
     * {@link RayCastCallback#reportFixture(org.jbox2d.dynamics.Fixture, Tuple2f, Tuple2f, float)} for
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
