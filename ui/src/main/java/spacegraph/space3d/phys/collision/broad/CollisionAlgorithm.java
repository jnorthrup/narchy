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

package spacegraph.space3d.phys.collision.broad;

import spacegraph.space3d.phys.Collidable;
import spacegraph.space3d.phys.collision.ManifoldResult;
import spacegraph.space3d.phys.collision.narrow.PersistentManifold;
import spacegraph.space3d.phys.util.OArrayList;

/**
 * Collision algorithm for handling narrowphase or midphase collision detection
 * between two collision object types.
 * 
 * @author jezek2
 */
public abstract class CollisionAlgorithm {

	

	protected Intersecter intersecter;

	public void init() {
	}

	public void init(CollisionAlgorithmConstructionInfo ci) {
		intersecter = ci.intersecter1;
	}

	public abstract void destroy();

	public abstract void processCollision(Collidable body0, Collidable body1, DispatcherInfo dispatchInfo, ManifoldResult resultOut);

	public abstract float calculateTimeOfImpact(Collidable body0, Collidable body1, DispatcherInfo dispatchInfo, ManifoldResult resultOut);
	
	public abstract void getAllContactManifolds(OArrayList<PersistentManifold> manifoldArray);
	

}
