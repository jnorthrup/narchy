/*
 * Java port of Bullet (c) 2008 Martin Dvorak <jezek2@advel.cz>
 *
 * Bullet Continuous Collision Detection and Physics Library
 * Copyright (c) 2003-2008 Erwin Coumans  http://www.bulletphysics.com/
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

// Dbvt implementation by Nathanael Presson

package com.bulletphysics.collision.broadphase;

import com.bulletphysics.collision.broadphase.BroadphaseProxy;
import com.bulletphysics.collision.broadphase.Dbvt;
import com.bulletphysics.collision.broadphase.DbvtAabbMm;

/**
 *
 * @author jezek2
 */
public class DbvtProxy extends BroadphaseProxy {

	public final DbvtAabbMm aabb = new DbvtAabbMm();
	public Dbvt.Node leaf;
	public final com.bulletphysics.collision.broadphase.DbvtProxy[] links = new com.bulletphysics.collision.broadphase.DbvtProxy[2];
	public int stage;

	public DbvtProxy(Object userPtr, short collisionFilterGroup, short collisionFilterMask) {
		super(userPtr, collisionFilterGroup, collisionFilterMask);
	}

}
