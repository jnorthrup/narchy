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
import spacegraph.space3d.phys.collision.broad.Broadphasing;
import spacegraph.space3d.phys.collision.broad.HashedOverlappingPairCache;
import spacegraph.space3d.phys.collision.broad.Intersecter;

/**
 *
 * @author tomrbryn
 */
public class PairCachingGhostObject extends GhostObject {
	
	private final HashedOverlappingPairCache hashPairCache = new HashedOverlappingPairCache();

	/**
	 * This method is mainly for expert/internal use only.
	 */
	@Override
	public void addOverlappingObjectInternal(Broadphasing otherProxy, Broadphasing thisProxy) {
		var actualThisProxy = thisProxy != null? thisProxy : broadphase;
		assert(actualThisProxy != null);

		var otherObject = otherProxy.data;
		assert (otherObject != null);


		var index = overlappingObjects.indexOf(otherObject);
		if (index == -1) {
			overlappingObjects.add(otherObject);
			hashPairCache.addOverlappingPair(actualThisProxy, otherProxy);
		}
	}

	@Override
	public void removeOverlappingObjectInternal(Broadphasing otherProxy, Intersecter intersecter, Broadphasing thisProxy1) {
		var otherObject = otherProxy.data;
		var actualThisProxy = thisProxy1 != null? thisProxy1 : broadphase;
		assert(actualThisProxy != null);

		assert (otherObject != null);
		var index = overlappingObjects.indexOf(otherObject);
		if (index != -1) {
            
            overlappingObjects.setFast(index, overlappingObjects.get(overlappingObjects.size() - 1));
			overlappingObjects.removeFast(overlappingObjects.size()-1);
			hashPairCache.removeOverlappingPair(actualThisProxy, otherProxy, intersecter);
		}
	}

	public HashedOverlappingPairCache getOverlappingPairCache() {
		return hashPairCache;
	}
	
}
