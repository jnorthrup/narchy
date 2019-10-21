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

package spacegraph.space3d.phys.collision;

import spacegraph.space3d.phys.Collidable;
import spacegraph.space3d.phys.collision.broad.BroadphasePair;
import spacegraph.space3d.phys.collision.broad.DispatchFunc;
import spacegraph.space3d.phys.collision.broad.DispatcherInfo;

/**
 * Default implementation of {@link NearCallback}.
 *
 * @author jezek2
 */
public class DefaultNearCallback implements NearCallback {

	private final ManifoldResult contactPointResult = new ManifoldResult();

	@Override
	public void handleCollision(BroadphasePair collisionPair, DefaultIntersecter dispatcher, DispatcherInfo dispatchInfo) {
        Collidable colObj0 = collisionPair.pProxy0.data;
        Collidable colObj1 = collisionPair.pProxy1.data;

		if (dispatcher.needsCollision(colObj0, colObj1)) {
			
			if (collisionPair.algorithm == null) {
				collisionPair.algorithm = dispatcher.findAlgorithm(colObj0, colObj1);
			}

			if (collisionPair.algorithm != null) {
				
				contactPointResult.init(colObj0, colObj1);

				if (dispatchInfo.dispatchFunc == DispatchFunc.DISPATCH_DISCRETE) {
					
					collisionPair.algorithm.processCollision(colObj0, colObj1, dispatchInfo, contactPointResult);
				}
				else {

                    float toi = collisionPair.algorithm.calculateTimeOfImpact(colObj0, colObj1, dispatchInfo, contactPointResult);
					if (dispatchInfo.timeOfImpact > toi) {
						dispatchInfo.timeOfImpact = toi;
					}
				}
			}
		}
	}
	
}
