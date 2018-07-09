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

package spacegraph.space3d.phys.math;

import com.jogamp.opengl.math.Quaternion;
import spacegraph.util.math.*;


/**
 * Transform represents translation and rotation (rigid transform). Scaling and
 * shearing is not supported.<p>
 * 
 * You can use local shape scaling or {@link UniformScalingShape} for static rescaling
 * of collision objects.
 *
 * the vector that this extends represents the 'origin' vector
 * ("Translation vector of this Transform.")
 * originally existing as its own field
 *
 * @author jezek2
 */
public final class Transform extends v3 {

	/** Rotation matrix of this Transform. */
	public final Matrix3f basis = new Matrix3f();

	public Transform() {
	}

	public Transform(float x, float y, float z) {
		setIdentity();
		this.set(x, y, z);
	}

	public Transform(v3 v) {
		setIdentity();
		this.set(v);
	}



	public static Transform t() {
		return new Transform();
	}




	public Transform(Matrix3f mat) {
		basis.set(mat);
	}

	public Transform(Matrix4f mat) {
		set(mat);
	}

	public Transform(Transform tr) {
		set(tr);
	}

	public static Transform t(Transform copy) {
        return new Transform(copy);
    }

	@Override
	public String toString() {
		return "t(" +
				super.toString() + ',' + basis.toStringCompact() +
				')';
	}

	public void set(Transform tr) {
		assert(this!=tr);

		super.set(tr); 

		basis.set(tr.basis); 
	}
	
	public void set(Matrix3f mat) {
		assert(this.basis!=mat);
		basis.set(mat);
		this.set(0f, 0f, 0f);
	}

	private void set(Matrix4f mat) {
		mat.getRotationScale(basis);
		this.set(mat.m03, mat.m13, mat.m23);
	}
	
	public v3 transform(v3 v) {
		basis.transform(v);
		v.add(this);
		return v;
	}

	public Transform setIdentity() {
		basis.setIdentity();
		this.set(0f, 0f, 0f);
		return this;
	}
	
	public Transform inverse() {
		basis.transpose();
		this.scale(-1f);
		basis.transform(this);
		return this;
	}

	public Transform  inverse(Transform tr) {
		set(tr);
		return inverse();
	}
	
	public void mul(Transform tr) {
		v3 vec = new v3(tr);
		transform(vec);

		basis.mul(tr.basis);
		this.set(vec);
	}

	public void mul(Transform tr1, Transform tr2) {
		v3 vec = new v3(tr2);
		tr1.transform(vec);

		basis.mul(tr1.basis, tr2.basis);
		this.set(vec);
	}
	
	public void invXform(v3 inVec, v3 out) {
		out.sub(inVec, this);

		Matrix3f mat = new Matrix3f(basis);
		mat.transpose();
		mat.transform(out);
	}
	
	public Quat4f getRotation(Quat4f out) {
		MatrixUtil.getRotation(basis, out);
		return out;
	}
	public Quaternion getRotation(Quaternion out) {
		MatrixUtil.getRotation(basis, out);
		return out;
	}

	public void setRotation(Quat4f q) {
		MatrixUtil.setRotation(basis, q);
	}
	public void setRotation(Quaternion q) {
		MatrixUtil.setRotation(basis, q);
	}

	public void setFromOpenGLMatrix(float[] m) {
		MatrixUtil.setFromOpenGLSubMatrix(basis, m);
		this.set(m[12], m[13], m[14]);
	}

	public float[] getOpenGLMatrix(float[] m) {
		MatrixUtil.getOpenGLSubMatrix(basis, m);
		m[12] = this.x;
		m[13] = this.y;
		m[14] = this.z;
		m[15] = 1f;
		return m;
	}

	public Matrix4f getMatrix(Matrix4f out) {
		out.set(basis);
		out.m03 = this.x;
		out.m13 = this.y;
		out.m23 = this.z;
		return out;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof Transform)) return false;
		Transform tr = (Transform)obj;
		return basis.equals(tr.basis) && super.equals(tr);
	}

	@Override
	public int hashCode() {
		int hash = 3;
		hash = 41 * hash + basis.hashCode();
		hash = 41 * hash + super.hashCode();
		return hash;
	}

	public final AxisAngle4f toAngleAxis(Quaternion tmpQ, AxisAngle4f tmpA, v3 angle) {
		tmpA.set(getRotation(tmpQ), false);
		tmpA.get(angle);
		return tmpA;
	}

	public void setTransScale(float x, float y, float scale) {
		set(x, y, 0);
		scale(scale);
	}

}
