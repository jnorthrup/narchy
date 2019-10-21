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
import spacegraph.space3d.phys.collision.broad.*;

/**
 * GhostPairCallback interfaces and forwards adding and removal of overlapping
 * pairs from the {@link Broadphase} to {@link GhostObject}.
 *
 * @author tomrbryn
 */
public class GhostPairCallback implements OverlappingPairCallback {

	@Override
	public BroadphasePair addOverlappingPair(Broadphasing proxy0, Broadphasing proxy1) {
		Collidable colObj0 = proxy0.data;
		Collidable colObj1 = proxy1.data;
		GhostObject ghost0 = GhostObject.upcast(colObj0);
		GhostObject ghost1 = GhostObject.upcast(colObj1);

		if (ghost0 != null) {
			ghost0.addOverlappingObjectInternal(proxy1, proxy0);
		}
		if (ghost1 != null) {
			ghost1.addOverlappingObjectInternal(proxy0, proxy1);
		}
		return null;
	}

	@Override
	public Object removeOverlappingPair(Broadphasing proxy0, Broadphasing proxy1, Intersecter intersecter) {
		Collidable colObj0 = proxy0.data;
		Collidable colObj1 = proxy1.data;
		GhostObject ghost0 = GhostObject.upcast(colObj0);
		GhostObject ghost1 = GhostObject.upcast(colObj1);
		
		if (ghost0 != null) {
			ghost0.removeOverlappingObjectInternal(proxy1, intersecter, proxy0);
		}
		if (ghost1 != null) {
			ghost1.removeOverlappingObjectInternal(proxy0, intersecter, proxy1);
		}
		return null;
	}

	@Override
	public void removeOverlappingPairsContainingProxy(Broadphasing proxy0, Intersecter intersecter) {
		assert (false);

		
		
	}
	
}
