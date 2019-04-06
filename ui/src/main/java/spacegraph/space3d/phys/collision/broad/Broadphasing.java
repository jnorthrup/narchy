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


import org.jetbrains.annotations.Nullable;
import spacegraph.space3d.phys.Body3D;
import spacegraph.space3d.phys.Collidable;

/**
 * BroadphaseProxy is the main class that can be used with the Bullet broadphases.
 * It stores collision shape type information, collision filter information and
 * a client object, typically a {@link Collidable} or {@link Body3D}.
 * 
 * @author jezek2
 */
public class Broadphasing {

	
	@Nullable
	public Collidable data;
	
	
	public short collisionFilterGroup;
	public short collisionFilterMask;
	
	public Object multiSapParentProxy;
	
	public int uid; 


	Broadphasing() {

	}
	
	Broadphasing(Collidable userPtr, short collisionFilterGroup, short collisionFilterMask) {
		this(userPtr, collisionFilterGroup, collisionFilterMask, null);
	}
	
	Broadphasing(Collidable userPtr, short collisionFilterGroup, short collisionFilterMask, Object multiSapParentProxy) {
		this.data = userPtr;
		this.collisionFilterGroup = collisionFilterGroup;
		this.collisionFilterMask = collisionFilterMask;
		this.multiSapParentProxy = multiSapParentProxy;
	}

	@Override
	public final int hashCode() {
		return uid;
	}
}
