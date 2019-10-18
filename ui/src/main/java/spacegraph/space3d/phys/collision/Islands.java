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

import jcog.data.list.FasterList;
import spacegraph.space3d.phys.Collidable;
import spacegraph.space3d.phys.Collisions;
import spacegraph.space3d.phys.collision.broad.BroadphasePair;
import spacegraph.space3d.phys.collision.broad.Intersecter;
import spacegraph.space3d.phys.collision.narrow.PersistentManifold;
import spacegraph.space3d.phys.math.MiscUtil;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * SimulationIslandManager creates and handles simulation islands, using {@link UnionFind}.
 *
 * @author jezek2
 */
public class Islands {

    
    public final UnionFind2 find = new UnionFind2();

    private final FasterList<PersistentManifold> islandmanifold = new FasterList<>();
    private final FasterList<Collidable> islandBodies = new FasterList<>();

    private void findUnions(Collisions colWorld) {
        FasterList<BroadphasePair> pairPtr = colWorld.pairs().getOverlappingPairArray();
        int n = pairPtr.size();


        for (BroadphasePair collisionPair : pairPtr) {

            Collidable colObj0 = collisionPair.pProxy0.data;
            if (colObj0 != null && ((colObj0).mergesSimulationIslands())) {
                Collidable colObj1 = collisionPair.pProxy1.data;
                if (colObj1 != null && ((colObj1).mergesSimulationIslands())) {
                    find.unite((colObj0).tag(), (colObj1).tag());
                }
            }
        }
    }

    public final void updateActivationState(Collisions<?> colWorld) {
        List<Collidable> cc = colWorld.collidables();
        int num = cc.size();

        find.reset(num);

        final int[] i = {0};
        for (Collidable collidable : cc) {
            collidable.setIslandTag(i[0]++);
            collidable.setCompanionId(-1);
            collidable.setHitFraction(1f);
        }


        findUnions(colWorld);
    }

    public final void storeIslandActivationState(Collisions<?> world) {
        
        int i = 0;
        List<Collidable> collidables = world.collidables();
        for (Collidable collidable : collidables) {
            storeIslandActivationState(i++, collidable);
        }
    }

    private boolean storeIslandActivationState(int i, Collidable c) {
        if (!c.isStaticOrKinematicObject()) {
            c.setIslandTag(find.find(i));
            c.setCompanionId(-1);
        } else {
            c.setIslandTag(-1);
            c.setCompanionId(-2);
        }
        return true;
    }

    private static int getIslandId(PersistentManifold lhs) {
        Collidable rcolObj0 = (Collidable) lhs.getBody0();
        int t0 = rcolObj0.tag();
        if (t0 >= 0) return t0;
        Collidable rcolObj1 = (Collidable) lhs.getBody1();
        return rcolObj1.tag();
    }

    private void buildIslands(Intersecter intersecter, List<Collidable> collidables) {

        


            islandmanifold.clearFast();

            
            

            find.sortIslands();
            int numElem = find.size();

            int endIslandIndex = 1;
            int startIslandIndex;

            
            for (startIslandIndex = 0; startIslandIndex < numElem; startIslandIndex = endIslandIndex) {
                int islandId = find.id(startIslandIndex);
                for (endIslandIndex = startIslandIndex + 1; (endIslandIndex < numElem) && (find.id(endIslandIndex) == islandId); endIslandIndex++) {
                }

                

                boolean allSleeping = true;

                int idx;
                for (idx = startIslandIndex; idx < endIslandIndex; idx++) {
                    int i = find.sz(idx);

                    
                    final Collidable colObj0 = collidables.get(i);
                    final int tag0 = colObj0.tag();

                    if ((tag0 != islandId) && (tag0 != -1)) {
                        islandError(colObj0);
                        continue;
                    }

                    
                    if (tag0 == islandId) {
                        int s = colObj0.getActivationState();
                        if (s == Collidable.ACTIVE_TAG || s == Collidable.DISABLE_DEACTIVATION) {
                            allSleeping = false;
                        }
                    }
                }


                if (allSleeping) {
                    
                    for (idx = startIslandIndex; idx < endIslandIndex; idx++) {
                        int i = find.sz(idx);
                        
                        final Collidable colObj0 = collidables.get(i);
                        int tag0 = colObj0.tag();
                        if ((tag0 != islandId) && (tag0 != -1)) {
                            islandError(colObj0);
                            continue;
                        }

                        if (tag0 == islandId) {
                            colObj0.setActivationState(Collidable.ISLAND_SLEEPING);
                        }
                    }
                } else {

                    
                    for (idx = startIslandIndex; idx < endIslandIndex; idx++) {
                        int i = find.sz(idx);

                        
                        Collidable colObj0 = collidables.get(i);
                        int tag0 = colObj0.tag();
                        if ((tag0 != islandId) && (tag0 != -1)) {
                            islandError(colObj0);
                            continue;
                        }

                        if (tag0 == islandId) {
                            if (colObj0.getActivationState() == Collidable.ISLAND_SLEEPING) {
                                colObj0.setActivationState(Collidable.WANTS_DEACTIVATION);
                            }
                        }
                    }
                }
            }


            int i;
            int maxNumManifolds = intersecter.manifoldCount();

            
            
            

            for (i = 0; i < maxNumManifolds; i++) {
                PersistentManifold manifold = intersecter.manifold(i);

                Collidable colObj0 = (Collidable) manifold.getBody0();
                if (colObj0!=null) {
                    Collidable colObj1 = (Collidable) manifold.getBody1();
                    if (colObj1!=null) {

                        
                        int s0 = colObj0.getActivationState();
                        int s1 = colObj1.getActivationState();
                        if ((s0 != Collidable.ISLAND_SLEEPING) || (s1 != Collidable.ISLAND_SLEEPING)) {

                            
                            if (s0 != Collidable.ISLAND_SLEEPING && colObj0.isKinematicObject()) {
                                colObj1.activate(true);
                            }
                            if (s1 != Collidable.ISLAND_SLEEPING && colObj1.isKinematicObject()) {
                                colObj0.activate(true);
                            }

                            
                            
                            if (intersecter.needsResponse(colObj0, colObj1)) {
                                islandmanifold.add(manifold);
                            }
                            
                        }
                    }
                }
            }

    }

    private static void islandError(Collidable colObj0) {
        System.err.println("error in island management, maybe spatial is in the display list multiple times: " + colObj0 + ' ' + colObj0.data());
    }

    public void buildAndProcessIslands(Intersecter intersecter, List<Collidable> collidables, IslandCallback callback) {
        buildIslands(intersecter, collidables);

        int endIslandIndex = 1;
        int startIslandIndex;
        int numElem = find.size();


            
            
            
            
            
            
            
            

            int numManifolds = islandmanifold.size();

            
            

            
            
            MiscUtil.quickSort(islandmanifold, persistentManifoldComparator);

            

            int startManifoldIndex = 0;
            int endManifoldIndex = 1;

            

            

            
            for (startIslandIndex = 0; startIslandIndex < numElem; startIslandIndex = endIslandIndex) {
                int islandId = find.id(startIslandIndex);
                boolean islandSleeping = false;

                for (endIslandIndex = startIslandIndex; (endIslandIndex < numElem) && ((find.id(endIslandIndex) == islandId)); endIslandIndex++) {
                    int i = find.sz(endIslandIndex);
                    
                    Collidable colObj0 = collidables.get(i);
                    islandBodies.add(colObj0);
                    if (!colObj0.isActive()) {
                        islandSleeping = true;
                    }
                }


                
                int numIslandManifolds = 0;
                
                int startManifold_idx = -1;

                if (startManifoldIndex < numManifolds) {
                    
                    int curIslandId = getIslandId(islandmanifold.get(startManifoldIndex));
                    if (curIslandId == islandId) {
                        
                        
                        startManifold_idx = startManifoldIndex;

                        
                        for (endManifoldIndex = startManifoldIndex + 1; (endManifoldIndex < numManifolds) && (islandId == getIslandId(islandmanifold.get(endManifoldIndex))); endManifoldIndex++) {

                        }
                        
                        numIslandManifolds = endManifoldIndex - startManifoldIndex;
                    }

                }


                if (!islandSleeping) {
                    callback.processIsland(islandBodies, islandmanifold, startManifold_idx, numIslandManifolds, islandId);
                    
                }

                if (numIslandManifolds != 0) {
                    startManifoldIndex = endManifoldIndex;
                }

                islandBodies.clearFast();
            }


            

    }

    

    public abstract static class IslandCallback {
        public abstract void processIsland(Collection<Collidable> bodies, FasterList<PersistentManifold> manifolds, int manifolds_offset, int numManifolds, int islandId);
    }

    private static final Comparator<PersistentManifold> persistentManifoldComparator = (lhs, rhs) ->
            lhs == rhs ? 0 : Integer.compare(getIslandId(lhs), getIslandId(rhs));

}
