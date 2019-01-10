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

package spacegraph.space3d.phys;

import spacegraph.space3d.phys.math.Clock;
import jcog.math.v3;

/**
 * Bullet statistics and profile support.
 * 
 * @author jezek2
 */
public class BulletStats {
	
	public static int gTotalContactPoints;
	
	
	
	public static int gNumDeepPenetrationChecks;
	public static int gNumGjkChecks;
	public static int gNumSplitImpulseRecoveries;
	
	public static int gNumAlignedAllocs;
	public static int gNumAlignedFree;
	public static int gTotalBytesAlignedAllocs;	
	
	public static int gPickingConstraintId;
	public static final v3 gOldPickingPos = new v3();
	public static float gOldPickingDist;
	
	public static int gOverlappingPairs;
	public static int gRemovePairs;
	public static int gAddedPairs;
	public static int gFindPairs;
	
	public static final Clock gProfileClock = new Clock();

	
	public static int gNumClampedCcdMotions;

	
	
	public static long updateTime;
	
	private static final boolean enableProfile = false;
	
	
	







	
	public static long profileGetTicks() {
		long ticks = gProfileClock.getTimeMicroseconds();
		return ticks;
	}

	public static float profileGetTickRate() {
		
		return 1000f;
	}

}
