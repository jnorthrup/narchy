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

package spacegraph.space3d.phys.util;

/**
 *
 * @author jezek2
 */
public final class IntArrayList {

	private int[] array;
	private int size;

	public IntArrayList() {
		this(16);
	}

	public IntArrayList(int capacity) {
		this.array = new int[capacity];
	}
	
	public void add(int value) {
		var a = this.array;
		if (size == a.length) {
			expand();
			a = this.array;
		}
		
		a[size++] = value;
	}
	
	private void expand() {
		var newArray = new int[array.length << 1];
		System.arraycopy(array, 0, newArray, 0, array.length);
		array = newArray;
	}

	public int remove(int index) {
		var s = this.size;

		var a = this.array;
		var old = a[index];
		if (index!= this.size -1)
			System.arraycopy(a, index+1, a, index, s - index - 1);
		this.size--;
		return old;
	}
	public void removeQuick(int index) {
		var s = this.size;

		var a = this.array;
		if (index!= --this.size)
			System.arraycopy(a, index+1, a, index, s - index - 1);
	}

	public int get(int index) {
		
		return array[index];
	}

	public void set(int index, int value) {
		
		array[index] = value;
	}
	public void setBoth(int indexValue) {
		array[indexValue] = indexValue;
	}

	public int size() {
		return size;
	}
	
	public void clear() {
		size = 0;
	}

}
