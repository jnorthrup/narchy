/*
 * Java port of Bullet (c) 2008 Martin Dvorak <jezek2@advel.cz>
 *
 * Bullet Continuous Collision Detection and Physics Library
 * Ragdoll Demo
 * Copyright (c) 2007 Starbreeze Studios
 *
 * This software is provided 'as-is', without any express or implied warranty.
 * In no event will the authors be held liable for any damages arising from
 * the use of this software.
 *
 * Permission is granted to anyone to use this software for any purpose,
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 *
 * 1. The origin of this software must not be misrepresented; you must not
 *    claim that you wrote the original software. If you use this software
 *    in a product, an acknowledgment in the product documentation would be
 *    appreciated but is not required.
 * 2. Altered source versions must be plainly marked as such, and must not be
 *    misrepresented as being the original software.
 * 3. This notice may not be removed or altered from any source distribution.
 *
 * Written by: Marten Svanfeldt
 */

package spacegraph.space3d.widget;

import spacegraph.space3d.SpaceGraphPhys3D.ExtraGlobals;
import spacegraph.space3d.phys.Body3D;
import spacegraph.space3d.phys.Dynamics3D;
import spacegraph.space3d.phys.constraint.TypedConstraint;
import spacegraph.space3d.phys.constraint.generic.Generic6DofConstraint;
import spacegraph.space3d.phys.math.MatrixUtil;
import spacegraph.space3d.phys.math.Transform;
import spacegraph.space3d.phys.shape.CapsuleShape;
import spacegraph.space3d.phys.shape.CollisionShape;
import spacegraph.space3d.phys.util.BulletStack;
import jcog.math.v3;


/**
 * @author jezek2
 */
public class RagDoll  {

    private Body3D head;

    public RagDoll() {
        super();
    }






























    private final BulletStack stack = BulletStack.get();

    public enum BodyPart {
        BODYPART_PELVIS,
        BODYPART_SPINE,
        BODYPART_HEAD,

        BODYPART_LEFT_UPPER_LEG,
        BODYPART_LEFT_LOWER_LEG,

        BODYPART_RIGHT_UPPER_LEG,
        BODYPART_RIGHT_LOWER_LEG,

        BODYPART_LEFT_UPPER_ARM,
        BODYPART_LEFT_LOWER_ARM,

        BODYPART_RIGHT_UPPER_ARM,
        BODYPART_RIGHT_LOWER_ARM,

        BODYPART_COUNT
    }

    public enum JointType {
        JOINT_PELVIS_SPINE,
        JOINT_SPINE_HEAD,

        JOINT_LEFT_HIP,
        JOINT_LEFT_KNEE,

        JOINT_RIGHT_HIP,
        JOINT_RIGHT_KNEE,

        JOINT_LEFT_SHOULDER,
        JOINT_LEFT_ELBOW,

        JOINT_RIGHT_SHOULDER,
        JOINT_RIGHT_ELBOW,

        JOINT_COUNT
    }


    private final CollisionShape[] shapes = new CollisionShape[BodyPart.BODYPART_COUNT.ordinal()];
    private final Body3D[] bodies = new Body3D[BodyPart.BODYPART_COUNT.ordinal()];
    private final TypedConstraint[] joints = new TypedConstraint[JointType.JOINT_COUNT.ordinal()];


    public Body3D[] build(Dynamics3D world, v3 positionOffset, float scale_ragdoll) {


        stack.pushCommonMath();

        Transform tmpTrans = stack.transforms.get();

        
        shapes[BodyPart.BODYPART_PELVIS.ordinal()] = new CapsuleShape(scale_ragdoll * 0.15f, scale_ragdoll * 0.20f);
        shapes[BodyPart.BODYPART_SPINE.ordinal()] = new CapsuleShape(scale_ragdoll * 0.15f, scale_ragdoll * 0.28f);
        shapes[BodyPart.BODYPART_HEAD.ordinal()] = new CapsuleShape(scale_ragdoll * 0.10f, scale_ragdoll * 0.05f);
        shapes[BodyPart.BODYPART_LEFT_UPPER_LEG.ordinal()] = new CapsuleShape(scale_ragdoll * 0.07f, scale_ragdoll * 0.45f);
        shapes[BodyPart.BODYPART_LEFT_LOWER_LEG.ordinal()] = new CapsuleShape(scale_ragdoll * 0.05f, scale_ragdoll * 0.37f);
        shapes[BodyPart.BODYPART_RIGHT_UPPER_LEG.ordinal()] = new CapsuleShape(scale_ragdoll * 0.07f, scale_ragdoll * 0.45f);
        shapes[BodyPart.BODYPART_RIGHT_LOWER_LEG.ordinal()] = new CapsuleShape(scale_ragdoll * 0.05f, scale_ragdoll * 0.37f);
        shapes[BodyPart.BODYPART_LEFT_UPPER_ARM.ordinal()] = new CapsuleShape(scale_ragdoll * 0.05f, scale_ragdoll * 0.33f);
        shapes[BodyPart.BODYPART_LEFT_LOWER_ARM.ordinal()] = new CapsuleShape(scale_ragdoll * 0.04f, scale_ragdoll * 0.25f);
        shapes[BodyPart.BODYPART_RIGHT_UPPER_ARM.ordinal()] = new CapsuleShape(scale_ragdoll * 0.05f, scale_ragdoll * 0.33f);
        shapes[BodyPart.BODYPART_RIGHT_LOWER_ARM.ordinal()] = new CapsuleShape(scale_ragdoll * 0.04f, scale_ragdoll * 0.25f);

        
        Transform offset = stack.transforms.get();
        offset.setIdentity();
        offset.set(positionOffset);

        Transform transform = stack.transforms.get();
        transform.setIdentity();
        transform.set(0f, scale_ragdoll * 1f, 0f);
        tmpTrans.mul(offset, transform);
        bodies[BodyPart.BODYPART_PELVIS.ordinal()] = localCreateRigidBody(1f, tmpTrans, shapes[BodyPart.BODYPART_PELVIS.ordinal()]);

        transform.setIdentity();
        transform.set(0f, scale_ragdoll * 1.2f, 0f);
        tmpTrans.mul(offset, transform);
        bodies[BodyPart.BODYPART_SPINE.ordinal()] = localCreateRigidBody(1f, tmpTrans, shapes[BodyPart.BODYPART_SPINE.ordinal()]);

        transform.setIdentity();
        transform.set(0f, scale_ragdoll * 1.6f, 0f);
        tmpTrans.mul(offset, transform);
        this.head = bodies[BodyPart.BODYPART_HEAD.ordinal()] =
                localCreateRigidBody(1f, tmpTrans, shapes[BodyPart.BODYPART_HEAD.ordinal()]);






















        transform.setIdentity();
        transform.set(-0.18f * scale_ragdoll, 0.65f * scale_ragdoll, 0f);
        tmpTrans.mul(offset, transform);
        bodies[BodyPart.BODYPART_LEFT_UPPER_LEG.ordinal()] = localCreateRigidBody(1f, tmpTrans, shapes[BodyPart.BODYPART_LEFT_UPPER_LEG.ordinal()]);

        transform.setIdentity();
        transform.set(-0.18f * scale_ragdoll, 0.2f * scale_ragdoll, 0f);
        tmpTrans.mul(offset, transform);
        bodies[BodyPart.BODYPART_LEFT_LOWER_LEG.ordinal()] = localCreateRigidBody(1f, tmpTrans, shapes[BodyPart.BODYPART_LEFT_LOWER_LEG.ordinal()]);

        transform.setIdentity();
        transform.set(0.18f * scale_ragdoll, 0.65f * scale_ragdoll, 0f);
        tmpTrans.mul(offset, transform);
        bodies[BodyPart.BODYPART_RIGHT_UPPER_LEG.ordinal()] = localCreateRigidBody(1f, tmpTrans, shapes[BodyPart.BODYPART_RIGHT_UPPER_LEG.ordinal()]);

        transform.setIdentity();
        transform.set(0.18f * scale_ragdoll, 0.2f * scale_ragdoll, 0f);
        tmpTrans.mul(offset, transform);
        bodies[BodyPart.BODYPART_RIGHT_LOWER_LEG.ordinal()] = localCreateRigidBody(1f, tmpTrans, shapes[BodyPart.BODYPART_RIGHT_LOWER_LEG.ordinal()]);

        transform.setIdentity();
        transform.set(-0.35f * scale_ragdoll, 1.45f * scale_ragdoll, 0f);
        MatrixUtil.setEulerZYX(transform.basis, 0, 0, ExtraGlobals.SIMD_HALF_PI);
        tmpTrans.mul(offset, transform);
        bodies[BodyPart.BODYPART_LEFT_UPPER_ARM.ordinal()] = localCreateRigidBody(1f, tmpTrans, shapes[BodyPart.BODYPART_LEFT_UPPER_ARM.ordinal()]);

        transform.setIdentity();
        transform.set(-0.7f * scale_ragdoll, 1.45f * scale_ragdoll, 0f);
        MatrixUtil.setEulerZYX(transform.basis, 0, 0, ExtraGlobals.SIMD_HALF_PI);
        tmpTrans.mul(offset, transform);
        bodies[BodyPart.BODYPART_LEFT_LOWER_ARM.ordinal()] = localCreateRigidBody(1f, tmpTrans, shapes[BodyPart.BODYPART_LEFT_LOWER_ARM.ordinal()]);

        transform.setIdentity();
        transform.set(0.35f * scale_ragdoll, 1.45f * scale_ragdoll, 0f);
        MatrixUtil.setEulerZYX(transform.basis, 0, 0, -ExtraGlobals.SIMD_HALF_PI);
        tmpTrans.mul(offset, transform);
        bodies[BodyPart.BODYPART_RIGHT_UPPER_ARM.ordinal()] = localCreateRigidBody(1f, tmpTrans, shapes[BodyPart.BODYPART_RIGHT_UPPER_ARM.ordinal()]);

        transform.setIdentity();
        transform.set(0.7f * scale_ragdoll, 1.45f * scale_ragdoll, 0f);
        MatrixUtil.setEulerZYX(transform.basis, 0, 0, -ExtraGlobals.SIMD_HALF_PI);
        tmpTrans.mul(offset, transform);
        bodies[BodyPart.BODYPART_RIGHT_LOWER_ARM.ordinal()] = localCreateRigidBody(1f, tmpTrans, shapes[BodyPart.BODYPART_RIGHT_LOWER_ARM.ordinal()]);

        






        
        
        Transform localA = stack.transforms.get(), localB = stack.transforms.get();
        
        localA.setIdentity();
        localB.setIdentity();

        localA.set(0f, 0.30f * scale_ragdoll, 0f);

        localB.set(0f, -0.14f * scale_ragdoll, 0f);

        boolean useLinearReferenceFrameA = true;
        Generic6DofConstraint joint6DOF = new Generic6DofConstraint(bodies[BodyPart.BODYPART_SPINE.ordinal()], bodies[BodyPart.BODYPART_HEAD.ordinal()], localA, localB, useLinearReferenceFrameA);

        
        
        
        
        joint6DOF.setAngularLowerLimit(stack.vectors.get(-ExtraGlobals.SIMD_PI * 0.3f, -ExtraGlobals.FLT_EPSILON, -ExtraGlobals.SIMD_PI * 0.3f));
        joint6DOF.setAngularUpperLimit(stack.vectors.get(ExtraGlobals.SIMD_PI * 0.5f, ExtraGlobals.FLT_EPSILON, ExtraGlobals.SIMD_PI * 0.3f));
        
        joints[JointType.JOINT_SPINE_HEAD.ordinal()] = joint6DOF;
        world.addConstraint(joints[JointType.JOINT_SPINE_HEAD.ordinal()], true);
        

        
        localA.setIdentity();
        localB.setIdentity();

        localA.set(-0.2f * scale_ragdoll, 0.15f * scale_ragdoll, 0f);

        MatrixUtil.setEulerZYX(localB.basis, ExtraGlobals.SIMD_HALF_PI, 0, -ExtraGlobals.SIMD_HALF_PI);
        localB.set(0f, -0.18f * scale_ragdoll, 0f);

        joint6DOF = new Generic6DofConstraint(bodies[BodyPart.BODYPART_SPINE.ordinal()], bodies[BodyPart.BODYPART_LEFT_UPPER_ARM.ordinal()], localA, localB, useLinearReferenceFrameA);

        
        
        
        
        joint6DOF.setAngularLowerLimit(stack.vectors.get(-ExtraGlobals.SIMD_PI * 0.8f, -ExtraGlobals.FLT_EPSILON, -ExtraGlobals.SIMD_PI * 0.5f));
        joint6DOF.setAngularUpperLimit(stack.vectors.get(ExtraGlobals.SIMD_PI * 0.8f, ExtraGlobals.FLT_EPSILON, ExtraGlobals.SIMD_PI * 0.5f));
        
        joints[JointType.JOINT_LEFT_SHOULDER.ordinal()] = joint6DOF;
        world.addConstraint(joints[JointType.JOINT_LEFT_SHOULDER.ordinal()], true);
        

        
        localA.setIdentity();
        localB.setIdentity();

        localA.set(0.2f * scale_ragdoll, 0.15f * scale_ragdoll, 0f);
        MatrixUtil.setEulerZYX(localB.basis, 0f, 0f, ExtraGlobals.SIMD_HALF_PI);
        localB.set(0f, -0.18f * scale_ragdoll, 0f);
        joint6DOF = new Generic6DofConstraint(bodies[BodyPart.BODYPART_SPINE.ordinal()], bodies[BodyPart.BODYPART_RIGHT_UPPER_ARM.ordinal()], localA, localB, useLinearReferenceFrameA);

        
        
        
        
        joint6DOF.setAngularLowerLimit(stack.vectors.get(-ExtraGlobals.SIMD_PI * 0.8f, -ExtraGlobals.SIMD_EPSILON, -ExtraGlobals.SIMD_PI * 0.5f));
        joint6DOF.setAngularUpperLimit(stack.vectors.get(ExtraGlobals.SIMD_PI * 0.8f, ExtraGlobals.SIMD_EPSILON, ExtraGlobals.SIMD_PI * 0.5f));
        
        joints[JointType.JOINT_RIGHT_SHOULDER.ordinal()] = joint6DOF;
        world.addConstraint(joints[JointType.JOINT_RIGHT_SHOULDER.ordinal()], true);
        

        
        localA.setIdentity();
        localB.setIdentity();

        localA.set(0f, 0.18f * scale_ragdoll, 0f);
        localB.set(0f, -0.14f * scale_ragdoll, 0f);
        joint6DOF = new Generic6DofConstraint(bodies[BodyPart.BODYPART_LEFT_UPPER_ARM.ordinal()], bodies[BodyPart.BODYPART_LEFT_LOWER_ARM.ordinal()], localA, localB, useLinearReferenceFrameA);

        
        
        
        
        joint6DOF.setAngularLowerLimit(stack.vectors.get(-ExtraGlobals.SIMD_EPSILON, -ExtraGlobals.SIMD_EPSILON, -ExtraGlobals.SIMD_EPSILON));
        joint6DOF.setAngularUpperLimit(stack.vectors.get(ExtraGlobals.SIMD_PI * 0.7f, ExtraGlobals.SIMD_EPSILON, ExtraGlobals.SIMD_EPSILON));
        
        joints[JointType.JOINT_LEFT_ELBOW.ordinal()] = joint6DOF;
        world.addConstraint(joints[JointType.JOINT_LEFT_ELBOW.ordinal()], true);
        

        
        localA.setIdentity();
        localB.setIdentity();

        localA.set(0f, 0.18f * scale_ragdoll, 0f);
        localB.set(0f, -0.14f * scale_ragdoll, 0f);
        joint6DOF = new Generic6DofConstraint(bodies[BodyPart.BODYPART_RIGHT_UPPER_ARM.ordinal()], bodies[BodyPart.BODYPART_RIGHT_LOWER_ARM.ordinal()], localA, localB, useLinearReferenceFrameA);

        
        
        
        
        joint6DOF.setAngularLowerLimit(stack.vectors.get(-ExtraGlobals.SIMD_EPSILON, -ExtraGlobals.SIMD_EPSILON, -ExtraGlobals.SIMD_EPSILON));
        joint6DOF.setAngularUpperLimit(stack.vectors.get(ExtraGlobals.SIMD_PI * 0.7f, ExtraGlobals.SIMD_EPSILON, ExtraGlobals.SIMD_EPSILON));
        

        joints[JointType.JOINT_RIGHT_ELBOW.ordinal()] = joint6DOF;
        world.addConstraint(joints[JointType.JOINT_RIGHT_ELBOW.ordinal()], true);
        


        
        localA.setIdentity();
        localB.setIdentity();

        MatrixUtil.setEulerZYX(localA.basis, 0, ExtraGlobals.SIMD_HALF_PI, 0);
        localA.set(0f, 0.15f * scale_ragdoll, 0f);
        MatrixUtil.setEulerZYX(localB.basis, 0, ExtraGlobals.SIMD_HALF_PI, 0);
        localB.set(0f, -0.15f * scale_ragdoll, 0f);
        joint6DOF = new Generic6DofConstraint(bodies[BodyPart.BODYPART_PELVIS.ordinal()], bodies[BodyPart.BODYPART_SPINE.ordinal()], localA, localB, useLinearReferenceFrameA);

        
        
        
        
        joint6DOF.setAngularLowerLimit(stack.vectors.get(-ExtraGlobals.SIMD_PI * 0.2f, -ExtraGlobals.SIMD_EPSILON, -ExtraGlobals.SIMD_PI * 0.3f));
        joint6DOF.setAngularUpperLimit(stack.vectors.get(ExtraGlobals.SIMD_PI * 0.2f, ExtraGlobals.SIMD_EPSILON, ExtraGlobals.SIMD_PI * 0.6f));
        
        joints[JointType.JOINT_PELVIS_SPINE.ordinal()] = joint6DOF;
        world.addConstraint(joints[JointType.JOINT_PELVIS_SPINE.ordinal()], true);
        

        
        localA.setIdentity();
        localB.setIdentity();

        localA.set(-0.18f * scale_ragdoll, -0.10f * scale_ragdoll, 0f);

        localB.set(0f, 0.225f * scale_ragdoll, 0f);

        joint6DOF = new Generic6DofConstraint(bodies[BodyPart.BODYPART_PELVIS.ordinal()], bodies[BodyPart.BODYPART_LEFT_UPPER_LEG.ordinal()], localA, localB, useLinearReferenceFrameA);

        
        
        
        
        joint6DOF.setAngularLowerLimit(stack.vectors.get(-ExtraGlobals.SIMD_HALF_PI * 0.5f, -ExtraGlobals.SIMD_EPSILON, -ExtraGlobals.SIMD_EPSILON));
        joint6DOF.setAngularUpperLimit(stack.vectors.get(ExtraGlobals.SIMD_HALF_PI * 0.8f, ExtraGlobals.SIMD_EPSILON, ExtraGlobals.SIMD_HALF_PI * 0.6f));
        
        joints[JointType.JOINT_LEFT_HIP.ordinal()] = joint6DOF;
        world.addConstraint(joints[JointType.JOINT_LEFT_HIP.ordinal()], true);
        


        
        localA.setIdentity();
        localB.setIdentity();

        localA.set(0.18f * scale_ragdoll, -0.10f * scale_ragdoll, 0f);
        localB.set(0f, 0.225f * scale_ragdoll, 0f);

        joint6DOF = new Generic6DofConstraint(bodies[BodyPart.BODYPART_PELVIS.ordinal()], bodies[BodyPart.BODYPART_RIGHT_UPPER_LEG.ordinal()], localA, localB, useLinearReferenceFrameA);

        
        
        
        
        joint6DOF.setAngularLowerLimit(stack.vectors.get(-ExtraGlobals.SIMD_HALF_PI * 0.5f, -ExtraGlobals.SIMD_EPSILON, -ExtraGlobals.SIMD_HALF_PI * 0.6f));
        joint6DOF.setAngularUpperLimit(stack.vectors.get(ExtraGlobals.SIMD_HALF_PI * 0.8f, ExtraGlobals.SIMD_EPSILON, ExtraGlobals.SIMD_EPSILON));
        
        joints[JointType.JOINT_RIGHT_HIP.ordinal()] = joint6DOF;
        world.addConstraint(joints[JointType.JOINT_RIGHT_HIP.ordinal()], true);
        


        
        localA.setIdentity();
        localB.setIdentity();

        localA.set(0f, -0.225f * scale_ragdoll, 0f);
        localB.set(0f, 0.185f * scale_ragdoll, 0f);
        joint6DOF = new Generic6DofConstraint(bodies[BodyPart.BODYPART_LEFT_UPPER_LEG.ordinal()], bodies[BodyPart.BODYPART_LEFT_LOWER_LEG.ordinal()], localA, localB, useLinearReferenceFrameA);
        
        
        
        
        
        joint6DOF.setAngularLowerLimit(stack.vectors.get(-ExtraGlobals.SIMD_EPSILON, -ExtraGlobals.SIMD_EPSILON, -ExtraGlobals.SIMD_EPSILON));
        joint6DOF.setAngularUpperLimit(stack.vectors.get(ExtraGlobals.SIMD_PI * 0.7f, ExtraGlobals.SIMD_EPSILON, ExtraGlobals.SIMD_EPSILON));
        
        joints[JointType.JOINT_LEFT_KNEE.ordinal()] = joint6DOF;
        world.addConstraint(joints[JointType.JOINT_LEFT_KNEE.ordinal()], true);
        

        
        localA.setIdentity();
        localB.setIdentity();

        localA.set(0f, -0.225f * scale_ragdoll, 0f);
        localB.set(0f, 0.185f * scale_ragdoll, 0f);
        joint6DOF = new Generic6DofConstraint(bodies[BodyPart.BODYPART_RIGHT_UPPER_LEG.ordinal()], bodies[BodyPart.BODYPART_RIGHT_LOWER_LEG.ordinal()], localA, localB, useLinearReferenceFrameA);

        
        
        
        
        joint6DOF.setAngularLowerLimit(stack.vectors.get(-ExtraGlobals.SIMD_EPSILON, -ExtraGlobals.SIMD_EPSILON, -ExtraGlobals.SIMD_EPSILON));
        joint6DOF.setAngularUpperLimit(stack.vectors.get(ExtraGlobals.SIMD_PI * 0.7f, ExtraGlobals.SIMD_EPSILON, ExtraGlobals.SIMD_EPSILON));
        
        joints[JointType.JOINT_RIGHT_KNEE.ordinal()] = joint6DOF;
        world.addConstraint(joints[JointType.JOINT_RIGHT_KNEE.ordinal()], true);
        

        stack.popCommonMath();

        return bodies;
    }




















    private static Body3D localCreateRigidBody(float mass, Transform startTransform, CollisionShape shape) {
        
        

            Body3D body = Dynamics3D.newBody(mass, shape, startTransform, +1, -1);

            body.setCenterOfMassTransform(startTransform);

            

            return body;
        
          
        
    }


}
