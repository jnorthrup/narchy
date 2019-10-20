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
import spacegraph.space3d.phys.shape.BU_Simplex1to4;

/**
 * Helper class for tetrahedrons.
 * 
 * @author jezek2
 */
public class TetrahedronShapeEx extends BU_Simplex1to4 {

	public TetrahedronShapeEx() {
		numVertices = 4;
		for (var i = 0; i < numVertices; i++) {
			vertices[i] = new v3();
		}
	}
	public TetrahedronShapeEx(v3 v0, v3 v1, v3 v2, v3 v3) {
		this();
		vertices[0].set(v0);
		vertices[1].set(v1);
		vertices[2].set(v2);
		vertices[3].set(v3);
		recalcLocalAabb();

	}

	
}
