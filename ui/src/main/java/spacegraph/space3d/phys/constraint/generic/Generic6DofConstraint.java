/*
 * Java port of Bullet (c) 2008 Martin Dvorak <jezek2@advel.cz>
 *
 * Bullet Continuous Collision Detection and Physics Library
 * Copyright (c) 2003-2008 Erwin Coumans  http:
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
 */

/*
2007-09-09
btGeneric6DofConstraint Refactored by Francisco Le�n
email: projectileman@yahoo.com
http:
*/

package spacegraph.space3d.phys.constraint.generic;

import jcog.math.v3;
import spacegraph.space3d.phys.Body3D;
import spacegraph.space3d.phys.BulletGlobals;
import spacegraph.space3d.phys.constraint.TypedConstraint;
import spacegraph.space3d.phys.math.MatrixUtil;
import spacegraph.space3d.phys.math.Transform;
import spacegraph.space3d.phys.math.VectorUtil;
import spacegraph.space3d.phys.solve.JacobianEntry;
import spacegraph.util.math.Matrix3f;


/*!

*/

/**
 * Generic6DofConstraint between two rigid bodies each with a pivot point that describes
 * the axis location in local space.<p>
 * 
 * Generic6DofConstraint can leave any of the 6 degree of freedom "free" or "locked".
 * Currently this limit supports rotational motors.<br>
 * 
 * <ul>
 * <li>For linear limits, use {@link #setLinearUpperLimit}, {@link #setLinearLowerLimit}.
 * You can set the parameters with the {@link TranslationalLimitMotor} structure accessible
 * through the {@link #getTranslationalLimitMotor} method. </li>
 *
 * <li>For angular limits, use the {@link RotationalLimitMotor} structure for configuring
 * the limit. This is accessible through {@link #getRotationalLimitMotor} method,
 * this brings support for limit parameters and motors.</li>
 *
 * <li>Angulars limits have these possible ranges:
 * <table border="1">
 * <tr>
 * 	<td><b>AXIS</b></td>
 * 	<td><b>MIN ANGLE</b></td>
 * 	<td><b>MAX ANGLE</b></td>
 * </tr><tr>
 * 	<td>X</td>
 * 		<td>-PI</td>
 * 		<td>PI</td>
 * </tr><tr>
 * 	<td>Y</td>
 * 		<td>-PI/2</td>
 * 		<td>PI/2</td>
 * </tr><tr>
 * 	<td>Z</td>
 * 		<td>-PI/2</td>
 * 		<td>PI/2</td>
 * </tr>
 * </table>
 * </li>
 * </ul>
 *
 * @author jezek2
 */
public class Generic6DofConstraint extends TypedConstraint {

    private final Transform frameInA = new Transform();
    private final Transform frameInB = new Transform();
    private final JacobianEntry[] jacLinear/*[3]*/ = {new JacobianEntry(), new JacobianEntry(), new JacobianEntry()};
    private final JacobianEntry[] jacAng/*[3]*/ = {new JacobianEntry(), new JacobianEntry(), new JacobianEntry()};
    private final TranslationalLimitMotor linearLimits = new TranslationalLimitMotor();
    private final RotationalLimitMotor[] angularLimits/*[3]*/ = {new RotationalLimitMotor(), new RotationalLimitMotor(), new RotationalLimitMotor()};
    private float timeStep;
    private final Transform calculatedTransformA = new Transform();
    private final Transform calculatedTransformB = new Transform();
    private final v3 calculatedAxisAngleDiff = new v3();
    private final v3[] calculatedAxis/*[3]*/ = {new v3(), new v3(), new v3()};
    private final v3 anchorPos = new v3();
    private final v3 calculatedLinearDiff = new v3();
    private final boolean useLinearReferenceFrameA;

	public Generic6DofConstraint() {
		super(TypedConstraintType.D6_CONSTRAINT_TYPE);
		useLinearReferenceFrameA = true;
	}

        public Generic6DofConstraint(Body3D rbB, Transform frameInB, boolean useLinearReferenceFrameB)
        {
            super(TypedConstraintType.D6_CONSTRAINT_TYPE, getFixed(), rbB);
            this.frameInB.set(frameInB);
            this.useLinearReferenceFrameA = useLinearReferenceFrameB;

            
            rbB.getCenterOfMassTransform(frameInA);
            frameInA.mul(frameInB);
            
        }

	public Generic6DofConstraint(Body3D rbA, Body3D rbB, Transform frameInA, Transform frameInB, boolean useLinearReferenceFrameA) {
		this(rbA, rbB, frameInA, frameInB, useLinearReferenceFrameA, TypedConstraintType.D6_CONSTRAINT_TYPE);
	}

	private Generic6DofConstraint(Body3D rbA, Body3D rbB, Transform frameInA, Transform frameInB, boolean useLinearReferenceFrameA, TypedConstraintType type) {
		super(type, rbA, rbB);
		this.frameInA.set(frameInA);
		this.frameInB.set(frameInB);
		this.useLinearReferenceFrameA = useLinearReferenceFrameA;
	}

	private static float getMatrixElem(Matrix3f mat, int index) {
		int i = index % 3;
		int j = index / 3;
		return mat.get(i, j);
	}

	/**
	 * MatrixToEulerXYZ from http:
	 */
	private static boolean matrixToEulerXYZ(Matrix3f mat, v3 xyz) {
		
		
		
		

		if (getMatrixElem(mat, 2) < 1.0f) {
			if (getMatrixElem(mat, 2) > -1.0f) {
				xyz.x = (float) Math.atan2(-getMatrixElem(mat, 5), getMatrixElem(mat, 8));
				xyz.y = (float) Math.asin(getMatrixElem(mat, 2));
				xyz.z = (float) Math.atan2(-getMatrixElem(mat, 1), getMatrixElem(mat, 0));
				return true;
			}
			else {
				
				xyz.x = -(float) Math.atan2(getMatrixElem(mat, 3), getMatrixElem(mat, 4));
				xyz.y = -BulletGlobals.SIMD_HALF_PI;
				xyz.z = 0.0f;
				return false;
			}
		}
		
		xyz.x = (float) Math.atan2(getMatrixElem(mat, 3), getMatrixElem(mat, 4));
		xyz.y = BulletGlobals.SIMD_HALF_PI;
		xyz.z = 0.0f;

		return false;
	}

	/**
	 * tests linear limits
	 */
    private void calculateLinearInfo()
        {
            calculatedLinearDiff.sub(calculatedTransformB, calculatedTransformA);

            Matrix3f basisInv = new Matrix3f();
            basisInv.invert(calculatedTransformA.basis);
            basisInv.transform(calculatedLinearDiff);    

            linearLimits.currentLinearDiff.set(calculatedLinearDiff);
            for(int i = 0; i < 3; i++)
            {
                linearLimits.testLimitValue(i, VectorUtil.coord(calculatedLinearDiff, i) );
            }
        }


	/**
	 * Calcs the euler angles between the two bodies.
	 */
    private void calculateAngleInfo() {
		Matrix3f mat = new Matrix3f();

		Matrix3f relative_frame = new Matrix3f();
		mat.set(calculatedTransformA.basis);
		MatrixUtil.invert(mat);
		relative_frame.mul(mat, calculatedTransformB.basis);

		matrixToEulerXYZ(relative_frame, calculatedAxisAngleDiff);

		
		
		
		
		
		
		
		
		
		
		
		
		
		

		v3 axis0 = new v3();
		calculatedTransformB.basis.getColumn(0, axis0);

		v3 axis2 = new v3();
		calculatedTransformA.basis.getColumn(2, axis2);

		calculatedAxis[1].cross(axis2, axis0);
		calculatedAxis[0].cross(calculatedAxis[1], axis2);
		calculatedAxis[2].cross(axis0, calculatedAxis[1]);

		
		
		
		
		
		
		
		
		
		
	}

	/**
	 * Calcs global transform of the offsets.<p>
	 * Calcs the global transform for the joint offset for body A an B, and also calcs the angle differences between the bodies.
	 * 
	 * See also: Generic6DofConstraint.getCalculatedTransformA, Generic6DofConstraint.getCalculatedTransformB, Generic6DofConstraint.calculateAngleInfo
	 */
    private void calculateTransforms() {
		rbA.getCenterOfMassTransform(calculatedTransformA);
		calculatedTransformA.mul(frameInA);

		rbB.getCenterOfMassTransform(calculatedTransformB);
		calculatedTransformB.mul(frameInB);

                calculateLinearInfo();            
		calculateAngleInfo();

	}
	
	private void buildLinearJacobian(/*JacobianEntry jacLinear*/int jacLinear_index, v3 normalWorld, v3 pivotAInW, v3 pivotBInW) {
		Matrix3f mat1 = rbA.getCenterOfMassTransform(new Transform()).basis;
		mat1.transpose();

		Matrix3f mat2 = rbB.getCenterOfMassTransform(new Transform()).basis;
		mat2.transpose();

		v3 tmpVec = new v3();
		
		v3 tmp1 = new v3();
		tmp1.sub(pivotAInW, rbA.getCenterOfMassPosition(tmpVec));

		v3 tmp2 = new v3();
		tmp2.sub(pivotBInW, rbB.getCenterOfMassPosition(tmpVec));

		jacLinear[jacLinear_index].init(
				mat1,
				mat2,
				tmp1,
				tmp2,
				normalWorld,
				rbA.getInvInertiaDiagLocal(new v3()),
				rbA.getInvMass(),
				rbB.getInvInertiaDiagLocal(new v3()),
				rbB.getInvMass());
	}

	private void buildAngularJacobian(/*JacobianEntry jacAngular*/int jacAngular_index, v3 jointAxisW) {
		Matrix3f mat1 = rbA.getCenterOfMassTransform(new Transform()).basis;
		mat1.transpose();

		Matrix3f mat2 = rbB.getCenterOfMassTransform(new Transform()).basis;
		mat2.transpose();

		jacAng[jacAngular_index].init(jointAxisW,
				mat1,
				mat2,
				rbA.getInvInertiaDiagLocal(new v3()),
				rbB.getInvInertiaDiagLocal(new v3()));
	}

	/**
	 * Test angular limit.<p>
	 * Calculates angular correction and returns true if limit needs to be corrected.
	 * Generic6DofConstraint.buildJacobian must be called previously.
	 */
    private boolean testAngularLimitMotor(int axis_index) {
		float angle = VectorUtil.coord(calculatedAxisAngleDiff, axis_index);

		
		angularLimits[axis_index].testLimitValue(angle);
		return angularLimits[axis_index].needApplyTorques();
	}
	
	/**
	 * Test linear limit.<p>
	 * Calculates linear correction and returns true if limit needs to be corrected.
	 * Generic6DofConstraint.buildJacobian must be called previously.
	 */
    private boolean testLinearLimitMotor(int axis_index) {
		float diff = VectorUtil.coord(calculatedLinearDiff, axis_index);

		
		linearLimits.testLimitValue(axis_index, diff); 
		return linearLimits.needApplyForces(axis_index);
	}


        @Override
	public void buildJacobian() {
		
		linearLimits.accumulatedImpulse.set(0f, 0f, 0f);
		for (int i=0; i<3; i++) {
			angularLimits[i].accumulatedImpulse = 0f;
		}
		
		
		calculateTransforms();
		
		v3 tmpVec = new v3();

		
		
		calcAnchorPos();
		v3 pivotAInW = new v3(anchorPos);
		v3 pivotBInW = new v3(anchorPos);
		
		
		
		

		v3 normalWorld = new v3();
		
		for (int i=0; i<3; i++) {
			if ( testLinearLimitMotor(i))
                        {
				if (useLinearReferenceFrameA) {
					calculatedTransformA.basis.getColumn(i, normalWorld);
				}
				else {
					calculatedTransformB.basis.getColumn(i, normalWorld);
				}

				buildLinearJacobian(
						/*jacLinear[i]*/i, normalWorld,
						pivotAInW, pivotBInW);

			}
		}

		
		for (int i=0; i<3; i++) {
			
			if (testAngularLimitMotor(i)) {   
				this.getAxis(i, normalWorld);
				
				buildAngularJacobian(/*jacAng[i]*/i, normalWorld);
			}
		}
	}

	@Override
	public void solveConstraint(float timeStep) {
		this.timeStep = timeStep;

		

		int i;

		

		v3 pointInA = new v3(calculatedTransformA);
		v3 pointInB = new v3(calculatedTransformB);

		float jacDiagABInv;
		v3 linear_axis = new v3();
		for (i = 0; i < 3; i++) {
			if (linearLimits.needApplyForces(i))
                        {
							jacDiagABInv = 1f / jacLinear[i].Adiag;

				if (useLinearReferenceFrameA) {
					calculatedTransformA.basis.getColumn(i, linear_axis);
				}
				else {
					calculatedTransformB.basis.getColumn(i, linear_axis);
				}

				linearLimits.solveLinearAxis(
						this.timeStep,
						jacDiagABInv,
						rbA, pointInA,
						rbB, pointInB,
						i, linear_axis, anchorPos);
			}
		}

		
		v3 angular_axis = new v3();
		float angularJacDiagABInv;
		for (i = 0; i < 3; i++) {
			if (angularLimits[i].needApplyTorques()) { 
				
				getAxis(i, angular_axis);

				angularJacDiagABInv = 1f / jacAng[i].Adiag;

				angularLimits[i].solveAngularLimits(
                                        this.timeStep,
                                        angular_axis,
                                        angularJacDiagABInv,
                                        rbA,
                                        rbB);
			}
		}
	}
	

    public void updateRHS(float timeStep) {
	}

	/**
	 * Get the rotation axis in global coordinates.
	 * Generic6DofConstraint.buildJacobian must be called previously.
	 */
    private v3 getAxis(int axis_index, v3 out) {
		out.set(calculatedAxis[axis_index]);
		return out;
	}

	/**
	 * Get the relative Euler angle.
	 * Generic6DofConstraint.buildJacobian must be called previously.
	 */
	public float getAngle(int axis_index) {
		return VectorUtil.coord(calculatedAxisAngleDiff, axis_index);
	}

	/**
	 * Gets the global transform of the offset for body A.<p>
	 * See also: Generic6DofConstraint.getFrameOffsetA, Generic6DofConstraint.getFrameOffsetB, Generic6DofConstraint.calculateAngleInfo.
	 */
	public Transform getCalculatedTransformA(Transform out) {
		out.set(calculatedTransformA);
		return out;
	}

	/**
	 * Gets the global transform of the offset for body B.<p>
	 * See also: Generic6DofConstraint.getFrameOffsetA, Generic6DofConstraint.getFrameOffsetB, Generic6DofConstraint.calculateAngleInfo.
	 */
	public Transform getCalculatedTransformB(Transform out) {
		out.set(calculatedTransformB);
		return out;
	}

	public Transform getFrameOffsetA(Transform out) {
		out.set(frameInA);
		return out;
	}

	public Transform getFrameOffsetB(Transform out) {
		out.set(frameInB);
		return out;
	}
	
	public void setLinearLowerLimit(v3 linearLower) {
		linearLimits.lowerLimit.set(linearLower);
	}

	public void setLinearUpperLimit(v3 linearUpper) {
		linearLimits.upperLimit.set(linearUpper);
	}

	public void setAngularLowerLimit(v3 angularLower) {
		angularLimits[0].loLimit = angularLower.x;
		angularLimits[1].loLimit = angularLower.y;
		angularLimits[2].loLimit = angularLower.z;
	}

	public void setAngularUpperLimit(v3 angularUpper) {
		angularLimits[0].hiLimit = angularUpper.x;
		angularLimits[1].hiLimit = angularUpper.y;
		angularLimits[2].hiLimit = angularUpper.z;
	}

	/**
	 * Retrieves the angular limit information.
	 */
	public RotationalLimitMotor getRotationalLimitMotor(int index) {
		return angularLimits[index];
	}

	/**
	 * Retrieves the limit information.
	 */
	public TranslationalLimitMotor getTranslationalLimitMotor() {
		return linearLimits;
	}

	/**
	 * first 3 are linear, next 3 are angular
	 */
	public void setLimit(int axis, float lo, float hi) {
		if (axis < 3) {
			VectorUtil.setCoord(linearLimits.lowerLimit, axis, lo);
			VectorUtil.setCoord(linearLimits.upperLimit, axis, hi);
		}
		else {
			angularLimits[axis - 3].loLimit = lo;
			angularLimits[axis - 3].hiLimit = hi;
		}
	}
	
	/**
	 * Test limit.<p>
	 * - free means upper &lt; lower,<br>
	 * - locked means upper == lower<br>
	 * - limited means upper &gt; lower<br>
	 * - limitIndex: first 3 are linear, next 3 are angular
	 */
	public boolean isLimited(int limitIndex) {
		if (limitIndex < 3) {
			return linearLimits.isLimited(limitIndex);
		}
		return angularLimits[limitIndex - 3].isLimited();
	}
	
	
	private void calcAnchorPos() {
		float imA = rbA.getInvMass();
		float imB = rbB.getInvMass();
		float weight;
		if (imB == 0f) {
			weight = 1f;
		}
		else {
			weight = imA / (imA + imB);
		}
		v3 pA = calculatedTransformA;
		v3 pB = calculatedTransformB;

		v3 tmp1 = new v3();
		v3 tmp2 = new v3();

		tmp1.scale(weight, pA);
		tmp2.scale(1f - weight, pB);
		anchorPos.add(tmp1, tmp2);
	}
	
}
