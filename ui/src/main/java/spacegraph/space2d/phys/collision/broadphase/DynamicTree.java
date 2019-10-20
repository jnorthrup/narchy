/*******************************************************************************
 * Copyright (c) 2013, Daniel Murphy
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package spacegraph.space2d.phys.collision.broadphase;

import jcog.math.v2;
import spacegraph.space2d.phys.callbacks.DebugDraw;
import spacegraph.space2d.phys.callbacks.TreeCallback;
import spacegraph.space2d.phys.callbacks.TreeRayCastCallback;
import spacegraph.space2d.phys.collision.AABB;
import spacegraph.space2d.phys.collision.RayCastInput;
import spacegraph.space2d.phys.common.Color3f;
import spacegraph.space2d.phys.common.MathUtils;
import spacegraph.space2d.phys.common.Settings;

/**
 * A dynamic tree arranges data in a binary tree to accelerate queries such as volume queries and
 * ray casts. Leafs are proxies with an AABB. In the tree we expand the proxy AABB by _fatAABBFactor
 * so that the proxy AABB is bigger than the client object. This allows the client object to move by
 * small amounts without triggering a tree update.
 *
 * @author daniel
 */
public class DynamicTree implements BroadPhaseStrategy {

    private static final int NULL_NODE = -1;

    private DynamicTreeNode m_root;
    private DynamicTreeNode[] node;
    private int m_nodeCount;
    private int m_nodeCapacity;

    private int m_freeList;

    private final v2[] drawVecs = new v2[4];
    private DynamicTreeNode[] stack = new DynamicTreeNode[20];
    private int stackPtr = 0;

    public DynamicTree() {
        m_root = null;
        m_nodeCount = 0;
        m_nodeCapacity = 16;
        node = new DynamicTreeNode[m_nodeCapacity];

        
        for (var i = m_nodeCapacity - 1; i >= 0; i--) {
            node[i] = new DynamicTreeNode(i);
            node[i].parent = (i == m_nodeCapacity - 1) ? null : node[i + 1];
            node[i].height = -1;
        }
        m_freeList = 0;

        for (var i = 0; i < drawVecs.length; i++) {
            drawVecs[i] = new v2();
        }
    }

    @Override
    public final int createProxy(AABB aabb, Object userData) {
        assert (aabb.isValid());
        var node = allocateNode();
        var proxyId = node.id;

        var nodeAABB = node.aabb;
        nodeAABB.lowerBound.x = aabb.lowerBound.x - Settings.aabbExtension;
        nodeAABB.lowerBound.y = aabb.lowerBound.y - Settings.aabbExtension;
        nodeAABB.upperBound.x = aabb.upperBound.x + Settings.aabbExtension;
        nodeAABB.upperBound.y = aabb.upperBound.y + Settings.aabbExtension;
        node.data = userData;

        insertLeaf(proxyId);

        return proxyId;
    }

    @Override
    public final void destroyProxy(int proxyId) {
        assert (0 <= proxyId && proxyId < m_nodeCapacity);
        var node = this.node[proxyId];
        assert (node.child1 == null);

        removeLeaf(node);
        freeNode(node);
    }

    @Override
    public final boolean moveProxy(int proxyId, AABB aabb, v2 displacement) {
        assert (aabb.isValid());
        assert (0 <= proxyId && proxyId < m_nodeCapacity);
        var node = this.node[proxyId];
        assert (node.child1 == null);

        var nodeAABB = node.aabb;
        
        if (nodeAABB.lowerBound.x <= aabb.lowerBound.x && nodeAABB.lowerBound.y <= aabb.lowerBound.y
                && aabb.upperBound.x <= nodeAABB.upperBound.x && aabb.upperBound.y <= nodeAABB.upperBound.y) {
            return false;
        }

        removeLeaf(node);


        var lowerBound = nodeAABB.lowerBound;
        var upperBound = nodeAABB.upperBound;
        lowerBound.x = aabb.lowerBound.x - Settings.aabbExtension;
        lowerBound.y = aabb.lowerBound.y - Settings.aabbExtension;
        upperBound.x = aabb.upperBound.x + Settings.aabbExtension;
        upperBound.y = aabb.upperBound.y + Settings.aabbExtension;


        var dx = displacement.x * Settings.aabbMultiplier;
        var dy = displacement.y * Settings.aabbMultiplier;
        if (dx < 0.0f) {
            lowerBound.x += dx;
        } else {
            upperBound.x += dx;
        }

        if (dy < 0.0f) {
            lowerBound.y += dy;
        } else {
            upperBound.y += dy;
        }

        insertLeaf(proxyId);
        return true;
    }

    @Override
    public final Object getUserData(int proxyId) {
        assert (0 <= proxyId && proxyId < m_nodeCapacity);
        return node[proxyId].data;
    }

    @Override
    public final AABB getFatAABB(int proxyId) {
        assert (0 <= proxyId && proxyId < m_nodeCapacity);
        return node[proxyId].aabb;
    }

    @Override
    public final void query(TreeCallback callback, AABB aabb) {
        assert (aabb.isValid());
        stackPtr = 0;
        stack[stackPtr++] = m_root;

        while (stackPtr > 0) {
            var node = stack[--stackPtr];
            if (node == null) {
                continue;
            }

            if (AABB.testOverlap(node.aabb, aabb)) {
                if (node.child1 == null) {
                    var proceed = callback.treeCallback(node.id);
                    if (!proceed) {
                        return;
                    }
                } else {
                    grow();
                    stack[stackPtr++] = node.child1;
                    stack[stackPtr++] = node.child2;
                }
            }
        }
    }

    private void grow() {
        if (stack.length - stackPtr - 2 <= 0) {
            var newBuffer = new DynamicTreeNode[stack.length * 2];
            System.arraycopy(stack, 0, newBuffer, 0, stack.length);
            stack = newBuffer;
        }
    }

    private final v2 r = new v2();
    private final AABB aabb = new AABB();
    private final RayCastInput subInput = new RayCastInput();

    @Override
    public void raycast(TreeRayCastCallback callback, RayCastInput input) {
        var p1 = input.p1;
        var p2 = input.p2;
        float p1x = p1.x, p2x = p2.x, p1y = p1.y, p2y = p2.y;
        r.x = p2x - p1x;
        r.y = p2y - p1y;
        assert ((r.x * r.x + r.y * r.y) > 0f);
        r.normalize();
        var rx = r.x;
        var ry = r.y;


        var maxFraction = input.maxFraction;


        var segAABB = aabb;


        var tempx = (p2x - p1x) * maxFraction + p1x;
        var tempy = (p2y - p1y) * maxFraction + p1y;
        segAABB.lowerBound.x = Math.min(p1x, tempx);
        segAABB.lowerBound.y = Math.min(p1y, tempy);
        segAABB.upperBound.x = Math.max(p1x, tempx);
        segAABB.upperBound.y = Math.max(p1y, tempy);
        

        stackPtr = 0;
        stack[stackPtr++] = m_root;
        var vy = 1f * rx;
        var absVy = Math.abs(vy);
        var vx = -1f * ry;
        var absVx = Math.abs(vx);
        while (stackPtr > 0) {
            var node = stack[--stackPtr];
            if (node == null) {
                continue;
            }

            var nodeAABB = node.aabb;
            if (!AABB.testOverlap(nodeAABB, segAABB)) {
                continue;
            }


            var cx = (nodeAABB.lowerBound.x + nodeAABB.upperBound.x) * .5f;
            var cy = (nodeAABB.lowerBound.y + nodeAABB.upperBound.y) * .5f;
            var hx = (nodeAABB.upperBound.x - nodeAABB.lowerBound.x) * .5f;
            var hy = (nodeAABB.upperBound.y - nodeAABB.lowerBound.y) * .5f;
            tempx = p1x - cx;
            tempy = p1y - cy;
            var separation = Math.abs(vx * tempx + vy * tempy) - (absVx * hx + absVy * hy);
            if (separation > 0.0f) {
                continue;
            }

            if (node.child1 == null) {
                subInput.p1.x = p1x;
                subInput.p1.y = p1y;
                subInput.p2.x = p2x;
                subInput.p2.y = p2y;
                subInput.maxFraction = maxFraction;

                var value = callback.raycastCallback(subInput, node.id);

                if (value == 0.0f) {
                    
                    return;
                }

                if (value > 0.0f) {
                    
                    maxFraction = value;
                    
                    
                    
                    tempx = (p2x - p1x) * maxFraction + p1x;
                    tempy = (p2y - p1y) * maxFraction + p1y;
                    segAABB.lowerBound.x = Math.min(p1x, tempx);
                    segAABB.lowerBound.y = Math.min(p1y, tempy);
                    segAABB.upperBound.x = Math.max(p1x, tempx);
                    segAABB.upperBound.y = Math.max(p1y, tempy);
                }
            } else {
                grow();
                stack[stackPtr++] = node.child1;
                stack[stackPtr++] = node.child2;
            }
        }
    }

    @Override
    public final int computeHeight() {
        return computeHeight(m_root);
    }

    private int computeHeight(DynamicTreeNode node) {
        assert (0 <= node.id && node.id < m_nodeCapacity);

        if (node.child1 == null) {
            return 0;
        }
        var height1 = computeHeight(node.child1);
        var height2 = computeHeight(node.child2);
        return 1 + MathUtils.max(height1, height2);
    }

    /**
     * Validate this tree. For testing.
     */
    private void validate() {
        validateStructure(m_root);
        validateMetrics(m_root);

        var freeCount = 0;
        var freeNode = m_freeList != NULL_NODE ? node[m_freeList] : null;
        while (freeNode != null) {
            assert (0 <= freeNode.id && freeNode.id < m_nodeCapacity);
            assert (freeNode == node[freeNode.id]);
            freeNode = freeNode.parent;
            ++freeCount;
        }

        assert (getHeight() == computeHeight());

        assert (m_nodeCount + freeCount == m_nodeCapacity);
    }

    @Override
    public int getHeight() {
        if (m_root == null) {
            return 0;
        }
        return m_root.height;
    }

    @Override
    public int getMaxBalance() {
        var maxBalance = 0;
        for (var i = 0; i < m_nodeCapacity; ++i) {
            var node = this.node[i];
            if (node.height <= 1) {
                continue;
            }

            assert (node.child1 == null == false);

            var child1 = node.child1;
            var child2 = node.child2;
            var balance = MathUtils.abs(child2.height - child1.height);
            maxBalance = MathUtils.max(maxBalance, balance);
        }

        return maxBalance;
    }

    @Override
    public float getAreaRatio() {
        if (m_root == null) {
            return 0.0f;
        }

        var root = m_root;
        var rootArea = root.aabb.getPerimeter();

        var totalArea = 0.0f;
        for (var i = 0; i < m_nodeCapacity; ++i) {
            var node = this.node[i];
            if (node.height < 0)
                continue;

            totalArea += node.aabb.getPerimeter();
        }

        return totalArea / rootArea;
    }

    /**
     * Build an optimal tree. Very expensive. For testing.
     */
    public void rebuildBottomUp() {
        var nodes = new int[m_nodeCount];
        var count = 0;


        for (var i = 0; i < m_nodeCapacity; ++i) {
            if (node[i].height < 0) {

                continue;
            }

            var node = this.node[i];
            if (node.child1 == null) {
                node.parent = null;
                nodes[count] = i;
                ++count;
            } else {
                freeNode(node);
            }
        }

        var b = new AABB();
        while (count > 1) {
            var minCost = Float.POSITIVE_INFINITY;
            int iMin = -1, jMin = -1;
            for (var i = 0; i < count; ++i) {
                var aabbi = node[nodes[i]].aabb;

                for (var j = i + 1; j < count; ++j) {
                    var aabbj = node[nodes[j]].aabb;
                    b.combine(aabbi, aabbj);
                    var cost = b.getPerimeter();
                    if (cost < minCost) {
                        iMin = i;
                        jMin = j;
                        minCost = cost;
                    }
                }
            }

            var index1 = nodes[iMin];
            var index2 = nodes[jMin];
            var child1 = node[index1];
            var child2 = node[index2];

            var parent = allocateNode();
            parent.child1 = child1;
            parent.child2 = child2;
            parent.height = 1 + MathUtils.max(child1.height, child2.height);
            parent.aabb.combine(child1.aabb, child2.aabb);
            parent.parent = null;

            child1.parent = parent;
            child2.parent = parent;

            nodes[jMin] = nodes[count - 1];
            nodes[iMin] = parent.id;
            --count;
        }

        m_root = node[nodes[0]];

        validate();
    }

    private DynamicTreeNode allocateNode() {
        if (m_freeList == NULL_NODE) {
            assert (m_nodeCount == m_nodeCapacity);

            var old = node;
            m_nodeCapacity *= 2;
            node = new DynamicTreeNode[m_nodeCapacity];
            System.arraycopy(old, 0, node, 0, old.length);

            
            for (var i = m_nodeCapacity - 1; i >= m_nodeCount; i--) {
                node[i] = new DynamicTreeNode(i);
                node[i].parent = (i == m_nodeCapacity - 1) ? null : node[i + 1];
                node[i].height = -1;
            }
            m_freeList = m_nodeCount;
        }
        var nodeId = m_freeList;
        var treeNode = node[nodeId];
        m_freeList = treeNode.parent != null ? treeNode.parent.id : NULL_NODE;

        treeNode.parent = null;
        treeNode.child1 = null;
        treeNode.child2 = null;
        treeNode.height = 0;
        treeNode.data = null;
        ++m_nodeCount;
        return treeNode;
    }

    /**
     * returns a node to the pool
     */
    private void freeNode(DynamicTreeNode node) {
        assert (node != null);
        assert (0 < m_nodeCount);
        node.parent = m_freeList != NULL_NODE ? this.node[m_freeList] : null;
        node.height = -1;
        m_freeList = node.id;
        m_nodeCount--;
    }

    private final AABB combinedAABB = new AABB();

    private void insertLeaf(int leaf_index) {
        var leaf = node[leaf_index];
        if (m_root == null) {
            m_root = leaf;
            m_root.parent = null;
            return;
        }


        var leafAABB = leaf.aabb;
        var index = m_root;
        while (index.child1 != null) {
            var node = index;
            var child1 = node.child1;
            var child2 = node.child2;

            var area = node.aabb.getPerimeter();

            combinedAABB.combine(node.aabb, leafAABB);
            var combinedArea = combinedAABB.getPerimeter();


            var inheritanceCost = 2.0f * (combinedArea - area);

            
            float cost1;
            if (child1.child1 == null) {
                combinedAABB.combine(leafAABB, child1.aabb);
                cost1 = combinedAABB.getPerimeter() + inheritanceCost;
            } else {
                combinedAABB.combine(leafAABB, child1.aabb);
                var oldArea = child1.aabb.getPerimeter();
                var newArea = combinedAABB.getPerimeter();
                cost1 = (newArea - oldArea) + inheritanceCost;
            }

            
            float cost2;
            if (child2.child1 == null) {
                combinedAABB.combine(leafAABB, child2.aabb);
                cost2 = combinedAABB.getPerimeter() + inheritanceCost;
            } else {
                combinedAABB.combine(leafAABB, child2.aabb);
                var oldArea = child2.aabb.getPerimeter();
                var newArea = combinedAABB.getPerimeter();
                cost2 = newArea - oldArea + inheritanceCost;
            }


            var cost = 2.0f * combinedArea;
            if (cost < cost1 && cost < cost2) {
                break;
            }

            
            if (cost1 < cost2) {
                index = child1;
            } else {
                index = child2;
            }
        }

        var sibling = index;
        var oldParent = node[sibling.id].parent;
        var newParent = allocateNode();
        newParent.parent = oldParent;
        newParent.data = null;
        newParent.aabb.combine(leafAABB, sibling.aabb);
        newParent.height = sibling.height + 1;

        if (oldParent != null) {
            
            if (oldParent.child1 == sibling) {
                oldParent.child1 = newParent;
            } else {
                oldParent.child2 = newParent;
            }

            newParent.child1 = sibling;
            newParent.child2 = leaf;
            sibling.parent = newParent;
            leaf.parent = newParent;
        } else {
            
            newParent.child1 = sibling;
            newParent.child2 = leaf;
            sibling.parent = newParent;
            leaf.parent = newParent;
            m_root = newParent;
        }

        
        index = leaf.parent;
        while (index != null) {
            index = balance(index);

            var child1 = index.child1;
            var child2 = index.child2;

            assert (child1 != null);
            assert (child2 != null);

            index.height = 1 + MathUtils.max(child1.height, child2.height);
            index.aabb.combine(child1.aabb, child2.aabb);

            index = index.parent;
        }
        
    }

    private void removeLeaf(DynamicTreeNode leaf) {
        if (leaf == m_root) {
            m_root = null;
            return;
        }

        var parent = leaf.parent;
        var grandParent = parent.parent;
        var sibling = parent.child1 == leaf ? parent.child2 : parent.child1;

        if (grandParent != null) {
            
            if (grandParent.child1 == parent) {
                grandParent.child1 = sibling;
            } else {
                grandParent.child2 = sibling;
            }
            sibling.parent = grandParent;
            freeNode(parent);


            var index = grandParent;
            while (index != null) {
                index = balance(index);

                DynamicTreeNode child1 = index.child1, child2 = index.child2;

                index.aabb.combine(child1.aabb, child2.aabb);
                index.height = 1 + MathUtils.max(child1.height, child2.height);

                index = index.parent;
            }
        } else {
            m_root = sibling;
            sibling.parent = null;
            freeNode(parent);
        }

        
    }

    
    
    private DynamicTreeNode balance(DynamicTreeNode A) {
        assert (A != null);


        if (A.child1 == null || A.height < 2) {
            return A;
        }

        var iB = A.child1;
        var iC = A.child2;
        assert (0 <= iB.id && iB.id < m_nodeCapacity);
        assert (0 <= iC.id && iC.id < m_nodeCapacity);

        var B = iB;
        var C = iC;

        var balance = C.height - B.height;

        
        if (balance > 1) {
            var iF = C.child1;
            var iG = C.child2;
            var F = iF;
            var G = iG;
            assert (F != null);
            assert (G != null);
            assert (0 <= iF.id && iF.id < m_nodeCapacity);
            assert (0 <= iG.id && iG.id < m_nodeCapacity);

            
            C.child1 = A;
            C.parent = A.parent;
            A.parent = iC;

            
            if (C.parent != null) {
                if (C.parent.child1 == A) {
                    C.parent.child1 = iC;
                } else {
                    assert (C.parent.child2 == A);
                    C.parent.child2 = iC;
                }
            } else {
                m_root = iC;
            }

            
            if (F.height > G.height) {
                C.child2 = iF;
                A.child2 = iG;
                G.parent = A;
                A.aabb.combine(B.aabb, G.aabb);
                C.aabb.combine(A.aabb, F.aabb);

                A.height = 1 + MathUtils.max(B.height, G.height);
                C.height = 1 + MathUtils.max(A.height, F.height);
            } else {
                C.child2 = iG;
                A.child2 = iF;
                F.parent = A;
                A.aabb.combine(B.aabb, F.aabb);
                C.aabb.combine(A.aabb, G.aabb);

                A.height = 1 + MathUtils.max(B.height, F.height);
                C.height = 1 + MathUtils.max(A.height, G.height);
            }

            return iC;
        }

        
        if (balance < -1) {
            var iD = B.child1;
            var iE = B.child2;
            var D = iD;
            var E = iE;
            assert (0 <= iD.id && iD.id < m_nodeCapacity);
            assert (0 <= iE.id && iE.id < m_nodeCapacity);

            
            B.child1 = A;
            B.parent = A.parent;
            A.parent = iB;

            
            if (B.parent != null) {
                if (B.parent.child1 == A) {
                    B.parent.child1 = iB;
                } else {
                    assert (B.parent.child2 == A);
                    B.parent.child2 = iB;
                }
            } else {
                m_root = iB;
            }

            
            if (D.height > E.height) {
                B.child2 = iD;
                A.child1 = iE;
                E.parent = A;
                A.aabb.combine(C.aabb, E.aabb);
                B.aabb.combine(A.aabb, D.aabb);

                A.height = 1 + MathUtils.max(C.height, E.height);
                B.height = 1 + MathUtils.max(A.height, D.height);
            } else {
                B.child2 = iE;
                A.child1 = iD;
                D.parent = A;
                A.aabb.combine(C.aabb, D.aabb);
                B.aabb.combine(A.aabb, E.aabb);

                A.height = 1 + MathUtils.max(C.height, D.height);
                B.height = 1 + MathUtils.max(A.height, E.height);
            }

            return iB;
        }

        return A;
    }

    private void validateStructure(DynamicTreeNode node) {
        if (node == null)
            return;

        assert (node == this.node[node.id]);

        assert node != m_root || (node.parent == null);

        DynamicTreeNode child1 = node.child1, child2 = node.child2;

        if (node.child1 == null) {
            //assert (child1 == null);
            assert (child2 == null);
            assert (node.height == 0);
            return;
        }

        assert (/*child1 != null &&*/ 0 <= child1.id && child1.id < m_nodeCapacity);
        assert (child2 != null && 0 <= child2.id && child2.id < m_nodeCapacity);

        assert (child1.parent == node);
        assert (child2.parent == node);

        validateStructure(child1);
        validateStructure(child2);
    }

    private void validateMetrics(DynamicTreeNode node) {
        if (node == null) {
            return;
        }

        var child1 = node.child1;
        var child2 = node.child2;

        if (node.child1 == null) {
            //assert (child1 == null);
            assert (child2 == null);
            assert (node.height == 0);
            return;
        }

        assert (/*child1 != null &&*/ 0 <= child1.id && child1.id < m_nodeCapacity);
        assert (child2 != null && 0 <= child2.id && child2.id < m_nodeCapacity);

        var height1 = child1.height;
        var height2 = child2.height;
        var height = 1 + MathUtils.max(height1, height2);
        assert (node.height == height);

        var aabb = new AABB();
        aabb.combine(child1.aabb, child2.aabb);

        assert (aabb.lowerBound.equals(node.aabb.lowerBound));
        assert (aabb.upperBound.equals(node.aabb.upperBound));

        validateMetrics(child1);
        validateMetrics(child2);
    }

    @Override
    public void drawTree(DebugDraw argDraw) {
        if (m_root == null) {
            return;
        }
        var height = computeHeight();
        drawTree(argDraw, m_root, 0, height);
    }

    private final Color3f color = new Color3f();
    private final v2 textVec = new v2();

    private void drawTree(DebugDraw argDraw, DynamicTreeNode node, int spot, int height) {
        node.aabb.vertices(drawVecs);

        color.set(1, (height - spot) * 1f / height, (height - spot) * 1f / height);
        argDraw.drawPolygon(drawVecs, 4, color);

        argDraw.getViewportTranform().getWorldToScreen(node.aabb.upperBound, textVec);
        argDraw.drawString(textVec.x, textVec.y, node.id + "-" + (spot + 1) + '/' + height, color);

        if (node.child1 != null) {
            drawTree(argDraw, node.child1, spot + 1, height);
        }
        if (node.child2 != null) {
            drawTree(argDraw, node.child2, spot + 1, height);
        }
    }
}
