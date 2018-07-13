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

import jcog.data.list.FasterList;
import spacegraph.space3d.phys.math.AabbUtil2;
import spacegraph.space3d.phys.math.MiscUtil;
import spacegraph.space3d.phys.math.VectorUtil;
import spacegraph.util.math.v3;

import java.io.Serializable;



/**
 * OptimizedBvh store an AABB tree that can be quickly traversed on CPU (and SPU, GPU in future).
 * 
 * @author jezek2
 */
public class OptimizedBvh implements Serializable {

	private static final long serialVersionUID = 1L;

	
	
	private static final boolean DEBUG_TREE_BUILDING = false;
	private static int gStackDepth;
	private static int gMaxStackDepth;
	
	private static int maxIterations;
	
	
	private static final int MAX_SUBTREE_SIZE_IN_BYTES = 2048;

	
	
	public static final int MAX_NUM_PARTS_IN_BITS = 10;

	

	private final FasterList<OptimizedBvhNode> leafNodes = new FasterList<>();
	private final FasterList<OptimizedBvhNode> contiguousNodes = new FasterList<>();

	private final QuantizedBvhNodes quantizedLeafNodes = new QuantizedBvhNodes();
	private final QuantizedBvhNodes quantizedContiguousNodes = new QuantizedBvhNodes();
	
	private int curNodeIndex;

	
	private boolean useQuantization;
	private final v3 bvhAabbMin = new v3();
	private final v3 bvhAabbMax = new v3();
	private final v3 bvhQuantization = new v3();
	
	private final TraversalMode traversalMode = TraversalMode.STACKLESS;
	private final FasterList<BvhSubtreeInfo> SubtreeHeaders = new FasterList<>();
	
	private int subtreeHeaderCount;

	
	
	private void setInternalNodeAabbMin(int nodeIndex, v3 aabbMin) {
		if (useQuantization) {
			quantizedContiguousNodes.setQuantizedAabbMin(nodeIndex, quantizeWithClamp(aabbMin));
		}
		else {
			
			contiguousNodes.get(nodeIndex).aabbMinOrg.set(aabbMin);
		}
	}

	private void setInternalNodeAabbMax(int nodeIndex, v3 aabbMax) {
		if (useQuantization) {
			quantizedContiguousNodes.setQuantizedAabbMax(nodeIndex, quantizeWithClamp(aabbMax));
		}
		else {
			
			contiguousNodes.get(nodeIndex).aabbMaxOrg.set(aabbMax);
		}
	}

	private v3 getAabbMin(int nodeIndex) {
		if (useQuantization) {
			v3 tmp = new v3();
			unQuantize(tmp, quantizedLeafNodes.getQuantizedAabbMin(nodeIndex));
			return tmp;
		}

		
		
		return leafNodes.get(nodeIndex).aabbMinOrg;
	}

	private v3 getAabbMax(int nodeIndex) {
		if (useQuantization) {
			v3 tmp = new v3();
			unQuantize(tmp, quantizedLeafNodes.getQuantizedAabbMax(nodeIndex));
			return tmp;
		}

		
		
		return leafNodes.get(nodeIndex).aabbMaxOrg;
	}

	private void setQuantizationValues(v3 aabbMin, v3 aabbMax) {
		setQuantizationValues(aabbMin, aabbMax, 1f);
	}

	private void setQuantizationValues(v3 aabbMin, v3 aabbMax, float quantizationMargin) {
		
		v3 clampValue = new v3();
		clampValue.set(quantizationMargin,quantizationMargin,quantizationMargin);
		bvhAabbMin.sub(aabbMin, clampValue);
		bvhAabbMax.add(aabbMax, clampValue);
		v3 aabbSize = new v3();
		aabbSize.sub(bvhAabbMax, bvhAabbMin);
		bvhQuantization.set(65535f, 65535f, 65535f);
		VectorUtil.div(bvhQuantization, bvhQuantization, aabbSize);
	}

	private void setInternalNodeEscapeIndex(int nodeIndex, int escapeIndex) {
		if (useQuantization) {
			quantizedContiguousNodes.setEscapeIndexOrTriangleIndex(nodeIndex, -escapeIndex);
		}
		else {
			
			contiguousNodes.get(nodeIndex).escapeIndex = escapeIndex;
		}
	}

	private void mergeInternalNodeAabb(int nodeIndex, v3 newAabbMin, v3 newAabbMax) {
		if (useQuantization) {

			long quantizedAabbMin = quantizeWithClamp(newAabbMin);
			long quantizedAabbMax = quantizeWithClamp(newAabbMax);
			for (int i = 0; i < 3; i++) {
				int n = QuantizedBvhNodes.getCoord(quantizedAabbMin, i);
				if (quantizedContiguousNodes.getQuantizedAabbMin(nodeIndex, i) > n) {
					quantizedContiguousNodes.setQuantizedAabbMin(nodeIndex, i, n);
				}

				int m = QuantizedBvhNodes.getCoord(quantizedAabbMax, i);
				if (quantizedContiguousNodes.getQuantizedAabbMax(nodeIndex, i) < m) {
					quantizedContiguousNodes.setQuantizedAabbMax(nodeIndex, i, m);
				}
			}
		}
		else {
			
			
			OptimizedBvhNode cn = contiguousNodes.get(nodeIndex);
			VectorUtil.setMin(cn.aabbMinOrg, newAabbMin);
			
			VectorUtil.setMax(cn.aabbMaxOrg, newAabbMax);
		}
	}

	private void swapLeafNodes(int i, int splitIndex) {
		if (useQuantization) {
			quantizedLeafNodes.swap(i, splitIndex);
		}
		else {
			
			
			OptimizedBvhNode tmp = leafNodes.get(i);
			
			leafNodes.setFast(i, leafNodes.get(splitIndex));
			leafNodes.setFast(splitIndex, tmp);
		}
	}

	private void assignInternalNodeFromLeafNode(int internalNode, int leafNodeIndex) {
		if (useQuantization) {
			quantizedContiguousNodes.set(internalNode, quantizedLeafNodes, leafNodeIndex);
		}
		else {
			
			
			contiguousNodes.get(internalNode).set(leafNodes.get(leafNodeIndex));
		}
	}

	private static class NodeTriangleCallback extends InternalTriangleIndexCallback {
		final FasterList<OptimizedBvhNode> triangleNodes;

		NodeTriangleCallback(FasterList<OptimizedBvhNode> triangleNodes) {
			this.triangleNodes = triangleNodes;
		}

		private final v3 aabbMin = new v3(), aabbMax = new v3();

		@Override
        public void internalProcessTriangleIndex(v3[] triangle, int partId, int triangleIndex) {
			OptimizedBvhNode node = new OptimizedBvhNode();
			aabbMin.set(1e30f, 1e30f, 1e30f);
			aabbMax.set(-1e30f, -1e30f, -1e30f);
			VectorUtil.setMin(aabbMin, triangle[0]);
			VectorUtil.setMax(aabbMax, triangle[0]);
			VectorUtil.setMin(aabbMin, triangle[1]);
			VectorUtil.setMax(aabbMax, triangle[1]);
			VectorUtil.setMin(aabbMin, triangle[2]);
			VectorUtil.setMax(aabbMax, triangle[2]);

			
			node.aabbMinOrg.set(aabbMin);
			node.aabbMaxOrg.set(aabbMax);

			node.escapeIndex = -1;

			
			node.subPart = partId;
			node.triangleIndex = triangleIndex;
			triangleNodes.add(node);
		}
	}

	private static class QuantizedNodeTriangleCallback extends InternalTriangleIndexCallback {
		

		final QuantizedBvhNodes triangleNodes;
		final OptimizedBvh optimizedTree;

		QuantizedNodeTriangleCallback(QuantizedBvhNodes triangleNodes, OptimizedBvh tree) {
			this.triangleNodes = triangleNodes;
			this.optimizedTree = tree;
		}

		@Override
        public void internalProcessTriangleIndex(v3[] triangle, int partId, int triangleIndex) {
			
			assert (partId < (1 << MAX_NUM_PARTS_IN_BITS));
			assert (triangleIndex < (1 << (31 - MAX_NUM_PARTS_IN_BITS)));
			
			assert (triangleIndex >= 0);

			int nodeId = triangleNodes.add();
			v3 aabbMin = new v3(), aabbMax = new v3();
			aabbMin.set(1e30f, 1e30f, 1e30f);
			aabbMax.set(-1e30f, -1e30f, -1e30f);
			VectorUtil.setMin(aabbMin, triangle[0]);
			VectorUtil.setMax(aabbMax, triangle[0]);
			VectorUtil.setMin(aabbMin, triangle[1]);
			VectorUtil.setMax(aabbMax, triangle[1]);
			VectorUtil.setMin(aabbMin, triangle[2]);
			VectorUtil.setMax(aabbMax, triangle[2]);

			
			final float MIN_AABB_DIMENSION = 0.002f;
			final float MIN_AABB_HALF_DIMENSION = 0.001f;
			if (aabbMax.x - aabbMin.x < MIN_AABB_DIMENSION) {
				aabbMax.x = (aabbMax.x + MIN_AABB_HALF_DIMENSION);
				aabbMin.x = (aabbMin.x - MIN_AABB_HALF_DIMENSION);
			}
			if (aabbMax.y - aabbMin.y < MIN_AABB_DIMENSION) {
				aabbMax.y = (aabbMax.y + MIN_AABB_HALF_DIMENSION);
				aabbMin.y = (aabbMin.y - MIN_AABB_HALF_DIMENSION);
			}
			if (aabbMax.z - aabbMin.z < MIN_AABB_DIMENSION) {
				aabbMax.z = (aabbMax.z + MIN_AABB_HALF_DIMENSION);
				aabbMin.z = (aabbMin.z - MIN_AABB_HALF_DIMENSION);
			}

			triangleNodes.setQuantizedAabbMin(nodeId, optimizedTree.quantizeWithClamp(aabbMin));
			triangleNodes.setQuantizedAabbMax(nodeId, optimizedTree.quantizeWithClamp(aabbMax));

			triangleNodes.setEscapeIndexOrTriangleIndex(nodeId, (partId << (31 - MAX_NUM_PARTS_IN_BITS)) | triangleIndex);
		}
	}

	public void build(StridingMeshInterface triangles, boolean useQuantizedAabbCompression, v3 _aabbMin, v3 _aabbMax) {
		this.useQuantization = useQuantizedAabbCompression;

		

		int numLeafNodes = 0;

		if (useQuantization) {
			
			setQuantizationValues(_aabbMin, _aabbMax);

			QuantizedNodeTriangleCallback callback = new QuantizedNodeTriangleCallback(quantizedLeafNodes, this);

			triangles.internalProcessAllTriangles(callback, bvhAabbMin, bvhAabbMax);

			
			numLeafNodes = quantizedLeafNodes.size();

			quantizedContiguousNodes.resize(2 * numLeafNodes);
		}
		else {
			NodeTriangleCallback callback = new NodeTriangleCallback(leafNodes);

			v3 aabbMin = new v3();
			aabbMin.set(-1e30f, -1e30f, -1e30f);
			v3 aabbMax = new v3();
			aabbMax.set(1e30f, 1e30f, 1e30f);

			triangles.internalProcessAllTriangles(callback, aabbMin, aabbMax);

			
			numLeafNodes = leafNodes.size();

			
			
			MiscUtil.resize(contiguousNodes, 2 * numLeafNodes, OptimizedBvhNode.class);
		}

		curNodeIndex = 0;

		buildTree(0, numLeafNodes);

		
		if (useQuantization && SubtreeHeaders.isEmpty()) {
			BvhSubtreeInfo subtree = new BvhSubtreeInfo();
			SubtreeHeaders.add(subtree);

			subtree.setAabbFromQuantizeNode(quantizedContiguousNodes, 0);
			subtree.rootNodeIndex = 0;
			subtree.subtreeSize = quantizedContiguousNodes.isLeafNode(0) ? 1 : quantizedContiguousNodes.getEscapeIndex(0);
		}

		
		subtreeHeaderCount = SubtreeHeaders.size();

		
		quantizedLeafNodes.clear();
		leafNodes.clearFast();
	}

	public void refit(StridingMeshInterface meshInterface) {
		if (useQuantization) {
			
			v3 aabbMin = new v3(), aabbMax = new v3();
			meshInterface.calculateAabbBruteForce(aabbMin, aabbMax);

			setQuantizationValues(aabbMin, aabbMax);

			updateBvhNodes(meshInterface, 0, curNodeIndex, 0);

			

			int i;
			for (i = 0; i < SubtreeHeaders.size(); i++) {
				
				BvhSubtreeInfo subtree = SubtreeHeaders.get(i);
				subtree.setAabbFromQuantizeNode(quantizedContiguousNodes, subtree.rootNodeIndex);
			}

		}
		else {
			
			build(meshInterface, false, null, null);
		}
	}

	public static void refitPartial(StridingMeshInterface meshInterface, v3 aabbMin, v3 aabbMax) {
		throw new UnsupportedOperationException();


































	}

	private void updateBvhNodes(StridingMeshInterface meshInterface, int firstNode, int endNode, int index) {
		assert (useQuantization);

		int curNodeSubPart = -1;

		v3[] triangleVerts/*[3]*/ = { new v3(), new v3(), new v3() };
		v3 aabbMin = new v3(), aabbMax = new v3();
		v3 meshScaling = meshInterface.getScaling(new v3());

		VertexData data = null;

		for (int i = endNode - 1; i >= firstNode; i--) {
			QuantizedBvhNodes curNodes = quantizedContiguousNodes;
			int curNodeId = i;

			if (curNodes.isLeafNode(curNodeId)) {
				
				int nodeSubPart = curNodes.getPartId(curNodeId);
				int nodeTriangleIndex = curNodes.getTriangleIndex(curNodeId);
				if (nodeSubPart != curNodeSubPart) {
					if (curNodeSubPart >= 0) {
						meshInterface.unLockReadOnlyVertexBase(curNodeSubPart);
					}
					data = meshInterface.getLockedReadOnlyVertexIndexBase(nodeSubPart);
				}
				

				data.getTriangle(nodeTriangleIndex*3, meshScaling, triangleVerts);

				aabbMin.set(1e30f, 1e30f, 1e30f);
				aabbMax.set(-1e30f, -1e30f, -1e30f);
				VectorUtil.setMin(aabbMin, triangleVerts[0]);
				VectorUtil.setMax(aabbMax, triangleVerts[0]);
				VectorUtil.setMin(aabbMin, triangleVerts[1]);
				VectorUtil.setMax(aabbMax, triangleVerts[1]);
				VectorUtil.setMin(aabbMin, triangleVerts[2]);
				VectorUtil.setMax(aabbMax, triangleVerts[2]);

				curNodes.setQuantizedAabbMin(curNodeId, quantizeWithClamp(aabbMin));
				curNodes.setQuantizedAabbMax(curNodeId, quantizeWithClamp(aabbMax));
			}
			else {
				

				
				int leftChildNodeId = i + 1;

				int rightChildNodeId = quantizedContiguousNodes.isLeafNode(leftChildNodeId) ? i + 2 : i + 1 + quantizedContiguousNodes.getEscapeIndex(leftChildNodeId);

				for (int i2 = 0; i2 < 3; i2++) {
					curNodes.setQuantizedAabbMin(curNodeId, i2, quantizedContiguousNodes.getQuantizedAabbMin(leftChildNodeId, i2));
					if (curNodes.getQuantizedAabbMin(curNodeId, i2) > quantizedContiguousNodes.getQuantizedAabbMin(rightChildNodeId, i2)) {
						curNodes.setQuantizedAabbMin(curNodeId, i2, quantizedContiguousNodes.getQuantizedAabbMin(rightChildNodeId, i2));
					}

					curNodes.setQuantizedAabbMax(curNodeId, i2, quantizedContiguousNodes.getQuantizedAabbMax(leftChildNodeId, i2));
					if (curNodes.getQuantizedAabbMax(curNodeId, i2) < quantizedContiguousNodes.getQuantizedAabbMax(rightChildNodeId, i2)) {
						curNodes.setQuantizedAabbMax(curNodeId, i2, quantizedContiguousNodes.getQuantizedAabbMax(rightChildNodeId, i2));
					}
				}
			}
		}

		if (curNodeSubPart >= 0) {
			meshInterface.unLockReadOnlyVertexBase(curNodeSubPart);
		}
	}

	private void buildTree(int startIndex, int endIndex) {
		
		if (DEBUG_TREE_BUILDING) {
			gStackDepth++;
			if (gStackDepth > gMaxStackDepth) {
				gMaxStackDepth = gStackDepth;
			}
		}
		

		int splitAxis, splitIndex, i;
		int numIndices = endIndex - startIndex;
		int curIndex = curNodeIndex;

		assert (numIndices > 0);

		if (numIndices == 1) {
			
			if (DEBUG_TREE_BUILDING) {
				gStackDepth--;
			}
			

			assignInternalNodeFromLeafNode(curNodeIndex, startIndex);

			curNodeIndex++;
			return;
		}
		

		splitAxis = calcSplittingAxis(startIndex, endIndex);

		splitIndex = sortAndCalcSplittingIndex(startIndex, endIndex, splitAxis);

		int internalNodeIndex = curNodeIndex;

		v3 tmp1 = new v3();
		tmp1.set(-1e30f, -1e30f, -1e30f);
		setInternalNodeAabbMax(curNodeIndex, tmp1);
		v3 tmp2 = new v3();
		tmp2.set(1e30f, 1e30f, 1e30f);
		setInternalNodeAabbMin(curNodeIndex, tmp2);

		for (i = startIndex; i < endIndex; i++) {
			mergeInternalNodeAabb(curNodeIndex, getAabbMin(i), getAabbMax(i));
		}

		curNodeIndex++;

		

		int leftChildNodexIndex = curNodeIndex;

		
		buildTree(startIndex, splitIndex);

		int rightChildNodexIndex = curNodeIndex;
		
		buildTree(splitIndex, endIndex);

		
		if (DEBUG_TREE_BUILDING) {
			gStackDepth--;
		}
		

		int escapeIndex = curNodeIndex - curIndex;

		if (useQuantization) {
			
			int sizeQuantizedNode = QuantizedBvhNodes.getNodeSize();
			int treeSizeInBytes = escapeIndex * sizeQuantizedNode;
			if (treeSizeInBytes > MAX_SUBTREE_SIZE_IN_BYTES) {
				updateSubtreeHeaders(leftChildNodexIndex, rightChildNodexIndex);
			}
		}

		setInternalNodeEscapeIndex(internalNodeIndex, escapeIndex);
	}

	private static boolean testQuantizedAabbAgainstQuantizedAabb(long aabbMin1, long aabbMax1, long aabbMin2, long aabbMax2) {
		int aabbMin1_0 = QuantizedBvhNodes.getCoord(aabbMin1, 0);
		int aabbMin1_1 = QuantizedBvhNodes.getCoord(aabbMin1, 1);
		int aabbMin1_2 = QuantizedBvhNodes.getCoord(aabbMin1, 2);

		int aabbMax1_0 = QuantizedBvhNodes.getCoord(aabbMax1, 0);
		int aabbMax1_1 = QuantizedBvhNodes.getCoord(aabbMax1, 1);
		int aabbMax1_2 = QuantizedBvhNodes.getCoord(aabbMax1, 2);

		int aabbMin2_0 = QuantizedBvhNodes.getCoord(aabbMin2, 0);
		int aabbMin2_1 = QuantizedBvhNodes.getCoord(aabbMin2, 1);
		int aabbMin2_2 = QuantizedBvhNodes.getCoord(aabbMin2, 2);

		int aabbMax2_0 = QuantizedBvhNodes.getCoord(aabbMax2, 0);
		int aabbMax2_1 = QuantizedBvhNodes.getCoord(aabbMax2, 1);
		int aabbMax2_2 = QuantizedBvhNodes.getCoord(aabbMax2, 2);

		boolean overlap = true;
		overlap = !(aabbMin1_0 > aabbMax2_0 || aabbMax1_0 < aabbMin2_0) && overlap;
		overlap = !(aabbMin1_2 > aabbMax2_2 || aabbMax1_2 < aabbMin2_2) && overlap;
		overlap = !(aabbMin1_1 > aabbMax2_1 || aabbMax1_1 < aabbMin2_1) && overlap;
		return overlap;
	}

	private void updateSubtreeHeaders(int leftChildNodexIndex, int rightChildNodexIndex) {
		assert (useQuantization);

		
		int leftSubTreeSize = quantizedContiguousNodes.isLeafNode(leftChildNodexIndex) ? 1 : quantizedContiguousNodes.getEscapeIndex(leftChildNodexIndex);
		int leftSubTreeSizeInBytes = leftSubTreeSize * QuantizedBvhNodes.getNodeSize();

		
		int rightSubTreeSize = quantizedContiguousNodes.isLeafNode(rightChildNodexIndex) ? 1 : quantizedContiguousNodes.getEscapeIndex(rightChildNodexIndex);
		int rightSubTreeSizeInBytes = rightSubTreeSize * QuantizedBvhNodes.getNodeSize();

		if (leftSubTreeSizeInBytes <= MAX_SUBTREE_SIZE_IN_BYTES) {
			BvhSubtreeInfo subtree = new BvhSubtreeInfo();
			SubtreeHeaders.add(subtree);

			subtree.setAabbFromQuantizeNode(quantizedContiguousNodes, leftChildNodexIndex);
			subtree.rootNodeIndex = leftChildNodexIndex;
			subtree.subtreeSize = leftSubTreeSize;
		}

		if (rightSubTreeSizeInBytes <= MAX_SUBTREE_SIZE_IN_BYTES) {
			BvhSubtreeInfo subtree = new BvhSubtreeInfo();
			SubtreeHeaders.add(subtree);

			subtree.setAabbFromQuantizeNode(quantizedContiguousNodes, rightChildNodexIndex);
			subtree.rootNodeIndex = rightChildNodexIndex;
			subtree.subtreeSize = rightSubTreeSize;
		}

		
		subtreeHeaderCount = SubtreeHeaders.size();
	}

	private int sortAndCalcSplittingIndex(int startIndex, int endIndex, int splitAxis) {
		int i;
		int splitIndex = startIndex;
		int numIndices = endIndex - startIndex;
		float splitValue;

		v3 means = new v3();
		means.set(0f, 0f, 0f);
		v3 center = new v3();
		for (i = startIndex; i < endIndex; i++) {
			center.add(getAabbMax(i), getAabbMin(i));
			center.scale(0.5f);
			means.add(center);
		}
		means.scale(1f / (float) numIndices);

		splitValue = VectorUtil.coord(means, splitAxis);

		
		for (i = startIndex; i < endIndex; i++) {
			
			center.add(getAabbMax(i), getAabbMin(i));
			center.scale(0.5f);

			if (VectorUtil.coord(center, splitAxis) > splitValue) {
				
				swapLeafNodes(i, splitIndex);
				splitIndex++;
			}
		}

		
		
		
		

		
		

		
		int rangeBalancedIndices = numIndices / 3;
		boolean unbalanced = ((splitIndex <= (startIndex + rangeBalancedIndices)) || (splitIndex >= (endIndex - 1 - rangeBalancedIndices)));

		if (unbalanced) {
			splitIndex = startIndex + (numIndices >> 1);
		}

		boolean unbal = (splitIndex == startIndex) || (splitIndex == (endIndex));
		assert (!unbal);

		return splitIndex;
	}

	private int calcSplittingAxis(int startIndex, int endIndex) {
		int i;

		v3 means = new v3();
		means.set(0f, 0f, 0f);
		v3 variance = new v3();
		variance.set(0f, 0f, 0f);
		int numIndices = endIndex - startIndex;

		v3 center = new v3();
		for (i = startIndex; i < endIndex; i++) {
			center.add(getAabbMax(i), getAabbMin(i));
			center.scale(0.5f);
			means.add(center);
		}
		means.scale(1f / (float) numIndices);

		v3 diff2 = new v3();
		for (i = startIndex; i < endIndex; i++) {
			center.add(getAabbMax(i), getAabbMin(i));
			center.scale(0.5f);
			diff2.sub(center, means);
			
			VectorUtil.mul(diff2, diff2, diff2);
			variance.add(diff2);
		}
		variance.scale(1f / ((float) numIndices - 1));

		return VectorUtil.maxAxis(variance);
	}

	public void reportAabbOverlappingNodex(NodeOverlapCallback nodeCallback, v3 aabbMin, v3 aabbMax) {
		

		if (useQuantization) {
			
			long quantizedQueryAabbMin;
			long quantizedQueryAabbMax;
			quantizedQueryAabbMin = quantizeWithClamp(aabbMin);
			quantizedQueryAabbMax = quantizeWithClamp(aabbMax);

			
			switch (traversalMode) {
				case STACKLESS:
					walkStacklessQuantizedTree(nodeCallback, quantizedQueryAabbMin, quantizedQueryAabbMax, 0, curNodeIndex);
					break;





				case RECURSIVE:
					walkRecursiveQuantizedTreeAgainstQueryAabb(quantizedContiguousNodes, 0, nodeCallback, quantizedQueryAabbMin, quantizedQueryAabbMax);
					break;

				default:
					assert (false); 
			}
		}
		else {
			walkStacklessTree(nodeCallback, aabbMin, aabbMax);
		}
	}

	private void walkStacklessTree(NodeOverlapCallback nodeCallback, v3 aabbMin, v3 aabbMax) {
		assert (!useQuantization);

		
		OptimizedBvhNode rootNode = null;
		int rootNode_index = 0;

		int escapeIndex, curIndex = 0;
		int walkIterations = 0;
		boolean isLeafNode;
		
		
		boolean aabbOverlap;

		while (curIndex < curNodeIndex) {
			
			assert (walkIterations < curNodeIndex);

			walkIterations++;

			
			rootNode = contiguousNodes.get(rootNode_index);

			aabbOverlap = AabbUtil2.testAabbAgainstAabb2(aabbMin, aabbMax, rootNode.aabbMinOrg, rootNode.aabbMaxOrg);
			isLeafNode = (rootNode.escapeIndex == -1);

			
			if (isLeafNode && (aabbOverlap/* != 0*/)) {
				nodeCallback.processNode(rootNode.subPart, rootNode.triangleIndex);
			}

			rootNode = null;

			
			if ((aabbOverlap/* != 0*/) || isLeafNode) {
				rootNode_index++;
				curIndex++;
			}
			else {
				
				escapeIndex = /*rootNode*/ contiguousNodes.get(rootNode_index).escapeIndex;
				rootNode_index += escapeIndex;
				curIndex += escapeIndex;
			}
		}
		if (maxIterations < walkIterations) {
			maxIterations = walkIterations;
		}
	}

	private void walkRecursiveQuantizedTreeAgainstQueryAabb(QuantizedBvhNodes currentNodes, int currentNodeId, NodeOverlapCallback nodeCallback, long quantizedQueryAabbMin, long quantizedQueryAabbMax) {
		assert (useQuantization);

		boolean isLeafNode;
		boolean aabbOverlap;

		aabbOverlap = testQuantizedAabbAgainstQuantizedAabb(quantizedQueryAabbMin, quantizedQueryAabbMax, currentNodes.getQuantizedAabbMin(currentNodeId), currentNodes.getQuantizedAabbMax(currentNodeId));
		isLeafNode = currentNodes.isLeafNode(currentNodeId);

		if (aabbOverlap) {
			if (isLeafNode) {
				nodeCallback.processNode(currentNodes.getPartId(currentNodeId), currentNodes.getTriangleIndex(currentNodeId));
			}
			else {
				
				int leftChildNodeId = currentNodeId + 1;
				walkRecursiveQuantizedTreeAgainstQueryAabb(currentNodes, leftChildNodeId, nodeCallback, quantizedQueryAabbMin, quantizedQueryAabbMax);

				int rightChildNodeId = leftChildNodeId + (currentNodes.isLeafNode(leftChildNodeId) ? 1 : currentNodes.getEscapeIndex(leftChildNodeId));
				walkRecursiveQuantizedTreeAgainstQueryAabb(currentNodes, rightChildNodeId, nodeCallback, quantizedQueryAabbMin, quantizedQueryAabbMax);
			}
		}
	}

	private void walkStacklessQuantizedTreeAgainstRay(NodeOverlapCallback nodeCallback, v3 raySource, v3 rayTarget, v3 aabbMin, v3 aabbMax, int startNodeIndex, int endNodeIndex) {
		assert (useQuantization);

		v3 tmp = new v3();

		int curIndex = startNodeIndex;
		int walkIterations = 0;
		int subTreeSize = endNodeIndex - startNodeIndex;

		QuantizedBvhNodes rootNode = quantizedContiguousNodes;
		int rootNode_idx = startNodeIndex;
		int escapeIndex;

		boolean isLeafNode;
		boolean boxBoxOverlap = false;
		boolean rayBoxOverlap = false;

		float lambda_max = 1f;
		
		
		v3 rayFrom = new v3(raySource);
		v3 rayDirection = new v3();
		tmp.sub(rayTarget, raySource);
		rayDirection.normalize(tmp);
		lambda_max = rayDirection.dot(tmp);
		rayDirection.x = 1f / rayDirection.x;
		rayDirection.y = 1f / rayDirection.y;
		rayDirection.z = 1f / rayDirection.z;



		

		/* Quick pruning by quantized box */
		v3 rayAabbMin = new v3(raySource);
		v3 rayAabbMax = new v3(raySource);
		VectorUtil.setMin(rayAabbMin, rayTarget);
		VectorUtil.setMax(rayAabbMax, rayTarget);

		/* Add box cast extents to bounding box */
		rayAabbMin.add(aabbMin);
		rayAabbMax.add(aabbMax);

		long quantizedQueryAabbMin;
		long quantizedQueryAabbMax;
		quantizedQueryAabbMin = quantizeWithClamp(rayAabbMin);
		quantizedQueryAabbMax = quantizeWithClamp(rayAabbMax);

		v3 bounds_0 = new v3();
		v3 bounds_1 = new v3();
		v3 normal = new v3();
		float[] param = new float[1];

		while (curIndex < endNodeIndex) {

			
			
			
			
			
			
			
			
			
			
			
			
			
			
			

			
			assert (walkIterations < subTreeSize);

			walkIterations++;
			
			param[0] = 1f;
			rayBoxOverlap = false;
			boxBoxOverlap = testQuantizedAabbAgainstQuantizedAabb(quantizedQueryAabbMin, quantizedQueryAabbMax, rootNode.getQuantizedAabbMin(rootNode_idx), rootNode.getQuantizedAabbMax(rootNode_idx));
			isLeafNode = rootNode.isLeafNode(rootNode_idx);
			if (boxBoxOverlap) {
				unQuantize(bounds_0, rootNode.getQuantizedAabbMin(rootNode_idx));
				unQuantize(bounds_1, rootNode.getQuantizedAabbMax(rootNode_idx));
				/* Add box cast extents */
				bounds_0.add(aabbMin);
				bounds_1.add(aabbMax);
				
				
				
				
				
				
				
				
				
				
				
				rayBoxOverlap = AabbUtil2.rayAabb(raySource, rayTarget, bounds_0, bounds_1, param, normal);
				
			}

			if (isLeafNode && rayBoxOverlap) {
				nodeCallback.processNode(rootNode.getPartId(rootNode_idx), rootNode.getTriangleIndex(rootNode_idx));
			}

			if (rayBoxOverlap || isLeafNode) {
				rootNode_idx++;
				curIndex++;
			}
			else {
				escapeIndex = rootNode.getEscapeIndex(rootNode_idx);
				rootNode_idx += escapeIndex;
				curIndex += escapeIndex;
			}
		}

		if (maxIterations < walkIterations) {
			maxIterations = walkIterations;
		}
	}

	private void walkStacklessQuantizedTree(NodeOverlapCallback nodeCallback, long quantizedQueryAabbMin, long quantizedQueryAabbMax, int startNodeIndex, int endNodeIndex) {
		assert (useQuantization);

		int curIndex = startNodeIndex;
		int walkIterations = 0;
		int subTreeSize = endNodeIndex - startNodeIndex;

		QuantizedBvhNodes rootNode = quantizedContiguousNodes;
		int rootNode_idx = startNodeIndex;
		int escapeIndex;

		boolean isLeafNode;
		boolean aabbOverlap;

		while (curIndex < endNodeIndex) {
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			

			
			assert (walkIterations < subTreeSize);

			walkIterations++;
			aabbOverlap = testQuantizedAabbAgainstQuantizedAabb(quantizedQueryAabbMin, quantizedQueryAabbMax, rootNode.getQuantizedAabbMin(rootNode_idx), rootNode.getQuantizedAabbMax(rootNode_idx));
			isLeafNode = rootNode.isLeafNode(rootNode_idx);

			if (isLeafNode && aabbOverlap) {
				nodeCallback.processNode(rootNode.getPartId(rootNode_idx), rootNode.getTriangleIndex(rootNode_idx));
			}

			if (aabbOverlap || isLeafNode) {
				rootNode_idx++;
				curIndex++;
			}
			else {
				escapeIndex = rootNode.getEscapeIndex(rootNode_idx);
				rootNode_idx += escapeIndex;
				curIndex += escapeIndex;
			}
		}

		if (maxIterations < walkIterations) {
			maxIterations = walkIterations;
		}
	}

	public void reportRayOverlappingNodex(NodeOverlapCallback nodeCallback, v3 raySource, v3 rayTarget) {
		boolean fast_path = useQuantization && traversalMode == TraversalMode.STACKLESS;
		if (fast_path) {
			v3 tmp = new v3();
			tmp.set(0f, 0f, 0f);
			walkStacklessQuantizedTreeAgainstRay(nodeCallback, raySource, rayTarget, tmp, tmp, 0, curNodeIndex);
		}
		else {
			/* Otherwise fallback to AABB overlap test */
			v3 aabbMin = new v3(raySource);
			v3 aabbMax = new v3(raySource);
			VectorUtil.setMin(aabbMin, rayTarget);
			VectorUtil.setMax(aabbMax, rayTarget);
			reportAabbOverlappingNodex(nodeCallback, aabbMin, aabbMax);
		}
	}

	public void reportBoxCastOverlappingNodex(NodeOverlapCallback nodeCallback, v3 raySource, v3 rayTarget, v3 aabbMin, v3 aabbMax) {
		boolean fast_path = useQuantization && traversalMode == TraversalMode.STACKLESS;
		if (fast_path) {
			walkStacklessQuantizedTreeAgainstRay(nodeCallback, raySource, rayTarget, aabbMin, aabbMax, 0, curNodeIndex);
		}
		else {
			/* Slow path:
			Construct the bounding box for the entire box cast and send that down the tree */
			v3 qaabbMin = new v3(raySource);
			v3 qaabbMax = new v3(raySource);
			VectorUtil.setMin(qaabbMin, rayTarget);
			VectorUtil.setMax(qaabbMax, rayTarget);
			qaabbMin.add(aabbMin);
			qaabbMax.add(aabbMax);
			reportAabbOverlappingNodex(nodeCallback, qaabbMin, qaabbMax);
		}
	}
	
	private long quantizeWithClamp(v3 point) {
		assert (useQuantization);

		v3 clampedPoint = new v3(point);
		VectorUtil.setMax(clampedPoint, bvhAabbMin);
		VectorUtil.setMin(clampedPoint, bvhAabbMax);

		v3 v = new v3();
		v.sub(clampedPoint, bvhAabbMin);
		VectorUtil.mul(v, v, bvhQuantization);

		int out0 = (int)(v.x + 0.5f) & 0xFFFF;
		int out1 = (int)(v.y + 0.5f) & 0xFFFF;
		int out2 = (int)(v.z + 0.5f) & 0xFFFF;

		return ((long)out0) | (((long)out1) << 16) | (((long)out2) << 32);
	}
	
	private void unQuantize(v3 vecOut, long vecIn) {
		int vecIn0 = (int)((vecIn & 0x00000000FFFFL));
		int vecIn1 = (int)((vecIn & 0x0000FFFF0000L) >>> 16);
		int vecIn2 = (int)((vecIn & 0xFFFF00000000L) >>> 32);

		vecOut.x = (float)vecIn0 / (bvhQuantization.x);
		vecOut.y = (float)vecIn1 / (bvhQuantization.y);
		vecOut.z = (float)vecIn2 / (bvhQuantization.z);

		vecOut.add(bvhAabbMin);
	}
	
}
