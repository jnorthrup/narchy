/*******************************************************************************
 * Copyright (c) 2013, Daniel Murphy
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
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
 */
package spacegraph.space2d.phys.collision.broadphase

import jcog.math.v2
import spacegraph.space2d.phys.callbacks.DebugDraw
import spacegraph.space2d.phys.callbacks.PairCallback
import spacegraph.space2d.phys.callbacks.TreeCallback
import spacegraph.space2d.phys.callbacks.TreeRayCastCallback
import spacegraph.space2d.phys.collision.AABB
import spacegraph.space2d.phys.collision.RayCastInput
import java.util.*

/**
 * The broad-phase is used for computing pairs and performing volume queries and ray casts. This
 * broad-phase does not persist pairs. Instead, this reports potentially new pairs. It is up to the
 * client to consume the new pairs and to track subsequent overlap.
 *
 * @author Daniel Murphy
 */
class DefaultBroadPhaseBuffer(private val m_tree: BroadPhaseStrategy) : TreeCallback, BroadPhase {

    private var m_proxyCount: Int = 0

    private var m_moveBuffer: IntArray
    private var m_moveCapacity: Int = 0
    private var m_moveCount: Int = 0

    private var m_pairBuffer: Array<Pair<Int, Int>? >
    private var m_pairCapacity: Int = 0
    private var m_pairCount: Int = 0

    private var m_queryProxyId: Int = 0

    init {
        m_proxyCount = 0

        m_pairCapacity = 16
        m_pairCount = 0
        m_pairBuffer = Array(m_pairCapacity, { Pair(0, 0) })

        m_moveCapacity = 16
        m_moveCount = 0
        m_moveBuffer = IntArray(m_moveCapacity)
        m_queryProxyId = BroadPhase.NULL_PROXY
    }

    override fun createProxy(aabb: AABB, userData: Any): Int {
        val proxyId = m_tree.createProxy(aabb, userData)
        ++m_proxyCount
        bufferMove(proxyId)
        return proxyId
    }

    override fun destroyProxy(proxyId: Int) {
        unbufferMove(proxyId)
        --m_proxyCount
        m_tree.destroyProxy(proxyId)
    }

    override fun moveProxy(proxyId: Int, aabb: AABB, displacement: v2) {
        val buffer = m_tree.moveProxy(proxyId, aabb, displacement)
        if (buffer) {
            bufferMove(proxyId)
        }
    }

    override fun touchProxy(proxyId: Int) {
        bufferMove(proxyId)
    }

    override fun get(proxyId: Int): Any {
        return m_tree.getUserData(proxyId)
    }

    override fun getFatAABB(proxyId: Int): AABB {
        return m_tree.getFatAABB(proxyId)
    }

    override fun testOverlap(proxyIdA: Int, proxyIdB: Int): Boolean {


        val a = m_tree.getFatAABB(proxyIdA)
        val b = m_tree.getFatAABB(proxyIdB)
        return if (b.lowerBound.x - a.upperBound.x > 0.0f || b.lowerBound.y - a.upperBound.y > 0.0f) {
            false
        } else a.lowerBound.x - b.upperBound.x <= 0.0f && a.lowerBound.y - b.upperBound.y <= 0.0f

    }

    override fun getProxyCount(): Int {
        return m_proxyCount
    }

    override fun drawTree(argDraw: DebugDraw) {
        m_tree.drawTree(argDraw)
    }

    override fun updatePairs(callback: PairCallback) {

        m_pairCount = 0


        for (i in 0 until m_moveCount) {
            m_queryProxyId = m_moveBuffer!![i]
            if (m_queryProxyId == BroadPhase.NULL_PROXY) {
                continue
            }


            val fatAABB = m_tree.getFatAABB(m_queryProxyId)



            m_tree.query(this, fatAABB)
        }



        m_moveCount = 0


        Arrays.sort(m_pairBuffer!!, 0, m_pairCount)


        var i = 0
        while (i < m_pairCount) {
            val (first, second) = m_pairBuffer[i]!!
            val userDataA = m_tree.getUserData(first)
            val userDataB = m_tree.getUserData(second)


            callback.addPair(userDataA, userDataB)
            ++i


            while (i < m_pairCount) {
                val (first1, second1) = m_pairBuffer!![i]!!
                if (first1 !== first || second1 !== second) {
                    break
                }
                ++i
            }
        }
    }

    override fun query(callback: TreeCallback, aabb: AABB) {
        m_tree.query(callback, aabb)
    }

    override fun raycast(callback: TreeRayCastCallback, input: RayCastInput) {
        m_tree.raycast(callback, input)
    }

    override fun getTreeHeight(): Int {
        return m_tree.height
    }

    override fun getTreeBalance(): Int {
        return m_tree.maxBalance
    }

    override fun getTreeQuality(): Float {
        return m_tree.areaRatio
    }

    private fun bufferMove(proxyId: Int) {
        if (m_moveCount == m_moveCapacity) {
            val old = m_moveBuffer
            m_moveCapacity *= 2
            m_moveBuffer = IntArray(m_moveCapacity)
            System.arraycopy(old!!, 0, m_moveBuffer!!, 0, old.size)
        }

        m_moveBuffer!![m_moveCount] = proxyId!!
        ++m_moveCount
    }

    private fun unbufferMove(proxyId: Int) {
        for (i in 0 until m_moveCount) {
            if (m_moveBuffer!![i] == proxyId) {
                m_moveBuffer!![i] = BroadPhase.NULL_PROXY
            }
        }
    }

    /**
     * This is called from DynamicTree::query when we are gathering pairs.
     */
    override fun treeCallback(proxyId: Int): Boolean {

        if (proxyId == m_queryProxyId) {
            return true
        }


        if (m_pairCount == m_pairCapacity) {
            val oldBuffer = m_pairBuffer
            m_pairCapacity *= 2
            m_pairBuffer = arrayOfNulls(m_pairCapacity)
            System.arraycopy(oldBuffer!!, 0, m_pairBuffer!!, 0, oldBuffer.size)
        }
        m_pairBuffer!![m_pairCount++] =
                if (proxyId < m_queryProxyId) {
                    Pair(first = proxyId,
                            second = m_queryProxyId)
                } else {
                    Pair(
                            first = m_queryProxyId
                            , second = proxyId
                    )
                }
        return true
    }
}
