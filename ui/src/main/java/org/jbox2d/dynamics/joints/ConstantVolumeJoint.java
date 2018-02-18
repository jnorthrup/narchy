/*******************************************************************************
 * Copyright (c) 2013, Daniel Murphy
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 	* Redistributions of source code must retain the above copyright notice,
 * 	  this list of conditions and the following disclaimer.
 * 	* Redistributions in binary form must reproduce the above copyright notice,
 * 	  this list of conditions and the following disclaimer in the documentation
 * 	  and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package org.jbox2d.dynamics.joints;

import org.jbox2d.common.Settings;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body2D;
import org.jbox2d.dynamics.Dynamics2D;
import org.jbox2d.dynamics.SolverData;
import org.jbox2d.dynamics.contacts.Position;
import org.jbox2d.dynamics.contacts.Velocity;
import spacegraph.math.Tuple2f;

public class ConstantVolumeJoint extends Joint {

    private final Body2D[] bodies;
    private final float[] targetLengths;
    private float targetVolume;

    private final Tuple2f[] normals;
    private float m_impulse = 0.0f;

    private final Dynamics2D world;

    private final DistanceJoint[] distanceJoints;

    public Body2D[] getBodies() {
        return bodies;
    }

    public DistanceJoint[] getJoints() {
        return distanceJoints;
    }

    public void inflate(float factor) {
        targetVolume *= factor;
    }

    public ConstantVolumeJoint(Dynamics2D argWorld, ConstantVolumeJointDef def) {
        super(argWorld.pool, def);
        world = argWorld;
        int n = def.bodies.size();
        if (n <= 2) {
            throw new IllegalArgumentException(
                    "You cannot create a constant volume joint with less than three bodies.");
        }
        bodies = def.bodies.toArray(new Body2D[n]);

        targetLengths = new float[n];
        for (int i = 0; i < targetLengths.length; ++i) {
            final int next = (i == targetLengths.length - 1) ? 0 : i + 1;
            float dist = bodies[i].getWorldCenter().sub(bodies[next].getWorldCenter()).length();
            targetLengths[i] = dist;
        }
        targetVolume = getBodyArea();

        if (def.joints != null && def.joints.size() != n) {
            throw new IllegalArgumentException(
                    "Incorrect joint definition.  Joints have to correspond to the bodies");
        }
        if (def.joints == null) {
            final DistanceJointDef djd = new DistanceJointDef();
            distanceJoints = new DistanceJoint[bodies.length];
            for (int i = 0; i < targetLengths.length; ++i) {
                final int next = (i == targetLengths.length - 1) ? 0 : i + 1;
                djd.frequencyHz = def.frequencyHz;// 20.0f;
                djd.dampingRatio = def.dampingRatio;// 50.0f;
                djd.collideConnected = def.collideConnected;
                djd.initialize(bodies[i], bodies[next], bodies[i].getWorldCenter(),
                        bodies[next].getWorldCenter());
                distanceJoints[i] = (DistanceJoint) world.newJoint(djd);
            }
        } else {
            distanceJoints = def.joints.toArray(new DistanceJoint[def.joints.size()]);
        }

        normals = new Tuple2f[bodies.length];
        for (int i = 0; i < normals.length; ++i) {
            normals[i] = new Vec2();
        }
    }

    @Override
    public void destructor() {
        for (DistanceJoint distanceJoint : distanceJoints) {
            world.destroyJoint(distanceJoint);
        }
    }

    private float getBodyArea() {
        float area = 0.0f;
        for (int i = 0; i < bodies.length; ++i) {
            final int next = (i == bodies.length - 1) ? 0 : i + 1;
            area +=
                    bodies[i].getWorldCenter().x * bodies[next].getWorldCenter().y
                            - bodies[next].getWorldCenter().x * bodies[i].getWorldCenter().y;
        }
        area *= .5f;
        return area;
    }

    private float getSolverArea(Position[] positions) {
        float area = 0.0f;
        for (int i = 0; i < bodies.length; ++i) {
            final int next = (i == bodies.length - 1) ? 0 : i + 1;
            area +=
                    positions[bodies[i].island].c.x * positions[bodies[next].island].c.y
                            - positions[bodies[next].island].c.x * positions[bodies[i].island].c.y;
        }
        area *= .5f;
        return area;
    }

    private boolean constrainEdges(Position[] positions) {
        float perimeter = 0.0f;
        for (int i = 0; i < bodies.length; ++i) {
            final int next = (i == bodies.length - 1) ? 0 : i + 1;
            float dx = positions[bodies[next].island].c.x - positions[bodies[i].island].c.x;
            float dy = positions[bodies[next].island].c.y - positions[bodies[i].island].c.y;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            if (dist < Settings.EPSILON) {
                dist = 1.0f;
            }
            normals[i].x = dy / dist;
            normals[i].y = -dx / dist;
            perimeter += dist;
        }

        final Tuple2f delta = pool.popVec2();

        float deltaArea = targetVolume - getSolverArea(positions);
        float toExtrude = 0.5f * deltaArea / perimeter; // *relaxationFactor
        // float sumdeltax = 0.0f;
        boolean done = true;
        for (int i = 0; i < bodies.length; ++i) {
            final int next = (i == bodies.length - 1) ? 0 : i + 1;
            delta.set(toExtrude * (normals[i].x + normals[next].x), toExtrude
                    * (normals[i].y + normals[next].y));
            // sumdeltax += dx;
            float normSqrd = delta.lengthSquared();
            if (normSqrd > Settings.maxLinearCorrection * Settings.maxLinearCorrection) {
                delta.scaled(Settings.maxLinearCorrection / (float) Math.sqrt(normSqrd));
            }
            if (normSqrd > Settings.linearSlop * Settings.linearSlop) {
                done = false;
            }
            positions[bodies[next].island].c.x += delta.x;
            positions[bodies[next].island].c.y += delta.y;
            // bodies[next].m_linearVelocity.x += delta.x * step.inv_dt;
            // bodies[next].m_linearVelocity.y += delta.y * step.inv_dt;
        }

        pool.pushVec2(1);
        // System.out.println(sumdeltax);
        return done;
    }

    @Override
    public void initVelocityConstraints(final SolverData step) {
        Velocity[] velocities = step.velocities;
        Position[] positions = step.positions;
        final Tuple2f[] d = pool.getVec2Array(bodies.length);

        for (int i = 0; i < bodies.length; ++i) {
            final int prev = (i == 0) ? bodies.length - 1 : i - 1;
            final int next = (i == bodies.length - 1) ? 0 : i + 1;
            d[i].set(positions[bodies[next].island].c);
            d[i].subbed(positions[bodies[prev].island].c);
        }

        if (step.step.warmStarting) {
            m_impulse *= step.step.dtRatio;
            // float lambda = -2.0f * crossMassSum / dotMassSum;
            // System.out.println(crossMassSum + " " +dotMassSum);
            // lambda = MathUtils.clamp(lambda, -Settings.maxLinearCorrection,
            // Settings.maxLinearCorrection);
            // m_impulse = lambda;
            for (int i = 0; i < bodies.length; ++i) {
                velocities[bodies[i].island].v.x += bodies[i].m_invMass * d[i].y * .5f * m_impulse;
                velocities[bodies[i].island].v.y += bodies[i].m_invMass * -d[i].x * .5f * m_impulse;
            }
        } else {
            m_impulse = 0.0f;
        }
    }

    @Override
    public boolean solvePositionConstraints(SolverData step) {
        return constrainEdges(step.positions);
    }

    @Override
    public void solveVelocityConstraints(final SolverData step) {
        float crossMassSum = 0.0f;
        float dotMassSum = 0.0f;

        Velocity[] velocities = step.velocities;
        Position[] positions = step.positions;
        final Tuple2f d[] = pool.getVec2Array(bodies.length);

        for (int i = 0; i < bodies.length; ++i) {
            final int prev = (i == 0) ? bodies.length - 1 : i - 1;
            final int next = (i == bodies.length - 1) ? 0 : i + 1;
            d[i].set(positions[bodies[next].island].c);
            d[i].subbed(positions[bodies[prev].island].c);
            dotMassSum += (d[i].lengthSquared()) / bodies[i].getMass();
            crossMassSum += Tuple2f.cross(velocities[bodies[i].island].v, d[i]);
        }
        float lambda = -2.0f * crossMassSum / dotMassSum;
        // System.out.println(crossMassSum + " " +dotMassSum);
        // lambda = MathUtils.clamp(lambda, -Settings.maxLinearCorrection,
        // Settings.maxLinearCorrection);
        m_impulse += lambda;
        // System.out.println(m_impulse);
        for (int i = 0; i < bodies.length; ++i) {
            velocities[bodies[i].island].v.x += bodies[i].m_invMass * d[i].y * .5f * lambda;
            velocities[bodies[i].island].v.y += bodies[i].m_invMass * -d[i].x * .5f * lambda;
        }
    }

    /**
     * No-op
     */
    @Override
    public void getAnchorA(Tuple2f argOut) {
    }

    /**
     * No-op
     */
    @Override
    public void getAnchorB(Tuple2f argOut) {
    }

    /**
     * No-op
     */
    @Override
    public void getReactionForce(float inv_dt, Tuple2f argOut) {
    }

    /**
     * No-op
     */
    @Override
    public float getReactionTorque(float inv_dt) {
        return 0;
    }
}
