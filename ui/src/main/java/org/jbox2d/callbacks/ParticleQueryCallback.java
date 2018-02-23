package org.jbox2d.callbacks;

import org.jbox2d.dynamics.Dynamics2D;

import java.util.function.Predicate;

/**
 * Callback class for AABB queries. See
 * {@link Dynamics2D#queryAABB(Predicate<org.jbox2d.dynamics.Fixture>, org.jbox2d.collision.AABB)}.
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
