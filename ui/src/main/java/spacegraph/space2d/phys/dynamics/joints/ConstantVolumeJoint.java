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
package spacegraph.space2d.phys.dynamics.joints;

import jcog.math.v2;
import spacegraph.space2d.phys.common.Settings;
import spacegraph.space2d.phys.dynamics.Body2D;
import spacegraph.space2d.phys.dynamics.Dynamics2D;
import spacegraph.space2d.phys.dynamics.SolverData;

public class ConstantVolumeJoint extends Joint {

    private final Body2D[] bodies;
    private float targetVolume;

    private final v2[] normals;
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
        var n = def.bodies.size();
        if (n <= 2) {
            throw new IllegalArgumentException(
                    "You cannot create a constant volume joint with less than three bodies.");
        }
        bodies = def.bodies.toArray(new Body2D[n]);

        var targetLengths = new float[n];
        for (var i = 0; i < targetLengths.length; ++i) {
            var next = (i == targetLengths.length - 1) ? 0 : i + 1;
            var dist = bodies[i].getWorldCenter().subClone(bodies[next].getWorldCenter()).length();
            targetLengths[i] = dist;
        }
        targetVolume = getBodyArea();

        if (def.joints != null && def.joints.size() != n) {
            throw new IllegalArgumentException(
                    "Incorrect joint definition.  Joints have to correspond to the bodies");
        }
        if (def.joints == null) {
            var djd = new DistanceJointDef();
            distanceJoints = new DistanceJoint[bodies.length];
            for (var i = 0; i < targetLengths.length; ++i) {
                var next = (i == targetLengths.length - 1) ? 0 : i + 1;
                djd.frequencyHz = def.frequencyHz;
                djd.dampingRatio = def.dampingRatio;
                djd.collideConnected = def.collideConnected;
                djd.initialize(bodies[i], bodies[next], bodies[i].getWorldCenter(),
                        bodies[next].getWorldCenter());
                distanceJoints[i] = (DistanceJoint) world.addJoint(djd);
            }
        } else {
            distanceJoints = def.joints.toArray(new DistanceJoint[0]);
        }

        normals = new v2[bodies.length];
        for (var i = 0; i < normals.length; ++i) {
            normals[i] = new v2();
        }
    }

    @Override
    public void destructor() {
        for (var distanceJoint : distanceJoints) {
            world.removeJoint(distanceJoint);
        }
    }

    private float getBodyArea() {
        var area = 0.0f;
        for (var i = 0; i < bodies.length; ++i) {
            var next = (i == bodies.length - 1) ? 0 : i + 1;
            var ic = bodies[i].getWorldCenter();
            var nc = bodies[next].getWorldCenter();
            area += ic.x * nc.y - nc.x * ic.y;
        }
        area /= 2;
        return area;
    }

    private float getSolverArea(v2[] positions) {
        var area = 0.0f;
        for (var i = 0; i < bodies.length; ++i) {
            var next = (i == bodies.length - 1) ? 0 : i + 1;
            var pi = positions[bodies[i].island];
            var pn = positions[bodies[next].island];
            area += pi.x * pn.y - pn.x * pi.y;
        }
        area /= 2;
        return area;
    }

    private boolean constrainEdges(v2[] positions) {
        var perimeter = 0.0f;
        for (var i = 0; i < bodies.length; ++i) {
            var next = (i == bodies.length - 1) ? 0 : i + 1;
            var pn = positions[bodies[next].island];
            var pi = positions[bodies[i].island];
            var dx = pn.x - pi.x;
            var dy = pn.y - pi.y;
            var distSq = (dx * dx + dy * dy);
            var ni = normals[i];
            if (distSq < Settings.EPSILONsqr) {
                ni.setZero();
            } else {
                var dist = (float) Math.sqrt(distSq);

                ni.x = dy / dist;
                ni.y = -dx / dist;
                perimeter += dist;
            }

        }

        var delta = pool.popVec2();

        var deltaArea = targetVolume - getSolverArea(positions);
        var toExtrude = 0.5f * deltaArea / perimeter;

        var done = true;
        for (var i = 0; i < bodies.length; ++i) {
            var next = (i == bodies.length - 1) ? 0 : i + 1;
            delta.set(toExtrude * (normals[i].x + normals[next].x), toExtrude
                    * (normals[i].y + normals[next].y));

            var normSqrd = delta.lengthSquared();
            if (normSqrd > Settings.maxLinearCorrection * Settings.maxLinearCorrection) {
                delta.scaled(Settings.maxLinearCorrection / (float) Math.sqrt(normSqrd));
            }
            if (normSqrd > Settings.linearSlop * Settings.linearSlop) {
                done = false;
            }
            positions[bodies[next].island].x += delta.x;
            positions[bodies[next].island].y += delta.y;


        }

        pool.pushVec2(1);

        return done;
    }

    @Override
    public void initVelocityConstraints(SolverData step) {
        v2[] velocities = step.velocities;
        v2[] positions = step.positions;
        var d = pool.getVec2Array(bodies.length);

        for (var i = 0; i < bodies.length; ++i) {
            var prev = (i == 0) ? bodies.length - 1 : i - 1;
            var next = (i == bodies.length - 1) ? 0 : i + 1;
            d[i].set(positions[bodies[next].island]);
            d[i].subbed(positions[bodies[prev].island]);
        }

        if (step.step.warmStarting) {
            m_impulse *= step.step.dtRatio;


            for (var i = 0; i < bodies.length; ++i) {
                velocities[bodies[i].island].x += bodies[i].m_invMass * d[i].y * .5f * m_impulse;
                velocities[bodies[i].island].y += bodies[i].m_invMass * -d[i].x * .5f * m_impulse;
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
    public void solveVelocityConstraints(SolverData step) {
        var crossMassSum = 0.0f;
        var dotMassSum = 0.0f;

        v2[] velocities = step.velocities;
        v2[] positions = step.positions;
        var d = pool.getVec2Array(bodies.length);

        for (var i = 0; i < bodies.length; ++i) {
            var prev = (i == 0) ? bodies.length - 1 : i - 1;
            var next = (i == bodies.length - 1) ? 0 : i + 1;
            d[i].set(positions[bodies[next].island]);
            d[i].subbed(positions[bodies[prev].island]);
            dotMassSum += (d[i].lengthSquared()) / bodies[i].getMass();
            crossMassSum += v2.cross(velocities[bodies[i].island], d[i]);
        }
        var lambda = -2.0f * crossMassSum / dotMassSum;


        m_impulse += lambda;

        for (var i = 0; i < bodies.length; ++i) {
            velocities[bodies[i].island].x += bodies[i].m_invMass * d[i].y * .5f * lambda;
            velocities[bodies[i].island].y += bodies[i].m_invMass * -d[i].x * .5f * lambda;
        }
    }

    /**
     * No-op
     */
    @Override
    public void getAnchorA(v2 argOut) {
    }

    /**
     * No-op
     */
    @Override
    public void getAnchorB(v2 argOut) {
    }

    /**
     * No-op
     */
    @Override
    public void getReactionForce(float inv_dt, v2 argOut) {
    }

    /**
     * No-op
     */
    @Override
    public float getReactionTorque(float inv_dt) {
        return 0;
    }
}
