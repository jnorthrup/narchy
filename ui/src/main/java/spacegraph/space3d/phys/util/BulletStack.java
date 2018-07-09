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

/**
 * Per-thread stack based object pools for common types.
 * 
 * @see StackList
 * 
 * @author jezek2
 */
public class BulletStack {

	public BulletStack() {}
	
	private static final ThreadLocal<BulletStack> threadLocal = ThreadLocal.withInitial(BulletStack::new);
	
	/**
	 * Returns stack for current thread, or create one if not present.
	 * 
	 * @return stack
	 */
	public static BulletStack get() {
		return threadLocal.get();
	}
	
	
	public final VectorStackList vectors = new VectorStackList();
	public final TransformStackList transforms = new TransformStackList();
	private final MatrixStackList matrices = new MatrixStackList();
	
	
	


	
	
	/**
	 * Pushes Vector3f, Transform and Matrix3f stacks.
	 */
	public void pushCommonMath() {
		vectors.push();
		transforms.push();
		matrices.push();
	}
	
	/**
	 * Pops Vector3f, Transform and Matrix3f stacks.
	 */
	public void popCommonMath() {
		vectors.pop();
		transforms.pop();
		matrices.pop();
	}
	
}
