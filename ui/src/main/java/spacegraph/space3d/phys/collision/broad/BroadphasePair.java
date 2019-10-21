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

import java.util.Comparator;

/**
 * BroadphasePair class contains a pair of AABB-overlapping objects.
 * {@link Intersecter} can search a {@link CollisionAlgorithm} that performs
 * exact/narrowphase collision detection on the actual collision shapes.
 *
 * @author jezek2
 */
public class BroadphasePair {

	public Broadphasing pProxy0;
	public Broadphasing pProxy1;
	public CollisionAlgorithm algorithm;
	public Object userInfo;

	public BroadphasePair() {
	}

	public BroadphasePair(Broadphasing pProxy0, Broadphasing pProxy1) {
		this.pProxy0 = pProxy0;
		this.pProxy1 = pProxy1;
		this.algorithm = null;
		this.userInfo = null;
	}
	
	public void set(BroadphasePair p) {
		pProxy0 = p.pProxy0;
		pProxy1 = p.pProxy1;
		algorithm = p.algorithm;
		userInfo = p.userInfo;
	}
	
	public boolean equals(Object pp) {
		if (this == pp) return true;
        BroadphasePair p = (BroadphasePair) pp;
		return (pProxy0 == p.pProxy0 && pProxy1 == p.pProxy1);
	}
	
	public static final Comparator<BroadphasePair> broadphasePairSortPredicate = new Comparator<BroadphasePair>() {
        @Override
        public int compare(BroadphasePair a, BroadphasePair b) {
            if (a == b)
                return 0;


            Broadphasing a0 = a.pProxy0;
            Broadphasing b0 = b.pProxy0;
            Broadphasing a1 = a.pProxy1;
            Broadphasing b1 = b.pProxy1;
            int a0uid = a0.uid;
            int b0uid = b0.uid;
            int a1uid = a1.uid;
            int b1uid = b1.uid;
            boolean result = a0uid > b0uid ||
                    (a0uid == b0uid && a1uid > b1uid) ||
                    (a0uid == b0uid && a1uid == b1uid /*&& a.algorithm > b.m_algorithm*/);
            return result ? -1 : 1;
        }
    };
	
}
