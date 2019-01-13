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

import jcog.math.v3;
import org.jetbrains.annotations.NotNull;
import spacegraph.space3d.phys.Collidable;
import spacegraph.space3d.phys.util.OArrayList;

/**
 * SimpleBroadphase is just a unit-test for {@link AxisSweep3}, {@link AxisSweep3_32},
 * or {@link DbvtBroadphase}, so use those classes instead. It is a brute force AABB
 * culling broadphase based on O(n^2) AABB checks.
 *
 * @author jezek2
 */
public class SimpleBroadphase extends Broadphase {

    private final OArrayList<SimpleBroadphasing> handles = new OArrayList<>();
    
    private final OverlappingPairCache pairCache;
    private boolean ownsPairCache;

    public SimpleBroadphase() {
        this(16384, null);
    }

    public SimpleBroadphase(int maxProxies) {
        this(maxProxies, null);
    }

    private SimpleBroadphase(int maxProxies, OverlappingPairCache overlappingPairCache) {


        if (overlappingPairCache == null) {
            this.pairCache = new HashedOverlappingPairCache();
            this.ownsPairCache = true;
        } else {
            this.pairCache = overlappingPairCache;
        }

    }

    @Override
    public Broadphasing createProxy(v3 aabbMin, v3 aabbMax, BroadphaseNativeType shapeType, Collidable userPtr, short collisionFilterGroup, short collisionFilterMask, Intersecter intersecter, Object multiSapProxy) {
        assert (aabbMin.x <= aabbMax.x && aabbMin.y <= aabbMax.y && aabbMin.z <= aabbMax.z);

        SimpleBroadphasing proxy = new SimpleBroadphasing(aabbMin, aabbMax, shapeType, userPtr, collisionFilterGroup, collisionFilterMask, multiSapProxy);
        proxy.uid = handles.size();
        handles.add(proxy);
        return proxy;
    }

    @Override
    public void destroyProxy(Broadphasing proxyOrg, Intersecter intersecter) {
        handles.remove(proxyOrg);

        pairCache.removeOverlappingPairsContainingProxy(proxyOrg, intersecter);
    }

    @Override
    public void setAabb(@NotNull Broadphasing proxy, v3 aabbMin, v3 aabbMax, Intersecter intersecter) {
        SimpleBroadphasing sbp = (SimpleBroadphasing) proxy;
        sbp.min.set(aabbMin);
        sbp.max.set(aabbMax);
    }

    private static boolean aabbOverlap(SimpleBroadphasing a, SimpleBroadphasing b) {
        float aminX = a.min.x;
        float bmaxX = b.max.x;

        if (aminX <= bmaxX) {

            float amaxX = a.max.x;
            float bminX = b.min.x;

            if (bminX <= amaxX) {

                float aminY = a.min.y;
                float bmaxY = b.max.y;

                if (aminY <= bmaxY) {

                    float amaxY = a.max.y;
                    float bminY = b.min.y;

                    if (bminY <= amaxY) {

                        float aminZ = a.min.z;
                        float bmaxZ = b.max.z;


                        if (aminZ <= bmaxZ) {

                            float amaxZ = a.max.z;
                            float bminZ = b.min.z;

                            return bminZ <= amaxZ;
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void update(Intersecter intersecter) {
        for (int i = 0; i < handles.size(); i++) {
            
            SimpleBroadphasing proxy0 = handles.get(i);
            for (SimpleBroadphasing proxy1 : handles) {

                if (proxy0 == proxy1) continue;

                if (aabbOverlap(proxy0, proxy1)) {
                    if (pairCache.findPair(proxy0, proxy1) == null) {
                        pairCache.addOverlappingPair(proxy0, proxy1);
                    }
                } else {


                    if (!pairCache.hasDeferredRemoval()) {
                        if (pairCache.findPair(proxy0, proxy1) != null) {
                            pairCache.removeOverlappingPair(proxy0, proxy1, intersecter);
                        }
                    }
                }
            }
        }
    }

    @Override
    public OverlappingPairCache getOverlappingPairCache() {
        return pairCache;
    }

    @Override
    public void getBroadphaseAabb(v3 aabbMin, v3 aabbMax) {
        aabbMin.set(-1e30f, -1e30f, -1e30f);
        aabbMax.set(1e30f, 1e30f, 1e30f);
    }

    @Override
    public void printStats() {


    }

}
