/*
 * Java port of Bullet (c) 2008 Martin Dvorak <jezek2@advel.cz>
 *
 * Bullet Continuous Collision Detection and Physics Library
 * Copyright (c) 2003-2007 Erwin Coumans  http:
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

package spacegraph.space3d.phys.util;


import jcog.math.v3;

/**
 * Stack-based object pool for {@link v3}.
 *
 * @author jezek2
 */
public class VectorStackList extends StackList<v3> {

	public v3 get(float x, float y, float z) {
		v3 v = get();
		v.set(x, y, z);
		return v;
	}

	public v3 get(v3 vec) {
		v3 v = get();
		v.set(vec);
		return v;
	}

	@Override
	protected v3 create() {
		return new v3();
	}

	@Override
	protected void copy(v3 dest, v3 src) {
		dest.set(src);
	}

}
