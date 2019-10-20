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

import jcog.math.v3;
import jcog.util.ArrayUtil;

/**
 *
 * @author jezek2
 */
class BvhDataArray {

	private int size;
	
	float[] bound = ArrayUtil.EMPTY_FLOAT_ARRAY;
	int[] data = ArrayUtil.EMPTY_INT_ARRAY;

	public int size() {
		return size;
	}

	public void resize(int newSize) {
		var newBound = new float[newSize*6];

        System.arraycopy(bound, 0, newBound, 0, size*6);
		var newData = new int[newSize];
        System.arraycopy(data, 0, newData, 0, size);
		
		bound = newBound;
		data = newData;
		
		size = newSize;
	}
	
	public void swap(int idx1, int idx2) {
		var pos1 = idx1*6;
		var pos2 = idx2*6;

		var b = this.bound;

		var b0 = b[pos1];
		var b1 = b[pos1+1];
		var b2 = b[pos1+2];
		var b3 = b[pos1+3];
		var b4 = b[pos1+4];
		var b5 = b[pos1+5];
		var d = data[idx1];
		
		b[pos1] = b[pos2];
		b[pos1+1] = b[pos2+1];
		b[pos1+2] = b[pos2+2];
		b[pos1+3] = b[pos2+3];
		b[pos1+4] = b[pos2+4];
		b[pos1+5] = b[pos2+5];
		data[idx1] = data[idx2];

		b[pos2] = b0;
		b[pos2+1] = b1;
		b[pos2+2] = b2;
		b[pos2+3] = b3;
		b[pos2+4] = b4;
		b[pos2+5] = b5;
		data[idx2] = d;
	}
	
	public BoxCollision.AABB getBound(int idx, BoxCollision.AABB out) {
		var pos = idx*6;
		var b = this.bound;
		out.min.set(b[pos++], b[pos++], b[pos++]);
		out.max.set(b[pos++], b[pos++], b[pos]);
		return out;
	}

	public v3 getBoundMin(int idx, v3 out) {
		var pos = idx*6;
		var b = this.bound;
		out.set(b[pos++], b[pos++], b[pos]);
		return out;
	}

	public v3 getBoundMax(int idx, v3 out) {
		var pos = idx*6 + 3;
		var b = this.bound;
		out.set(b[pos++], b[pos++], b[pos]);
		return out;
	}
	
	public void setBound(int idx, BoxCollision.AABB aabb) {
		var pos = idx*6;
		var b = this.bound;
		b[pos++] = aabb.min.x;
		b[pos++] = aabb.min.y;
		b[pos++] = aabb.min.z;
		b[pos++] = aabb.max.x;
		b[pos++] = aabb.max.y;
		b[pos] =   aabb.max.z;
	}
	
	public int getData(int idx) {
		return data[idx];
	}
	
	public void setData(int idx, int value) {
		data[idx] = value;
	}
	
}
