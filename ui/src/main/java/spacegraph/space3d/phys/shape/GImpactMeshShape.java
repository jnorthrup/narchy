/*
 * Java port of Bullet (c) 2008 Martin Dvorak <jezek2@advel.cz>
 *
 * This source file is part of GIMPACT Library.
 *
 * For the latest info, see http:
 *
 * Copyright (c) 2007 Francisco Leon Najera. C.C. 80087371.
 * email: projectileman@yahoo.com
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

import jcog.math.v3;
import spacegraph.space3d.phys.dynamics.gimpact.*;
import spacegraph.space3d.phys.math.Transform;
import spacegraph.space3d.phys.util.IntArrayList;
import spacegraph.space3d.phys.util.OArrayList;

import static spacegraph.space3d.phys.dynamics.gimpact.GImpactCollisionAlgorithm.ShapeType;
import static spacegraph.space3d.phys.dynamics.gimpact.GImpactCollisionAlgorithm.ShapeType.TRIMESH_SHAPE;

/**
 *
 * @author jezek2
 */
public class GImpactMeshShape extends GImpactShape {
	
	private final OArrayList<GImpactMeshShapePart> mesh_parts = new OArrayList<>();

	public GImpactMeshShape(StridingMeshInterface meshInterface) {
		buildMeshParts(meshInterface);
	}
	
	public int getMeshPartCount() {
		return mesh_parts.size();
	}

	public GImpactMeshShapePart getMeshPart(int index) {
        return mesh_parts.get(index);
        
    }

	@Override
	public void setLocalScaling(v3 scaling) {
		localScaling.set(scaling);

        var i = mesh_parts.size();
		while ((i--) != 0) {

            var part = mesh_parts.get(i);
			part.setLocalScaling(scaling);
		}

		needs_update = true;
	}

	@Override
	public GImpactShape setMargin(float margin) {
		collisionMargin = margin;

        var i = mesh_parts.size();
		while ((i--) != 0) {

            var part = mesh_parts.get(i);
			part.setMargin(margin);
		}

		needs_update = true;
        return null;
    }

	@Override
	public void postUpdate() {
        var i = mesh_parts.size();
		while ((i--) != 0) {

            var part = mesh_parts.get(i);
			part.postUpdate();
		}

		needs_update = true;
	}

	@Override
	public void calculateLocalInertia(float mass, v3 inertia) {
		
		inertia.set(0f, 0f, 0f);

        var i = getMeshPartCount();
        var partmass = mass / i;

        var partinertia = new v3();

		while ((i--) != 0) {
			getMeshPart(i).calculateLocalInertia(partmass, partinertia);
			inertia.add(partinertia);
		}

		
		
		
		
		
		
		
		
		
		
		
		
		
		
	}
	
	@Override
	PrimitiveManagerBase getPrimitiveManager() {
		assert (false);
		return null;
	}

	@Override
	public int getNumChildShapes() {
		assert (false);
		return 0;
	}

	@Override
	public boolean childrenHasTransform() {
		assert (false);
		return false;
	}

	@Override
	public boolean needsRetrieveTriangles() {
		assert (false);
		return false;
	}

	@Override
	public boolean needsRetrieveTetrahedrons() {
		assert (false);
		return false;
	}

	@Override
	public void getBulletTriangle(int prim_index, TriangleShapeEx triangle) {
		assert (false);
	}

	@Override
    public void getBulletTetrahedron(int prim_index, TetrahedronShapeEx tetrahedron) {
		assert (false);
	}

	@Override
	public void lockChildShapes() {
		assert (false);
	}

	@Override
	public void unlockChildShapes() {
		assert (false);
	}

	@Override
	public void getChildAabb(int child_index, Transform t, v3 aabbMin, v3 aabbMax) {
		assert (false);
	}

	@Override
	public CollisionShape getChildShape(int index) {
		assert (false);
		return null;
	}

	@Override
	public Transform getChildTransform(int index) {
		assert (false);
		return null;
	}

	@Override
	public void setChildTransform(int index, Transform transform) {
		assert (false);
	}

	@Override
    public ShapeType getGImpactShapeType() {
		return TRIMESH_SHAPE;
	}

	@Override
	public String getName() {
		return "GImpactMesh";
	}

    @Override
	public void processAllTriangles(TriangleCallback callback, v3 aabbMin, v3 aabbMax) {
        var i = mesh_parts.size();
		while ((i--) != 0) {
            
            mesh_parts.get(i).processAllTriangles(callback, aabbMin, aabbMax);
		}
	}
	
	private void buildMeshParts(StridingMeshInterface meshInterface) {
		for (var i = 0; i<meshInterface.getNumSubParts(); i++) {
            var newpart = new GImpactMeshShapePart(meshInterface, i);
			mesh_parts.add(newpart);
		}
	}

	@Override
	protected void calcLocalAABB() {
        var tmpAABB = new BoxCollision.AABB();

		localAABB.invalidate();
        var i = mesh_parts.size();
		while ((i--) != 0) {
            
            mesh_parts.get(i).updateBound();
            
            localAABB.merge(mesh_parts.get(i).getLocalBox(tmpAABB));
		}
	}

	/**
     * This class manages a sub part of a mesh supplied by the StridingMeshInterface interface.<p>
     *
     * - Simply create this shape by passing the StridingMeshInterface to the constructor
     *   GImpactMeshShapePart, then you must call updateBound() after creating the mesh<br>
     * - When making operations with this shape, you must call <b>lock</b> before accessing
     *   to the trimesh primitives, and then call <b>unlock</b><br>
     * - You can handle deformable meshes with this shape, by calling postUpdate() every time
     *   when changing the mesh vertices.
     *
     * @author jezek2
     */
    public static class GImpactMeshShapePart extends GImpactShape {

        TrimeshPrimitiveManager primitive_manager = new TrimeshPrimitiveManager();

        private final IntArrayList collided = new IntArrayList();

        public GImpactMeshShapePart() {
            box_set.setPrimitiveManager(primitive_manager);
        }

        GImpactMeshShapePart(StridingMeshInterface meshInterface, int part) {
            primitive_manager.meshInterface = meshInterface;
            primitive_manager.part = part;
            box_set.setPrimitiveManager(primitive_manager);
        }

        @Override
        public boolean childrenHasTransform() {
            return false;
        }

        @Override
        public void lockChildShapes() {
            var dummymanager = (TrimeshPrimitiveManager) box_set.getPrimitiveManager();
            dummymanager.lock();
        }

        @Override
        public void unlockChildShapes() {
            var dummymanager = (TrimeshPrimitiveManager) box_set.getPrimitiveManager();
            dummymanager.unlock();
        }

        @Override
        public int getNumChildShapes() {
            return primitive_manager.get_primitive_count();
        }

        @Override
        public CollisionShape getChildShape(int index) {
            assert (false);
            return null;
        }

        @Override
        public Transform getChildTransform(int index) {
            assert (false);
            return null;
        }

        @Override
        public void setChildTransform(int index, Transform transform) {
            assert (false);
        }

        @Override
        PrimitiveManagerBase getPrimitiveManager() {
            return primitive_manager;
        }

        TrimeshPrimitiveManager getTrimeshPrimitiveManager() {
            return primitive_manager;
        }

        @Override
        public void calculateLocalInertia(float mass, v3 inertia) {
            lockChildShapes();

            
            
            inertia.set(0f, 0f, 0f);

            var i = getVertexCount();
            var pointmass = mass / i;

            var pointintertia = new v3();

            while ((i--) != 0) {
                getVertex(i, pointintertia);
                GImpactMassUtil.get_point_inertia(pointintertia, pointmass, pointintertia);
                inertia.add(pointintertia);
            }

            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            unlockChildShapes();
        }

        @Override
        public String getName() {
            return "GImpactMeshShapePart";
        }

        @Override
        public ShapeType getGImpactShapeType() {
            return ShapeType.TRIMESH_SHAPE_PART;
        }

        @Override
        public boolean needsRetrieveTriangles() {
            return true;
        }

        @Override
        public boolean needsRetrieveTetrahedrons() {
            return false;
        }

        @Override
        public void getBulletTriangle(int prim_index, TriangleShapeEx triangle) {
            primitive_manager.get_bullet_triangle(prim_index, triangle);
        }

        @Override
        public void getBulletTetrahedron(int prim_index, TetrahedronShapeEx tetrahedron) {
            assert (false);
        }

        public int getVertexCount() {
            return primitive_manager.get_vertex_count();
        }

        public void getVertex(int vertex_index, v3 vertex) {
            primitive_manager.get_vertex(vertex_index, vertex);
        }

        @Override
        public GImpactShape setMargin(float margin) {
            primitive_manager.margin = margin;
            super.postUpdate();
            return null;
        }

        @Override
        public float getMargin() {
            return primitive_manager.margin;
        }

        @Override
        public void setLocalScaling(v3 scaling) {
            primitive_manager.scale.set(scaling);
            super.postUpdate();
        }

        @Override
        public v3 getLocalScaling(v3 out) {
            out.set(primitive_manager.scale);
            return out;
        }

        int getPart() {
            return primitive_manager.part;
        }

        @Override
        public void processAllTriangles(TriangleCallback callback, v3 aabbMin, v3 aabbMax) {
            lockChildShapes();
            var box = new BoxCollision.AABB();
            box.min.set(aabbMin);
            box.max.set(aabbMax);

            collided.clear();
            box_set.boxQuery(box, collided);

            if (collided.size() == 0) {
                unlockChildShapes();
                return;
            }

            var part = getPart();
            var triangle = new PrimitiveTriangle();
            var i = collided.size();
            while ((i--) != 0) {
                getPrimitiveTriangle(collided.get(i), triangle);
                callback.processTriangle(triangle.vertices, part, collided.get(i));
            }
            unlockChildShapes();
        }

    }
}
