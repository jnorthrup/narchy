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

package spacegraph.space3d.phys.shape;

import spacegraph.space3d.phys.Collidable;
import spacegraph.space3d.phys.collision.broad.BroadphaseNativeType;
import spacegraph.space3d.phys.math.Transform;
import spacegraph.util.math.v3;

import static spacegraph.util.math.v3.v;

/**
 * CollisionShape class provides an interface for collision shapes that can be
 * shared among {@link Collidable}s.
 * 
 * @author jezek2
 */
public abstract class CollisionShape {

	

	private Object userPointer;
	
	
	public abstract void getAabb(Transform t, v3 aabbMin, v3 aabbMax);

	public float getBoundingRadius() {
		return getBoundingSphere(null);
	}

	private float getBoundingSphere(v3 center) {

		v3 aabbMin = new v3(), aabbMax = new v3();
		getAabb(new Transform().setIdentity(), aabbMin, aabbMax);

		v3 tmp = new v3();
		tmp.sub(aabbMax, aabbMin);

		float radius = tmp.length() * 0.5f;

		if (center!=null) {
			tmp.add(aabbMin, aabbMax);
			center.scale(0.5f, tmp);
		}
		return radius;
	}

	
	private float getAngularMotionDisc() {
		v3 center = new v3();
		return getBoundingSphere(center) + center.length();
	}

	
	
	public void calculateTemporalAabb(Transform curTrans, v3 linvel, v3 angvel, float timeStep, v3 temporalAabbMin, v3 temporalAabbMax) {
		
		getAabb(curTrans, temporalAabbMin, temporalAabbMax);

		float temporalAabbMaxx = temporalAabbMax.x;
		float temporalAabbMaxy = temporalAabbMax.y;
		float temporalAabbMaxz = temporalAabbMax.z;
		float temporalAabbMinx = temporalAabbMin.x;
		float temporalAabbMiny = temporalAabbMin.y;
		float temporalAabbMinz = temporalAabbMin.z;

		
		v3 linMotion = new v3(linvel, timeStep);

		
		if (linMotion.x > 0f) {
			temporalAabbMaxx += linMotion.x;
		}
		else {
			temporalAabbMinx += linMotion.x;
		}
		if (linMotion.y > 0f) {
			temporalAabbMaxy += linMotion.y;
		}
		else {
			temporalAabbMiny += linMotion.y;
		}
		if (linMotion.z > 0f) {
			temporalAabbMaxz += linMotion.z;
		}
		else {
			temporalAabbMinz += linMotion.z;
		}

		
		float angularMotion = angvel.length() * getAngularMotionDisc() * timeStep;
		v3 angularMotion3d = new v3();
		angularMotion3d.set(angularMotion, angularMotion, angularMotion);
		temporalAabbMin.set(temporalAabbMinx, temporalAabbMiny, temporalAabbMinz);
		temporalAabbMax.set(temporalAabbMaxx, temporalAabbMaxy, temporalAabbMaxz);

		temporalAabbMin.sub(angularMotion3d);
		temporalAabbMax.add(angularMotion3d);
	}


	public boolean isPolyhedral() {
		return getShapeType().isPolyhedral();
	}

	public boolean isConvex() {
		return getShapeType().isConvex();
	}

	public boolean isConcave() {
		return getShapeType().isConcave();
	}

	public boolean isCompound() {
		return getShapeType().isCompound();
	}

	
	public boolean isInfinite() {
		return getShapeType().isInfinite();
	}

	public abstract BroadphaseNativeType getShapeType();

	protected abstract void setLocalScaling(v3 scaling);
	
	
	protected abstract v3 getLocalScaling(v3 out);

	public abstract void calculateLocalInertia(float mass, v3 inertia);



	public abstract String getName();

	protected abstract CollisionShape setMargin(float margin);

	public abstract float getMargin();
	
	
	public void setUserPointer(Object userPtr) {
		userPointer = userPtr;
	}

	public Object getUserPointer() {
		return userPointer;
	}

	public void setLocalScaling(float x, float y, float z){
		setLocalScaling(v(Math.abs(x), Math.abs(y), Math.abs(z)));
	}

}
