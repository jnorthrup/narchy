package org.jbox2d.callbacks;

import org.jbox2d.dynamics.Dynamics2D;

/**
 * Callback class for AABB queries. See
 * {@link Dynamics2D#queryAABB(QueryCallback, org.jbox2d.collision.AABB)}.
 *
 * @author dmurph
 */
public interface ParticleQueryCallback {
    /**
     * Called for each particle found in the query AABB.
     *
     * @return false to terminate the query.
     */
    boolean reportParticle(int index);
}
