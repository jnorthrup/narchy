/**
 * Copyright (c) 2013, Daniel Murphy
 * All rights reserved.
 * <p>
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * <p>
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
 * <p>
 * Created at 12:11:41 PM Jan 23, 2011
 */
/**
 * Created at 12:11:41 PM Jan 23, 2011
 */
package spacegraph.space2d.phys.dynamics.joints;

import jcog.math.v2;
import spacegraph.space2d.phys.common.Settings;
import spacegraph.space2d.phys.dynamics.Body2D;

/**
 * Pulley joint definition. This requires two ground anchors, two dynamic body anchor points, and a
 * pulley ratio.
 *
 * @author Daniel Murphy
 */
class PulleyJointDef extends JointDef {

    /**
     * The first ground anchor in world coordinates. This point never moves.
     */
    public v2 groundAnchorA;

    /**
     * The second ground anchor in world coordinates. This point never moves.
     */
    public v2 groundAnchorB;

    /**
     * The local anchor point relative to bodyA's origin.
     */
    public v2 localAnchorA;

    /**
     * The local anchor point relative to bodyB's origin.
     */
    public v2 localAnchorB;

    /**
     * The a reference length for the segment attached to bodyA.
     */
    public float lengthA;

    /**
     * The a reference length for the segment attached to bodyB.
     */
    public float lengthB;

    /**
     * The pulley ratio, used to simulate a block-and-tackle.
     */
    public float ratio;

    public PulleyJointDef() {
        super(JointType.PULLEY);
        groundAnchorA = new v2(-1.0f, 1.0f);
        groundAnchorB = new v2(1.0f, 1.0f);
        localAnchorA = new v2(-1.0f, 0.0f);
        localAnchorB = new v2(1.0f, 0.0f);
        lengthA = 0.0f;
        lengthB = 0.0f;
        ratio = 1.0f;
        collideConnected = true;
    }

    /**
     * Initialize the bodies, anchors, lengths, max lengths, and ratio using the world anchors.
     */
    public void initialize(Body2D b1, Body2D b2, v2 ga1, v2 ga2, v2 anchor1, v2 anchor2, float r) {
        bodyA = b1;
        bodyB = b2;
        groundAnchorA = ga1;
        groundAnchorB = ga2;
        localAnchorA = bodyA.getLocalPoint(anchor1);
        localAnchorB = bodyB.getLocalPoint(anchor2);
        v2 d1 = anchor1.subClone(ga1);
        lengthA = d1.length();
        v2 d2 = anchor2.subClone(ga2);
        lengthB = d2.length();
        ratio = r;
        assert (ratio > Settings.EPSILON);
    }
}
