/*******************************************************************************
 * Copyright (c) 2013, Daniel Murphy
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 	* Redistributions of source code must retain the above copyright notice,
 * 	  this list of conditions and the following disclaimer.
 * 	* Redistributions in binary form must reproduce the above copyright notice,
 * 	  this list of conditions and the following disclaimer in the documentation
 * 	  and/or other materials provided with the distribution.
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
package spacegraph.space2d.phys.pooling;

import spacegraph.space2d.phys.collision.AABB;
import spacegraph.space2d.phys.collision.Collision;
import spacegraph.space2d.phys.collision.Distance;
import spacegraph.space2d.phys.collision.TimeOfImpact;
import spacegraph.space2d.phys.common.Mat22;
import spacegraph.space2d.phys.common.Mat33;
import spacegraph.space2d.phys.common.Rot;
import spacegraph.space2d.phys.common.Vec3;
import spacegraph.space2d.phys.dynamics.contacts.Contact;
import spacegraph.util.math.v2;

/**
 * World pool interface
 *
 * @author Daniel
 */
public interface IWorldPool {

    IDynamicStack<Contact> getPolyContactStack();

    IDynamicStack<Contact> getCircleContactStack();

    IDynamicStack<Contact> getPolyCircleContactStack();

    IDynamicStack<Contact> getEdgeCircleContactStack();

    IDynamicStack<Contact> getEdgePolyContactStack();

    IDynamicStack<Contact> getChainCircleContactStack();

    IDynamicStack<Contact> getChainPolyContactStack();

    v2 popVec2();

    v2[] popVec2(int num);

    void pushVec2(int num);

    Vec3 popVec3();

    Vec3[] popVec3(int num);

    void pushVec3(int num);

    Mat22 popMat22();

    Mat22[] popMat22(int num);

    void pushMat22(int num);

    Mat33 popMat33();

    void pushMat33(int num);

    AABB popAABB();

    AABB[] popAABB(int num);

    void pushAABB(int num);

    Rot popRot();

    void pushRot(int num);

    Collision getCollision();

    TimeOfImpact getTimeOfImpact();

    Distance getDistance();

    float[] getFloatArray(int argLength);

    int[] getIntArray(int argLength);

    v2[] getVec2Array(int argLength);
}
