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

import spacegraph.space3d.phys.math.VectorUtil;
import spacegraph.util.math.v3;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 *
 * @author jezek2
 */
class BvhTree {

	private int num_nodes;
	private final BvhTreeNodeArray node_array = new BvhTreeNodeArray();
	
	private static int _calc_splitting_axis(BvhDataArray primitive_boxes, int startIndex, int endIndex) {
		v3 means = new v3();
		v3 variance = new v3();

		int numIndices = endIndex - startIndex;

		v3 center = new v3();
		v3 diff2 = new v3();

		v3 tmp1 = new v3();
		v3 tmp2 = new v3();

		mean(primitive_boxes, startIndex, endIndex, means, numIndices, center, tmp1, tmp2);

		for (int i=startIndex; i<endIndex; i++) {
			primitive_boxes.getBoundMax(i, tmp1);
			primitive_boxes.getBoundMin(i, tmp2);
			center.add(tmp1, tmp2);
			center.scale(0.5f);
			diff2.sub(center, means);
			VectorUtil.mul(diff2, diff2, diff2);
			variance.add(diff2);
		}
		variance.scale(1f / (float)(numIndices - 1));

		return VectorUtil.maxAxis(variance);
	}

	private static void mean(BvhDataArray primitive_boxes, int startIndex, int endIndex, v3 means, float numIndices, v3 center, v3 tmp1, v3 tmp2) {
		for (int i = startIndex; i < endIndex; i++) {
			primitive_boxes.getBoundMax(i, tmp1);
			primitive_boxes.getBoundMin(i, tmp2);
			center.add(tmp1, tmp2);
			center.scale(0.5f);
			means.add(center);
		}
		means.scale(1f / numIndices);
	}

	private static int _sort_and_calc_splitting_index(BvhDataArray primitive_boxes, int startIndex, int endIndex, int splitAxis) {
		int splitIndex = startIndex;
		int numIndices = endIndex - startIndex;

		
		float splitValue = 0.0f;

		v3 means = new v3();
		means.set(0f, 0f, 0f);

		v3 center = new v3();

		v3 tmp1 = new v3();
		v3 tmp2 = new v3();

		mean(primitive_boxes, startIndex, endIndex, means, numIndices, center, tmp1, tmp2);

		splitValue = VectorUtil.coord(means, splitAxis);

		
		for (int i = startIndex; i < endIndex; i++) {
			primitive_boxes.getBoundMax(i, tmp1);
			primitive_boxes.getBoundMin(i, tmp2);
			center.add(tmp1, tmp2);
			center.scale(0.5f);

			if (VectorUtil.coord(center, splitAxis) > splitValue) {
				
				primitive_boxes.swap(i, splitIndex);
				
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

	private void _build_sub_tree(BvhDataArray primitive_boxes, int startIndex, int endIndex) {
		final Deque<_build_sub_treeFrame> stack = new ArrayDeque<>();
		stack.push(new _build_sub_treeFrame(primitive_boxes, startIndex, endIndex));
		while (!stack.isEmpty()) {
			final _build_sub_treeFrame frame = stack.peek();
			switch (frame.block) {
				case 0: {
					frame.curIndex = num_nodes;
					num_nodes++;
					assert ((frame.endIndex - frame.startIndex) > 0);
					if ((frame.endIndex - frame.startIndex) == 1) {
						
						
						
						node_array.set(frame.curIndex, frame.primitive_boxes, frame.startIndex);

						stack.pop();
						break;
					}
					frame.splitIndex = _calc_splitting_axis(frame.primitive_boxes, frame.startIndex, frame.endIndex);
					frame.splitIndex = _sort_and_calc_splitting_index(frame.primitive_boxes, frame.startIndex, frame.endIndex, frame.splitIndex);
					frame.node_bound = new BoxCollision.AABB();
					frame.tmpAABB = new BoxCollision.AABB();
					frame.node_bound.invalidate();
					for (int i = frame.startIndex; i < frame.endIndex; i++) {
						frame.primitive_boxes.getBound(i, frame.tmpAABB);
						frame.node_bound.merge(frame.tmpAABB);
					}
					setNodeBound(frame.curIndex, frame.node_bound);
					stack.push(new _build_sub_treeFrame(frame.primitive_boxes, frame.startIndex, frame.splitIndex));
					frame.block = 1;
					break;
				}
				case 1: {
					stack.push(new _build_sub_treeFrame(frame.primitive_boxes, frame.splitIndex, frame.endIndex));
					frame.block = 2;
					break;
				}
				case 2: {
					node_array.setEscapeIndex(frame.curIndex, num_nodes - frame.curIndex);
					stack.pop();
					break;
				}
			}
		}
	}

	private static final class _build_sub_treeFrame {
		private final BvhDataArray primitive_boxes;
		private final int startIndex;
		private final int endIndex;
		private int curIndex;
		private int splitIndex;
		private BoxCollision.AABB node_bound;
		private BoxCollision.AABB tmpAABB;
		private int block;

		private _build_sub_treeFrame(BvhDataArray primitive_boxes, int startIndex, int endIndex) {
			this.primitive_boxes = primitive_boxes;
			this.startIndex = startIndex;
			this.endIndex = endIndex;
		}
	}

	public void build_tree(BvhDataArray primitive_boxes) {
		
		num_nodes = 0;
		
		node_array.resize(primitive_boxes.size()*2);

		_build_sub_tree(primitive_boxes, 0, primitive_boxes.size());
	}

	public void clearNodes() {
		node_array.clear();
		num_nodes = 0;
	}

	public int getNodeCount() {
		return num_nodes;
	}

	/**
	 * Tells if the node is a leaf.
	 */
	public boolean isLeafNode(int nodeindex) {
		return node_array.isLeafNode(nodeindex);
	}

	public int getNodeData(int nodeindex) {
		return node_array.getDataIndex(nodeindex);
	}

	public void getNodeBound(int nodeindex, BoxCollision.AABB bound) {
		node_array.getBound(nodeindex, bound);
	}

	public void setNodeBound(int nodeindex, BoxCollision.AABB bound) {
		node_array.setBound(nodeindex, bound);
	}

	public static int getLeftNode(int nodeindex) {
		return nodeindex + 1;
	}

	public int getRightNode(int nodeindex) {
		if (node_array.isLeafNode(nodeindex + 1)) {
			return nodeindex + 2;
		}
		return nodeindex + 1 + node_array.getEscapeIndex(nodeindex + 1);
	}

	public int getEscapeNodeIndex(int nodeindex) {
		return node_array.getEscapeIndex(nodeindex);
	}

	public BvhTreeNodeArray get_node_pointer() {
		return node_array;
	}
	
}
