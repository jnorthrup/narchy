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

package spacegraph.space3d.phys.dynamics.gimpact;

import spacegraph.space3d.phys.Collidable;
import spacegraph.space3d.phys.collision.CollisionAlgorithmCreateFunc;
import spacegraph.space3d.phys.collision.DefaultIntersecter;
import spacegraph.space3d.phys.collision.ManifoldResult;
import spacegraph.space3d.phys.collision.broad.*;
import spacegraph.space3d.phys.collision.narrow.PersistentManifold;
import spacegraph.space3d.phys.math.Transform;
import spacegraph.space3d.phys.math.VectorUtil;
import spacegraph.space3d.phys.shape.*;
import spacegraph.space3d.phys.util.IntArrayList;
import spacegraph.space3d.phys.util.OArrayList;
import spacegraph.util.math.Vector4f;
import spacegraph.util.math.v3;

/**
 * Collision Algorithm for GImpact Shapes.<p>
 * 
 * For register this algorithm in Bullet, proceed as following:
 * <pre>
 * CollisionDispatcher dispatcher = (CollisionDispatcher)dynamicsWorld.getDispatcher();
 * GImpactCollisionAlgorithm.registerAlgorithm(dispatcher);
 * </pre>
 * 
 * @author jezek2
 */
public class GImpactCollisionAlgorithm extends CollisionAlgorithm {

	private CollisionAlgorithm convex_algorithm;
    private PersistentManifold manifoldPtr;
	private ManifoldResult resultOut;
	private DispatcherInfo dispatchInfo;
	private int triface0;
	private int part0;
	private int triface1;
	private int part1;

	private final PairSet tmpPairset = new PairSet();
	
	private void init(CollisionAlgorithmConstructionInfo ci, Collidable body0, Collidable body1) {
		super.init(ci);
		manifoldPtr = null;
		convex_algorithm = null;
	}
	
	@Override
	public void destroy() {
		clearCache();
	}

	@Override
	public void processCollision(Collidable body0, Collidable body1, DispatcherInfo dispatchInfo, ManifoldResult resultOut) {
		clearCache();

		this.resultOut = resultOut;
		this.dispatchInfo = dispatchInfo;
		GImpactShape gimpactshape0;
		GImpactShape gimpactshape1;

		if (body0.shape().getShapeType()== BroadphaseNativeType.GIMPACT_SHAPE_PROXYTYPE)
		{
			gimpactshape0 = (GImpactShape)body0.shape();

			if( body1.shape().getShapeType()== BroadphaseNativeType.GIMPACT_SHAPE_PROXYTYPE )
			{
				gimpactshape1 = (GImpactShape)body1.shape();

				gimpact_vs_gimpact(body0,body1,gimpactshape0,gimpactshape1);
			}
			else
			{
				gimpact_vs_shape(body0,body1,gimpactshape0,body1.shape(),false);
			}

		}
		else if (body1.shape().getShapeType()== BroadphaseNativeType.GIMPACT_SHAPE_PROXYTYPE )
		{
			gimpactshape1 = (GImpactShape)body1.shape();

			gimpact_vs_shape(body1,body0,gimpactshape1,body0.shape(),true);
		}
	}
	
	private void gimpact_vs_gimpact(Collidable body0, Collidable body1, GImpactShape shape0, GImpactShape shape1) {
		if (shape0.getGImpactShapeType() == ShapeType.TRIMESH_SHAPE) {
			GImpactMeshShape meshshape0 = (GImpactMeshShape) shape0;

			int part0 = meshshape0.getMeshPartCount();

			while ((part0--) != 0) {
				gimpact_vs_gimpact(body0, body1, meshshape0.getMeshPart(part0), shape1);
			}

			this.part0 = part0;

			return;
		}

		if (shape1.getGImpactShapeType() == ShapeType.TRIMESH_SHAPE) {
			GImpactMeshShape meshshape1 = (GImpactMeshShape) shape1;
			int part1 = meshshape1.getMeshPartCount();

			while ((part1--) != 0) {
				gimpact_vs_gimpact(body0, body1, shape0, meshshape1.getMeshPart(part1));
			}

			this.part1 = part1;

			return;
		}

		Transform orgtrans0 = body0.getWorldTransform(new Transform());
		Transform orgtrans1 = body1.getWorldTransform(new Transform());

		PairSet pairset = tmpPairset;
		pairset.clear();

		gimpact_vs_gimpact_find_pairs(orgtrans0, orgtrans1, shape0, shape1, pairset);

		if (pairset.size() == 0) {
			return;
		}
		if (shape0.getGImpactShapeType() == ShapeType.TRIMESH_SHAPE_PART &&
		    shape1.getGImpactShapeType() == ShapeType.TRIMESH_SHAPE_PART) {

			GImpactMeshShape.GImpactMeshShapePart shapepart0 = (GImpactMeshShape.GImpactMeshShapePart) shape0;
			GImpactMeshShape.GImpactMeshShapePart shapepart1 = (GImpactMeshShape.GImpactMeshShapePart) shape1;

			
			
			
			
			collide_sat_triangles(body0, body1, shapepart0, shapepart1, pairset, pairset.size());
			

			return;
		}

		

		shape0.lockChildShapes();
		shape1.lockChildShapes();

		GIM_ShapeRetriever retriever0 = new GIM_ShapeRetriever(shape0);
		GIM_ShapeRetriever retriever1 = new GIM_ShapeRetriever(shape1);

		boolean child_has_transform0 = shape0.childrenHasTransform();
		boolean child_has_transform1 = shape1.childrenHasTransform();

		Transform tmpTrans = new Transform();

		int i = pairset.size();
		while ((i--) != 0) {
			Pair pair = pairset.get(i);
			triface0 = pair.index1;
			triface1 = pair.index2;
			CollisionShape colshape0 = retriever0.getChildShape(triface0);
			CollisionShape colshape1 = retriever1.getChildShape(triface1);

			if (child_has_transform0) {
				tmpTrans.mul(orgtrans0, shape0.getChildTransform(triface0));
				body0.transform(tmpTrans);
			}

			if (child_has_transform1) {
				tmpTrans.mul(orgtrans1, shape1.getChildTransform(triface1));
				body1.transform(tmpTrans);
			}

			
			convex_vs_convex_collision(body0, body1, colshape0, colshape1);

			if (child_has_transform0) {
				body0.transform(orgtrans0);
			}

			if (child_has_transform1) {
				body1.transform(orgtrans1);
			}

		}

		shape0.unlockChildShapes();
		shape1.unlockChildShapes();
	}

	public void gimpact_vs_shape(Collidable body0, Collidable body1, GImpactShape shape0, CollisionShape shape1, boolean swapped) {
		ShapeType s = shape0.getGImpactShapeType();
		if (s == ShapeType.TRIMESH_SHAPE) {
			GImpactMeshShape meshshape0 = (GImpactMeshShape) shape0;
			int part0 = meshshape0.getMeshPartCount();

			while ((part0--) != 0) {
				gimpact_vs_shape(body0,
						body1,
						meshshape0.getMeshPart(part0),
						shape1, swapped);
			}
			this.part0 = part0;

			return;
		}

		
		if (s == ShapeType.TRIMESH_SHAPE_PART &&
				shape1.getShapeType() == BroadphaseNativeType.STATIC_PLANE_PROXYTYPE) {
			GImpactMeshShape.GImpactMeshShapePart shapepart = (GImpactMeshShape.GImpactMeshShapePart) shape0;
			StaticPlaneShape planeshape = (StaticPlaneShape) shape1;
			gimpacttrimeshpart_vs_plane_collision(body0, body1, shapepart, planeshape, swapped);
			return;
		}
		

		if (shape1.isCompound()) {
			CompoundShape compoundshape = (CompoundShape) shape1;
			gimpact_vs_compoundshape(body0, body1, shape0, compoundshape, swapped);
			return;
		}
		else if (shape1.isConcave()) {
			ConcaveShape concaveshape = (ConcaveShape) shape1;
			gimpact_vs_concave(body0, body1, shape0, concaveshape, swapped);
			return;
		}

		Transform orgtrans0 = body0.getWorldTransform(new Transform());
		Transform orgtrans1 = body1.getWorldTransform(new Transform());

		IntArrayList collided_results = new IntArrayList();

		gimpact_vs_shape_find_pairs(orgtrans0, orgtrans1, shape0, shape1, collided_results);

		int cr = collided_results.size();
		if (cr == 0) {
			return;
		}
		shape0.lockChildShapes();

		GIM_ShapeRetriever retriever0 = new GIM_ShapeRetriever(shape0);

		boolean child_has_transform0 = shape0.childrenHasTransform();

		Transform tmpTrans = new Transform();

		int i = cr;

		while ((i--) != 0) {
			int child_index = collided_results.get(i);
			if (swapped) {
				triface1 = child_index;
			}
			else {
				triface0 = child_index;
			}
			CollisionShape colshape0 = retriever0.getChildShape(child_index);

			if (child_has_transform0) {
				tmpTrans.mul(orgtrans0, shape0.getChildTransform(child_index));
				body0.transform(tmpTrans);
			}

			
			if (swapped) {
				shape_vs_shape_collision(body1, body0, shape1, colshape0);
			}
			else {
				shape_vs_shape_collision(body0, body1, colshape0, shape1);
			}

			
			if (child_has_transform0) {
				body0.transform(orgtrans0);
			}

		}

		shape0.unlockChildShapes();
	}
	
	private void gimpact_vs_compoundshape(Collidable body0, Collidable body1, GImpactShape shape0, CompoundShape shape1, boolean swapped) {
		Transform orgtrans1 = body1.getWorldTransform(new Transform());
		Transform childtrans1 = new Transform();
		Transform tmpTrans = new Transform();

		int i = shape1.getNumChildShapes();
		while ((i--) != 0) {
			CollisionShape colshape1 = shape1.getChildShape(i);
			childtrans1.mul(orgtrans1, shape1.getChildTransform(i, tmpTrans));

			body1.transform(childtrans1);

			
			gimpact_vs_shape(body0, body1,
					shape0, colshape1, swapped);

			
			body1.transform(orgtrans1);
		}
	}
	
	private void gimpact_vs_concave(Collidable body0, Collidable body1, GImpactShape shape0, ConcaveShape shape1, boolean swapped) {
		
		GImpactTriangleCallback tricallback = new GImpactTriangleCallback();
		tricallback.algorithm = this;
		tricallback.body0 = body0;
		tricallback.body1 = body1;
		tricallback.gimpactshape0 = shape0;
		tricallback.swapped = swapped;
		tricallback.margin = shape1.getMargin();

		
		Transform gimpactInConcaveSpace = body1.transform; /*new Transform();

		body1.getWorldTransform(gimpactInConcaveSpace);*/
		gimpactInConcaveSpace.inverse();
		gimpactInConcaveSpace.mul(body0.transform);

		v3 minAABB = new v3(), maxAABB = new v3();
		shape0.getAabb(gimpactInConcaveSpace, minAABB, maxAABB);

		shape1.processAllTriangles(tricallback, minAABB, maxAABB);
	}
	
	/**
	 * Creates a new contact point.
	 */
    private PersistentManifold newContactManifold(Collidable body0, Collidable body1) {
		manifoldPtr = intersecter.getNewManifold(body0, body1);
		return manifoldPtr;
	}

	private void destroyConvexAlgorithm() {
		if (convex_algorithm != null) {
			
			Intersecter.freeCollisionAlgorithm(convex_algorithm);
			convex_algorithm = null;
		}
	}

	private void destroyContactManifolds() {
		if (manifoldPtr == null) return;
		intersecter.releaseManifold(manifoldPtr);
		manifoldPtr = null;
	}

	private void clearCache() {
		destroyContactManifolds();
		destroyConvexAlgorithm();

		triface0 = -1;
		part0 = -1;
		triface1 = -1;
		part1 = -1;
	}

	protected PersistentManifold getLastManifold() {
		return manifoldPtr;
	}

	/**
	 * Call before process collision.
	 */
    private void checkManifold(Collidable body0, Collidable body1) {
        if (manifoldPtr == null) {
			newContactManifold(body0, body1);
		}

        resultOut.setPersistentManifold(manifoldPtr);
	}
	
	/**
	 * Call before process collision.
	 */
    private CollisionAlgorithm newAlgorithm(Collidable body0, Collidable body1) {
		checkManifold(body0, body1);

        CollisionAlgorithm convex_algorithm = intersecter.findAlgorithm(body0, body1, manifoldPtr);
		return convex_algorithm;
	}
	
	/**
	 * Call before process collision.
	 */
    private void checkConvexAlgorithm(Collidable body0, Collidable body1) {
		if (convex_algorithm != null) return;
		convex_algorithm = newAlgorithm(body0, body1);
	}

	private void addContactPoint(Collidable body0, Collidable body1, v3 point, v3 normal, float distance, float breakingThresh) {
		resultOut.setShapeIdentifiers(part0, triface0, part1, triface1);
		checkManifold(body0, body1);
		resultOut.addContactPoint(normal, point, distance, breakingThresh);
	}

	/*
	protected void collide_gjk_triangles(CollisionObject body0, CollisionObject body1, GImpactMeshShapePart shape0, GImpactMeshShapePart shape1, IntArrayList pairs, int pair_count) {
	}
	*/
	
	private void collide_sat_triangles(Collidable body0, Collidable body1, GImpactMeshShape.GImpactMeshShapePart shape0, GImpactMeshShape.GImpactMeshShapePart shape1, PairSet pairs, int pair_count) {
		v3 tmp = new v3();

		Transform orgtrans0 = body0.transform;
		Transform orgtrans1 = body1.transform;

		PrimitiveTriangle ptri0 = new PrimitiveTriangle();
		PrimitiveTriangle ptri1 = new PrimitiveTriangle();
		TriangleContact contact_data = new TriangleContact();

		shape0.lockChildShapes();
		shape1.lockChildShapes();

		int pair_pointer = 0;

		float breakingThresh = manifoldPtr.getContactBreakingThreshold();
		while ((pair_count--) != 0) {
			
			
			
			Pair pair = pairs.get(pair_pointer++);
			triface0 = pair.index1;
			triface1 = pair.index2;

			shape0.getPrimitiveTriangle(triface0, ptri0);
			shape1.getPrimitiveTriangle(triface1, ptri1);

			
			
			

			ptri0.applyTransform(orgtrans0);
			ptri1.applyTransform(orgtrans1);

			
			ptri0.buildTriPlane();
			ptri1.buildTriPlane();

			
			if (ptri0.overlap_test_conservative(ptri1)) {
				if (ptri0.find_triangle_collision_clip_method(ptri1, contact_data)) {

					int j = contact_data.point_count;
					while ((j--) != 0) {
						tmp.x = contact_data.separating_normal.x;
						tmp.y = contact_data.separating_normal.y;
						tmp.z = contact_data.separating_normal.z;

						addContactPoint(body0, body1,
								contact_data.points[j],
								tmp,
								-contact_data.penetration_depth, breakingThresh);
					}
				}
			}

			
			
			
		}

		shape0.unlockChildShapes();
		shape1.unlockChildShapes();
	}

	private void shape_vs_shape_collision(Collidable body0, Collidable body1, CollisionShape shape0, CollisionShape shape1) {
		CollisionShape tmpShape0 = body0.shape();
		CollisionShape tmpShape1 = body1.shape();

		body0.internalSetTemporaryCollisionShape(shape0);
		body1.internalSetTemporaryCollisionShape(shape1);

        CollisionAlgorithm algor = newAlgorithm(body0, body1);
        

        resultOut.setShapeIdentifiers(part0, triface0, part1, triface1);

        algor.processCollision(body0, body1, dispatchInfo, resultOut);

        
        Intersecter.freeCollisionAlgorithm(algor);

        body0.internalSetTemporaryCollisionShape(tmpShape0);
		body1.internalSetTemporaryCollisionShape(tmpShape1);
	}
	
	private void convex_vs_convex_collision(Collidable body0, Collidable body1, CollisionShape shape0, CollisionShape shape1) {
		CollisionShape tmpShape0 = body0.shape();
		CollisionShape tmpShape1 = body1.shape();

		body0.internalSetTemporaryCollisionShape(shape0);
		body1.internalSetTemporaryCollisionShape(shape1);

		resultOut.setShapeIdentifiers(part0, triface0, part1, triface1);

		checkConvexAlgorithm(body0, body1);
		convex_algorithm.processCollision(body0, body1, dispatchInfo, resultOut);

		body0.internalSetTemporaryCollisionShape(tmpShape0);
		body1.internalSetTemporaryCollisionShape(tmpShape1);
	}

	private static void gimpact_vs_gimpact_find_pairs(Transform trans0, Transform trans1, GImpactShape shape0, GImpactShape shape1, PairSet pairset) {
		if (shape0.hasBoxSet() && shape1.hasBoxSet()) {
			GImpactBvh.find_collision(shape0.getBoxSet(), trans0, shape1.getBoxSet(), trans1, pairset);
		}
		else {
			BoxCollision.AABB boxshape0 = new BoxCollision.AABB();
			BoxCollision.AABB boxshape1 = new BoxCollision.AABB();
			int i = shape0.getNumChildShapes();

			while ((i--) != 0) {
				shape0.getChildAabb(i, trans0, boxshape0.min, boxshape0.max);

				int j = shape1.getNumChildShapes();
				while ((j--) != 0) {
					shape1.getChildAabb(i, trans1, boxshape1.min, boxshape1.max);

					if (boxshape1.has_collision(boxshape0)) {
						pairset.push_pair(i, j);
					}
				}
			}
		}
	}

	private static void gimpact_vs_shape_find_pairs(Transform trans0, Transform trans1, GImpactShape shape0, CollisionShape shape1, IntArrayList collided_primitives) {
		BoxCollision.AABB boxshape = new BoxCollision.AABB();

		if (shape0.hasBoxSet()) {
			Transform trans1to0 = new Transform();
			trans1to0.inverse(trans0);
			trans1to0.mul(trans1);

			shape1.getAabb(trans1to0, boxshape.min, boxshape.max);

			shape0.getBoxSet().boxQuery(boxshape, collided_primitives);
		}
		else {
			shape1.getAabb(trans1, boxshape.min, boxshape.max);

			BoxCollision.AABB boxshape0 = new BoxCollision.AABB();
			int i = shape0.getNumChildShapes();

			while ((i--) != 0) {
				shape0.getChildAabb(i, trans0, boxshape0.min, boxshape0.max);

				if (boxshape.has_collision(boxshape0)) {
					collided_primitives.add(i);
				}
			}
		}
	}
	
	private void gimpacttrimeshpart_vs_plane_collision(Collidable body0, Collidable body1, GImpactMeshShape.GImpactMeshShapePart shape0, StaticPlaneShape shape1, boolean swapped) {
		Transform orgtrans0 = body0.transform;
		Transform orgtrans1 = body1.transform;

		StaticPlaneShape planeshape = shape1;
		Vector4f plane = new Vector4f();
		PlaneShape.get_plane_equation_transformed(planeshape, orgtrans1, plane);

		

		BoxCollision.AABB tribox = new BoxCollision.AABB();
		shape0.getAabb(orgtrans0, tribox.min, tribox.max);
		tribox.increment_margin(planeshape.getMargin());

		if (tribox.plane_classify(plane) != PlaneIntersectionType.COLLIDE_PLANE) {
			return;
		}
		shape0.lockChildShapes();

		float margin = shape0.getMargin() + planeshape.getMargin();

		v3 vertex = new v3();

		v3 tmp = new v3();

		int vi = shape0.getVertexCount();
		float breakingThresh = manifoldPtr.getContactBreakingThreshold();
		while ((vi--) != 0) {
			shape0.getVertex(vi, vertex);
			orgtrans0.transform(vertex);

			float distance = VectorUtil.dot3(vertex, plane) - plane.w - margin;

			if (distance < 0f)
			{
				if (swapped) {
					tmp.set(-plane.x, -plane.y, -plane.z);
					addContactPoint(body1, body0, vertex, tmp, distance, breakingThresh);
				}
				else {
					tmp.set(plane.x, plane.y, plane.z);
					addContactPoint(body0, body1, vertex, tmp, distance, breakingThresh);
				}
			}
		}

		shape0.unlockChildShapes();
	}
	
	
	public void setFace0(int value) {
		triface0 = value;
	}

	public int getFace0() {
		return triface0;
	}

	public void setFace1(int value) {
		triface1 = value;
	}

	public int getFace1() {
		return triface1;
	}

	public void setPart0(int value) {
		part0 = value;
	}

	public int getPart0() {
		return part0;
	}

	public void setPart1(int value) {
		part1 = value;
	}

	public int getPart1() {
		return part1;
	}

	@Override
	public float calculateTimeOfImpact(Collidable body0, Collidable body1, DispatcherInfo dispatchInfo, ManifoldResult resultOut) {
		return 1f;
	}

	@Override
	public void getAllContactManifolds(OArrayList<PersistentManifold> manifoldArray) {
		if (manifoldPtr != null) {
			manifoldArray.add(manifoldPtr);
		}
	}
	
	

	/**
	 * Use this function for register the algorithm externally.
	 */
	public static void registerAlgorithm(DefaultIntersecter dispatcher) {
		CreateFunc createFunc = new CreateFunc();

		for (int i = 0; i< BroadphaseNativeType.MAX_BROADPHASE_COLLISION_TYPES.ordinal(); i++) {
			dispatcher.registerCollisionCreateFunc(BroadphaseNativeType.GIMPACT_SHAPE_PROXYTYPE.ordinal(), i, createFunc);
		}

		for (int i = 0; i< BroadphaseNativeType.MAX_BROADPHASE_COLLISION_TYPES.ordinal(); i++) {
			dispatcher.registerCollisionCreateFunc(i, BroadphaseNativeType.GIMPACT_SHAPE_PROXYTYPE.ordinal(), createFunc);
		}
	}
	
	public static class CreateFunc extends CollisionAlgorithmCreateFunc {

		@Override
		public CollisionAlgorithm createCollisionAlgorithm(CollisionAlgorithmConstructionInfo ci, Collidable body0, Collidable body1) {
			GImpactCollisionAlgorithm algo =new GImpactCollisionAlgorithm();
			algo.init(ci, body0, body1);
			return algo;
		}

	}

	/**
     *
     * @author jezek2
     */
	public enum ShapeType {
        COMPOUND_SHAPE,
        TRIMESH_SHAPE_PART,
        TRIMESH_SHAPE
    }
}
