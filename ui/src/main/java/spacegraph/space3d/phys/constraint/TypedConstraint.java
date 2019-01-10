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

package spacegraph.space3d.phys.constraint;

import spacegraph.space3d.phys.Body3D;
import spacegraph.space3d.phys.math.Transform;
import spacegraph.space3d.phys.solve.ContactSolverInfo;
import jcog.math.v3;

/**
 * TypedConstraint is the base class for Bullet constraints and vehicles.
 * 
 * @author jezek2
 */
public abstract class TypedConstraint {
	
	
	
	
	private static final Body3D s_fixed = new Body3D(0, new Transform(), null);
	
	@Deprecated protected static /*synchronized*/ Body3D getFixed() {



		return s_fixed;
	}

	private int userConstraintType = -1;
	private int userConstraintId = -1;

	private final TypedConstraintType type;
	
	protected final Body3D rbA;
	protected final Body3D rbB;
	float appliedImpulse;

	protected TypedConstraint(TypedConstraintType type) {
		this(type, getFixed(), getFixed());
	}
	
	TypedConstraint(TypedConstraintType type, Body3D rbA) {
		this(type, rbA, getFixed());
	}
	
	protected TypedConstraint(TypedConstraintType type, Body3D rbA, Body3D rbB) {
		this.type = type;
		this.rbA = rbA;
		this.rbB = rbB;
		getFixed().setMass(0f, new v3(0f, 0f, 0f));
	}
	
	public abstract void buildJacobian();

	public abstract void solveConstraint(float timeStep);
	
	public Body3D getRigidBodyA() {
		return rbA;
	}

	public Body3D getRigidBodyB() {
		return rbB;
	}

	public int getUserConstraintType() {
		return userConstraintType;
	}
	
	public void setUserConstraintType(int userConstraintType) {
		this.userConstraintType = userConstraintType;
	}

	public int getUserConstraintId() {
		return userConstraintId;
	}

	public int getUid() {
		return userConstraintId;
	}

	public void setUserConstraintId(int userConstraintId) {
		this.userConstraintId = userConstraintId;
	}

	public float getAppliedImpulse() {
		return appliedImpulse;
	}


	
        
        
        public void getInfo2(ContactSolverInfo infoGlobal) {
        }

	/**
     * Typed constraint type.
     *
     * @author jezek2
     */
    public enum TypedConstraintType {
        POINT2POINT_CONSTRAINT_TYPE,
        HINGE_CONSTRAINT_TYPE,
        CONETWIST_CONSTRAINT_TYPE,
        D6_CONSTRAINT_TYPE,
        VEHICLE_CONSTRAINT_TYPE,
        SLIDER_CONSTRAINT_TYPE,
            D6_SPRING_CONSTRAINT_TYPE
    }
}
